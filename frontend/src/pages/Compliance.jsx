import { useState, useEffect } from 'react'
import api from '../api/axios'
import { useCompany } from '../context/CompanyContext'
import NoCompany from '../components/NoCompany'
import MonthYear from '../components/MonthYear'
import { Download, FileText, CheckCircle } from 'lucide-react'

export default function Compliance() {
  const { selected } = useCompany()
  const [month, setMonth] = useState(new Date().getMonth() + 1)
  const [year, setYear] = useState(new Date().getFullYear())
  const [result, setResult] = useState(null)
  const [violations, setViolations] = useState(null)
  const [history, setHistory] = useState([])
  const [msg, setMsg] = useState('')
  const [loading, setLoading] = useState(false)

  const loadHistory = async () => {
    if (!selected) return
    try {
      const res = await api.get(`/api/compliance/runs/${selected.id}`)
      setHistory(res.data)
    } catch { setHistory([]) }
  }

  useEffect(() => { setResult(null); setViolations(null); loadHistory() }, [selected])

  if (!selected) return <NoCompany />

  const runCheck = async () => {
    setMsg(''); setViolations(null); setLoading(true)
    try {
      const res = await api.post(`/api/compliance/check/${selected.id}?month=${month}&year=${year}`)
      setResult(res.data)
      loadHistory()
    } catch (err) {
      setMsg(err.response?.data?.message || 'Check failed. Add employees first.')
    } finally { setLoading(false) }
  }

  const loadViolations = async (runId) => {
    try {
      const res = await api.get(`/api/compliance/violations/${runId}`)
      setViolations(res.data)
    } catch (err) {
      setMsg('Failed to load violations')
    }
  }

  const resolveViolation = async (id) => {
    try {
      await api.put(`/api/compliance/violations/${id}/resolve`)
      setMsg('Violation marked resolved')
      if (result) loadViolations(result.id)
    } catch { setMsg('Failed to resolve') }
  }

  const downloadPdf = async (url, filename) => {
    try {
      const res = await api.get(url, { responseType: 'blob' })
      const link = window.URL.createObjectURL(new Blob([res.data]))
      const a = document.createElement('a')
      a.href = link; a.download = filename; a.click()
    } catch { setMsg('Download failed') }
  }

  return (
    <div>
      <h1 className="text-2xl font-bold text-slate-800 mb-1">Compliance Check</h1>
      <p className="text-slate-500 text-sm mb-4">EPF, ESI, TDS, PT + violations for {selected.companyName}</p>

      <div className="card mb-4 grid grid-cols-1 md:grid-cols-3 gap-3 items-end">
        <MonthYear month={month} year={year} setMonth={setMonth} setYear={setYear} />
        <div>
          <button className="btn btn-primary w-full" onClick={runCheck} disabled={loading}>
            {loading ? 'Checking...' : 'Run Compliance Check'}
          </button>
        </div>
      </div>

      {msg && <div className="bg-blue-50 text-blue-700 text-sm p-3 rounded-lg mb-4">{msg}</div>}

      {result && (
        <div className="card mb-4">
          <div className="flex items-center justify-between mb-4">
            <h3 className="font-semibold text-slate-800">Results — {result.totalEmployees} employees</h3>
            <span className="bg-green-100 text-green-700 text-xs px-3 py-1 rounded-full">{result.status}</span>
          </div>
          <div className="grid grid-cols-2 md:grid-cols-3 gap-4 mb-4">
            {[
              ['EPF Employee (12%)', result.totalEpfEmployee],
              ['EPF Employer (12%)', result.totalEpfEmployer],
              ['ESI Employee (0.75%)', result.totalEsiEmployee],
              ['ESI Employer (3.25%)', result.totalEsiEmployer],
              ['TDS', result.totalTds],
              ['Professional Tax', result.totalProfessionalTax],
            ].map(([label, val]) => (
              <div key={label} className="bg-slate-50 rounded-lg p-3">
                <p className="text-xs text-slate-500">{label}</p>
                <p className="text-lg font-semibold text-slate-800">₹{val}</p>
              </div>
            ))}
          </div>
          {/* DIRECT ACTIONS — no ID copying! */}
          <div className="flex flex-wrap gap-2">
            <button className="btn btn-primary flex items-center gap-2"
              onClick={() => downloadPdf(`/api/reports/compliance/${result.id}`, 'compliance-report.pdf')}>
              <FileText size={16} /> Download Audit Report
            </button>
            <button className="btn btn-outline flex items-center gap-2"
              onClick={() => downloadPdf(`/api/forms/challan/${result.id}`, 'challan.pdf')}>
              <Download size={16} /> Download Challan
            </button>
            <button className="btn btn-outline" onClick={() => loadViolations(result.id)}>
              View Violations
            </button>
          </div>
        </div>
      )}

      {violations && (
        <div className="card mb-4">
          <h3 className="font-semibold text-slate-800 mb-3">Violations ({violations.length})</h3>
          {violations.length === 0 ? (
            <p className="text-green-600">No open violations. All compliant!</p>
          ) : (
            <div className="space-y-3">
              {violations.map((v) => (
                <div key={v.id} className="border-l-4 border-red-400 bg-red-50 p-3 rounded">
                  <div className="flex justify-between items-start">
                    <span className="font-medium text-red-700">{v.violationType}</span>
                    <button onClick={() => resolveViolation(v.id)}
                      className="flex items-center gap-1 text-xs bg-green-100 text-green-700 px-2 py-1 rounded hover:bg-green-200">
                      <CheckCircle size={12} /> Mark Resolved
                    </button>
                  </div>
                  <p className="text-sm text-slate-700 mt-1">{v.description}</p>
                  <p className="text-sm text-slate-500 mt-1"><b>Fix:</b> {v.recommendedFix}</p>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {history.length > 0 && (
        <div className="card">
          <h3 className="font-semibold text-slate-800 mb-3">Past Compliance Checks</h3>
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left border-b text-slate-500">
                <th className="py-2">Period</th><th>Employees</th><th>Status</th><th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {history.map((run) => (
                <tr key={run.id} className="border-b hover:bg-slate-50">
                  <td className="py-2">{run.month}/{run.year}</td>
                  <td>{run.totalEmployees}</td>
                  <td><span className="text-xs bg-slate-100 px-2 py-0.5 rounded">{run.status}</span></td>
                  <td>
                    <div className="flex gap-2">
                      <button className="text-brand-500 text-xs hover:underline"
                        onClick={() => downloadPdf(`/api/reports/compliance/${run.id}`, `report-${run.month}-${run.year}.pdf`)}>
                        Report
                      </button>
                      <button className="text-brand-500 text-xs hover:underline"
                        onClick={() => downloadPdf(`/api/forms/challan/${run.id}`, `challan-${run.month}-${run.year}.pdf`)}>
                        Challan
                      </button>
                      <button className="text-brand-500 text-xs hover:underline"
                        onClick={() => { setResult(run); loadViolations(run.id) }}>
                        Violations
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
