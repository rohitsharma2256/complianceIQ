import { useState, useEffect } from 'react'
import api from '../api/axios'
import { useCompany } from '../context/CompanyContext'
import NoCompany from '../components/NoCompany'
import EmployeeSelect from '../components/EmployeeSelect'
import MonthYear from '../components/MonthYear'
import { Download, AlertTriangle, CheckCircle2, FileText, Receipt } from 'lucide-react'

export default function Forms() {
  const { selected } = useCompany()
  const now = new Date()

  const [msg, setMsg] = useState('')
  const [empId, setEmpId] = useState('')
  const [fy, setFy] = useState(now.getFullYear() - 1)
  const [month, setMonth] = useState(now.getMonth() + 1)
  const [year, setYear] = useState(now.getFullYear())
  const [ecrCheck, setEcrCheck] = useState(null)
  const [runs, setRuns] = useState([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    const load = async () => {
      if (!selected) return
      try {
        const res = await api.get(`/api/compliance/runs/${selected.id}`)
        setRuns(res.data)
      } catch { setRuns([]) }
    }
    load()
    setEcrCheck(null)
  }, [selected])

  if (!selected) return <NoCompany />

  /** Blob download pe backend JSON error bhejta hai - usko padho */
  const readBlobError = async (err, fallback) => {
    try {
      const text = await err.response?.data?.text?.()
      const json = JSON.parse(text)
      return json.error || json.message || fallback
    } catch { return fallback }
  }

  const saveBlob = (data, filename, type) => {
    const url = window.URL.createObjectURL(new Blob([data], type ? { type } : undefined))
    const a = document.createElement('a')
    a.href = url; a.download = filename; a.click()
    window.URL.revokeObjectURL(url)
  }

  const downloadFile = async (url, filename, type) => {
    setMsg(''); setLoading(true)
    try {
      const res = await api.get(url, { responseType: 'blob' })
      saveBlob(res.data, filename, type)
      setMsg(`${filename} downloaded.`)
    } catch (err) {
      setMsg(await readBlobError(err, 'Download failed. Check the inputs.'))
    } finally { setLoading(false) }
  }

  /* ---------- ECR: pehle validate, phir download ---------- */
  const validateEcr = async () => {
    setMsg(''); setLoading(true); setEcrCheck(null)
    try {
      const res = await api.get(
        `/api/ecr/validate/${selected.id}?month=${month}&year=${year}`)
      setEcrCheck(res.data)
    } catch (err) {
      setMsg(err.response?.data?.error || 'Could not validate the ECR data.')
    } finally { setLoading(false) }
  }

  const downloadEcr = () =>
    downloadFile(`/api/ecr/download/${selected.id}?month=${month}&year=${year}`,
      `ECR_${month}_${year}.txt`, 'text/plain')

  const fyYears = [now.getFullYear() - 1, now.getFullYear() - 2, now.getFullYear() - 3]

  const CHALLANS = [
    { code: 'EPF', label: 'EPF Challan', hint: 'Due 15th' },
    { code: 'ESI', label: 'ESI Challan', hint: 'Due 15th' },
    { code: 'TDS', label: 'TDS Challan', hint: 'Due 7th' },
  ]

  return (
    <div>
      <h1 className="text-2xl font-bold text-slate-800 mb-1">Statutory Forms</h1>
      <p className="text-slate-500 text-sm mb-4">
        Form 16, ECR and Challans for {selected.companyName}
      </p>

      {msg && <div className="bg-blue-50 text-blue-700 text-sm p-3 rounded-lg mb-4">{msg}</div>}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">

        {/* ==================== FORM 16 ==================== */}
        <div className="card">
          <h3 className="font-semibold mb-1 flex items-center gap-2">
            <FileText size={18} /> Form 16 (TDS Certificate)
          </h3>
          <p className="text-xs text-slate-500 mb-3">
            Part A + Part B draft with salary breakup and tax computation.
          </p>
          <div className="space-y-3">
            <EmployeeSelect value={empId} onChange={setEmpId} />
            <div>
              <label className="label">Financial Year</label>
              <select className="input" value={fy} onChange={(e) => setFy(+e.target.value)}>
                {fyYears.map((y) => (
                  <option key={y} value={y}>
                    {y}-{String(y + 1).slice(2)} (AY {y + 1}-{String(y + 2).slice(2)})
                  </option>
                ))}
              </select>
            </div>
            <button className="btn btn-primary w-full" disabled={loading}
              onClick={() => empId
                ? downloadFile(`/api/form16/${empId}?fy=${fy}`,
                    `form16_${fy}-${fy + 1}.pdf`)
                : setMsg('Select an employee first')}>
              {loading ? 'Generating...' : 'Download Form 16'}
            </button>
            <p className="text-xs text-amber-600">
              Reference copy. The statutory Part A must be downloaded from TRACES.
              Requires the employee PAN and the employer TAN.
            </p>
          </div>
        </div>

        {/* ==================== CHALLANS ==================== */}
        <div className="card">
          <h3 className="font-semibold mb-1 flex items-center gap-2">
            <Receipt size={18} /> Payment Challans
          </h3>
          <p className="text-xs text-slate-500 mb-3">
            EPF, ESI and TDS payment advice from your compliance checks.
          </p>
          {runs.length === 0 ? (
            <p className="text-slate-400 text-sm">Run a compliance check first.</p>
          ) : (
            <div className="space-y-2 max-h-64 overflow-y-auto">
              {runs.map((r) => (
                <div key={r.id} className="bg-slate-50 p-2 rounded">
                  <div className="text-sm font-medium mb-1.5">
                    {r.month}/{r.year} · {r.totalEmployees} employees
                  </div>
                  <div className="flex flex-wrap gap-3">
                    {CHALLANS.map((c) => (
                      <button key={c.code} disabled={loading}
                        title={c.hint}
                        className="text-brand-500 text-xs font-medium hover:underline disabled:opacity-50"
                        onClick={() => downloadFile(
                          `/api/challans/${r.id}?type=${c.code}`,
                          `${c.code}_challan_${r.month}_${r.year}.pdf`)}>
                        {c.label}
                      </button>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          )}
          <p className="text-xs text-slate-400 mt-3">
            Payment advice only. The actual challan with the CIN/CRN is generated
            on the EPFO, ESIC or income-tax portal.
          </p>
        </div>

        {/* ==================== ECR ==================== */}
        <div className="card md:col-span-2">
          <h3 className="font-semibold mb-1">ECR File (EPFO Portal Upload)</h3>
          <p className="text-sm text-slate-500 mb-3">
            The EPFO portal rejects files with missing or malformed UANs, so the
            data is validated before export.
          </p>

          <div className="grid grid-cols-1 md:grid-cols-4 gap-3 items-end mb-4">
            <MonthYear month={month} year={year} setMonth={setMonth} setYear={setYear} />
            <button className="btn btn-outline" onClick={validateEcr} disabled={loading}>
              {loading ? 'Checking...' : 'Validate Data'}
            </button>
            <button className="btn btn-primary flex items-center justify-center gap-2"
              onClick={downloadEcr}
              disabled={loading || (ecrCheck && !ecrCheck.valid)}>
              <Download size={16} /> Download ECR (.txt)
            </button>
          </div>

          {/* Validation result */}
          {ecrCheck && (
            <div className="space-y-3">
              <div className={`flex items-center gap-2 text-sm p-3 rounded-lg ${
                ecrCheck.valid ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-700'}`}>
                {ecrCheck.valid
                  ? <CheckCircle2 size={18} /> : <AlertTriangle size={18} />}
                <span>
                  {ecrCheck.valid
                    ? `Ready to export — ${ecrCheck.eligibleMembers} EPF member(s).`
                    : `${ecrCheck.errors.length} blocking issue(s). EPFO will reject this file.`}
                </span>
              </div>

              {ecrCheck.errors?.length > 0 && (
                <div className="border border-red-200 rounded-lg p-3">
                  <p className="text-sm font-semibold text-red-700 mb-2">
                    Must fix before export
                  </p>
                  <ul className="text-sm text-slate-700 space-y-1">
                    {ecrCheck.errors.map((e, i) => <li key={i}>• {e}</li>)}
                  </ul>
                </div>
              )}

              {ecrCheck.warnings?.length > 0 && (
                <div className="border border-amber-200 rounded-lg p-3">
                  <p className="text-sm font-semibold text-amber-700 mb-2">Warnings</p>
                  <ul className="text-sm text-slate-700 space-y-1">
                    {ecrCheck.warnings.map((w, i) => <li key={i}>• {w}</li>)}
                  </ul>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}