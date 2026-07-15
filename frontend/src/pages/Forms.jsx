import { useState, useEffect } from 'react'
import api from '../api/axios'
import { useCompany } from '../context/CompanyContext'
import NoCompany from '../components/NoCompany'
import EmployeeSelect from '../components/EmployeeSelect'
import MonthYear from '../components/MonthYear'
import { Download } from 'lucide-react'

export default function Forms() {
  const { selected } = useCompany()
  const [msg, setMsg] = useState('')
  const [empId, setEmpId] = useState('')
  const [fy, setFy] = useState(new Date().getFullYear())
  const [month, setMonth] = useState(new Date().getMonth() + 1)
  const [year, setYear] = useState(new Date().getFullYear())
  const [ecrRaw, setEcrRaw] = useState('')
  const [ecrRows, setEcrRows] = useState([])
  const [runs, setRuns] = useState([])

  useEffect(() => {
    const load = async () => {
      if (!selected) return
      try {
        const res = await api.get(`/api/compliance/runs/${selected.id}`)
        setRuns(res.data)
      } catch { setRuns([]) }
    }
    load()
    setEcrRaw(''); setEcrRows([])
  }, [selected])

  if (!selected) return <NoCompany />

  const downloadPdf = async (url, filename) => {
    setMsg('')
    try {
      const res = await api.get(url, { responseType: 'blob' })
      const link = window.URL.createObjectURL(new Blob([res.data]))
      const a = document.createElement('a')
      a.href = link; a.download = filename; a.click()
      setMsg(`${filename} downloaded!`)
    } catch { setMsg('Download failed. Check inputs.') }
  }

  const loadEcr = async () => {
    setMsg('')
    try {
      const res = await api.get(`/api/forms/ecr/${selected.id}?month=${month}&year=${year}`)
      const raw = res.data.ecrFile || ''
      setEcrRaw(raw)
      // Parse into readable rows
      const rows = raw.trim().split('\n').filter(Boolean).map((line) => {
        const p = line.split('#~#')
        return { uan: p[0], name: p[1], gross: p[2], epfWages: p[3], ee: p[5], eps: p[6], er: p[7] }
      })
      setEcrRows(rows)
      if (rows.length === 0) setMsg('No EPF-applicable employees found')
    } catch { setMsg('ECR generation failed') }
  }

  const downloadEcrFile = () => {
    const blob = new Blob([ecrRaw], { type: 'text/plain' })
    const a = document.createElement('a')
    a.href = window.URL.createObjectURL(blob)
    a.download = `ECR_${selected.companyName.replaceAll(' ', '_')}_${month}_${year}.txt`
    a.click()
  }

  return (
    <div>
      <h1 className="text-2xl font-bold text-slate-800 mb-1">Statutory Forms</h1>
      <p className="text-slate-500 text-sm mb-4">Form 16, ECR & Challans for {selected.companyName}</p>

      {msg && <div className="bg-blue-50 text-blue-700 text-sm p-3 rounded-lg mb-4">{msg}</div>}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="card">
          <h3 className="font-semibold mb-3">Form 16 (TDS Certificate)</h3>
          <div className="space-y-3">
            <EmployeeSelect value={empId} onChange={setEmpId} />
            <div>
              <label className="label">Financial Year</label>
              <select className="input" value={fy} onChange={(e) => setFy(e.target.value)}>
                {[2024, 2025, 2026].map((y) => <option key={y} value={y}>{y}-{y + 1}</option>)}
              </select>
            </div>
            <button className="btn btn-primary w-full"
              onClick={() => empId ? downloadPdf(`/api/forms/form16/${empId}?fy=${fy}`, 'form16.pdf') : setMsg('Select an employee')}>
              Download Form 16
            </button>
          </div>
        </div>

        <div className="card">
          <h3 className="font-semibold mb-3">Payment Challan</h3>
          <p className="text-sm text-slate-500 mb-3">From your compliance checks:</p>
          {runs.length === 0 ? (
            <p className="text-slate-400 text-sm">Run a compliance check first.</p>
          ) : (
            <div className="space-y-2">
              {runs.map((r) => (
                <div key={r.id} className="flex items-center justify-between bg-slate-50 p-2 rounded">
                  <span className="text-sm">Period {r.month}/{r.year} · {r.totalEmployees} emp</span>
                  <button className="text-brand-500 text-xs font-medium hover:underline"
                    onClick={() => downloadPdf(`/api/forms/challan/${r.id}`, `challan-${r.month}-${r.year}.pdf`)}>
                    Download
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="card md:col-span-2">
          <h3 className="font-semibold mb-1">ECR File (EPF Portal Upload)</h3>
          <p className="text-sm text-slate-500 mb-3">
            Generates the monthly EPF return. Review the table, then download the .txt file to upload on the EPFO portal.
          </p>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3 items-end mb-4">
            <MonthYear month={month} year={year} setMonth={setMonth} setYear={setYear} />
            <button className="btn btn-primary" onClick={loadEcr}>Generate ECR</button>
          </div>

          {ecrRows.length > 0 && (
            <>
              <div className="overflow-x-auto mb-3">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="text-left border-b text-slate-500 bg-slate-50">
                      <th className="py-2 px-2">Employee</th>
                      <th className="px-2">UAN</th>
                      <th className="px-2 text-right">Gross</th>
                      <th className="px-2 text-right">EPF Wages</th>
                      <th className="px-2 text-right">Employee Share</th>
                      <th className="px-2 text-right">EPS</th>
                      <th className="px-2 text-right">Employer Share</th>
                    </tr>
                  </thead>
                  <tbody>
                    {ecrRows.map((r, i) => (
                      <tr key={i} className="border-b hover:bg-slate-50">
                        <td className="py-2 px-2 font-medium">{r.name}</td>
                        <td className="px-2 text-slate-500">{r.uan}</td>
                        <td className="px-2 text-right">₹{r.gross}</td>
                        <td className="px-2 text-right">₹{r.epfWages}</td>
                        <td className="px-2 text-right">₹{r.ee}</td>
                        <td className="px-2 text-right">₹{r.eps}</td>
                        <td className="px-2 text-right">₹{r.er}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <button className="btn btn-primary flex items-center gap-2" onClick={downloadEcrFile}>
                <Download size={16} /> Download ECR File (.txt for EPFO portal)
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  )
}