import { useState, useEffect } from 'react'
import api from '../api/axios'
import { useCompany } from '../context/CompanyContext'
import NoCompany from '../components/NoCompany'
import { FileText } from 'lucide-react'

export default function Reports() {
  const { selected } = useCompany()
  const [history, setHistory] = useState([])
  const [msg, setMsg] = useState('')

  useEffect(() => {
    const load = async () => {
      if (!selected) return
      try {
        const res = await api.get(`/api/compliance/runs/${selected.id}`)
        setHistory(res.data)
      } catch { setHistory([]) }
    }
    load()
  }, [selected])

  if (!selected) return <NoCompany />

  const download = async (run) => {
    setMsg('')
    try {
      const res = await api.get(`/api/reports/compliance/${run.id}`, { responseType: 'blob' })
      const link = window.URL.createObjectURL(new Blob([res.data]))
      const a = document.createElement('a')
      a.href = link
      a.download = `audit-report-${run.month}-${run.year}.pdf`
      a.click()
      setMsg(`Report for ${run.month}/${run.year} downloaded!`)
    } catch {
      setMsg('Download failed')
    }
  }

  return (
    <div>
      <h1 className="text-2xl font-bold text-slate-800 mb-1">Audit Reports</h1>
      <p className="text-slate-500 text-sm mb-4">
        AI-powered PDF reports for {selected.companyName}. Run a compliance check first to generate new periods.
      </p>

      {msg && <div className="bg-blue-50 text-blue-700 text-sm p-3 rounded-lg mb-4">{msg}</div>}

      {history.length === 0 ? (
        <div className="card text-slate-400">
          No compliance checks yet. Go to <b>Compliance</b> and run a check — the report will appear here.
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {history.map((run) => (
            <div key={run.id} className="card">
              <div className="flex items-start gap-3">
                <div className="bg-brand-50 text-brand-500 p-3 rounded-lg">
                  <FileText size={22} />
                </div>
                <div className="flex-1">
                  <h3 className="font-semibold text-slate-800">Period {run.month}/{run.year}</h3>
                  <p className="text-xs text-slate-500">{run.totalEmployees} employees · {run.status}</p>
                </div>
              </div>
              <button className="btn btn-primary w-full mt-4" onClick={() => download(run)}>
                Download PDF Report
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
