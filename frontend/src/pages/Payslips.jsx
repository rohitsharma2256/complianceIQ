import { useState } from 'react'
import api from '../api/axios'
import { useCompany } from '../context/CompanyContext'
import NoCompany from '../components/NoCompany'
import EmployeeSelect from '../components/EmployeeSelect'
import MonthYear from '../components/MonthYear'
import { Download, Package } from 'lucide-react'

export default function Payslips() {
  const { selected, employees } = useCompany()
  const [employeeId, setEmployeeId] = useState('')
  const [month, setMonth] = useState(new Date().getMonth() + 1)
  const [year, setYear] = useState(new Date().getFullYear())
  const [msg, setMsg] = useState('')
  const [loading, setLoading] = useState(false)

  if (!selected) return <NoCompany />

  const downloadOne = async () => {
    if (!employeeId) return setMsg('Select an employee first')
    setLoading(true); setMsg('')
    try {
      const res = await api.get(`/api/payslips/${employeeId}?month=${month}&year=${year}`, { responseType: 'blob' })
      const emp = employees.find((e) => e.id === employeeId)
      const link = window.URL.createObjectURL(new Blob([res.data]))
      const a = document.createElement('a')
      a.href = link
      a.download = `${emp?.fullName || 'payslip'}-${month}-${year}.pdf`
      a.click()
      setMsg('Payslip downloaded!')
    } catch { setMsg('Download failed') } finally { setLoading(false) }
  }

  const downloadAll = async () => {
    setLoading(true); setMsg('')
    try {
      const res = await api.get(`/api/payslips/bulk/${selected.id}?month=${month}&year=${year}`, { responseType: 'blob' })
      const link = window.URL.createObjectURL(new Blob([res.data]))
      const a = document.createElement('a')
      a.href = link
      a.download = `payslips-${month}-${year}.zip`
      a.click()
      setMsg(`All ${employees.length} payslips downloaded as ZIP!`)
    } catch { setMsg('Bulk download failed') } finally { setLoading(false) }
  }

  return (
    <div>
      <h1 className="text-2xl font-bold text-slate-800 mb-1">Payslips</h1>
      <p className="text-slate-500 text-sm mb-4">{selected.companyName} — {employees.length} employees</p>

      {msg && <div className="bg-blue-50 text-blue-700 text-sm p-3 rounded-lg mb-4">{msg}</div>}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="card">
          <h3 className="font-semibold mb-3 flex items-center gap-2"><Download size={18} /> Single Payslip</h3>
          <div className="space-y-3">
            <EmployeeSelect value={employeeId} onChange={setEmployeeId} />
            <div className="grid grid-cols-2 gap-3">
              <MonthYear month={month} year={year} setMonth={setMonth} setYear={setYear} />
            </div>
            <button className="btn btn-primary w-full" onClick={downloadOne} disabled={loading}>
              {loading ? '...' : 'Download Payslip'}
            </button>
          </div>
        </div>

        <div className="card">
          <h3 className="font-semibold mb-3 flex items-center gap-2"><Package size={18} /> All Payslips (ZIP)</h3>
          <p className="text-sm text-slate-500 mb-3">
            Download payslips for all {employees.length} employees of {selected.companyName} in one ZIP file.
          </p>
          <div className="grid grid-cols-2 gap-3 mb-3">
            <MonthYear month={month} year={year} setMonth={setMonth} setYear={setYear} />
          </div>
          <button className="btn btn-primary w-full" onClick={downloadAll} disabled={loading || employees.length === 0}>
            {loading ? 'Preparing ZIP...' : `Download All ${employees.length} Payslips`}
          </button>
        </div>
      </div>
    </div>
  )
}
