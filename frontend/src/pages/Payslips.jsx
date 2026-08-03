import { useState } from 'react'
import api from '../api/axios'
import { useCompany } from '../context/CompanyContext'
import NoCompany from '../components/NoCompany'
import EmployeeSelect from '../components/EmployeeSelect'
import MonthYear from '../components/MonthYear'
import { Download, Package } from 'lucide-react'

export default function Payslips() {
  const { selected, employees } = useCompany()
  const now = new Date()
  const currentMonth = now.getMonth() + 1
  const currentYear = now.getFullYear()

  const [employeeId, setEmployeeId] = useState('')
  const [month, setMonth] = useState(currentMonth)
  const [year, setYear] = useState(currentYear)
  const [msg, setMsg] = useState('')
  const [loading, setLoading] = useState(false)

  if (!selected) return <NoCompany />

  const future = year > currentYear || (year === currentYear && month > currentMonth)

  /** Blob download ke waqt backend JSON error bhej sakta hai - usko padho */
  const readBlobError = async (err, fallback) => {
    try {
      const text = await err.response?.data?.text?.()
      const json = JSON.parse(text)
      return json.error || json.message || fallback
    } catch {
      return fallback
    }
  }

  const saveBlob = (data, filename) => {
    const url = window.URL.createObjectURL(new Blob([data]))
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    a.click()
    window.URL.revokeObjectURL(url)
  }

  const downloadOne = async () => {
    if (!employeeId) return setMsg('Select an employee first')
    setLoading(true); setMsg('')
    try {
      const res = await api.get(
        `/api/payslips/${employeeId}?month=${month}&year=${year}`,
        { responseType: 'blob' })
      const emp = employees.find((e) => e.id === employeeId)
      const name = (emp?.fullName || 'payslip').replace(/[^a-zA-Z0-9]/g, '_')
      saveBlob(res.data, `${name}_${month}_${year}.pdf`)
      setMsg('Payslip downloaded!')
    } catch (err) {
      setMsg(await readBlobError(err, 'Download failed'))
    } finally { setLoading(false) }
  }

  const downloadAll = async () => {
    setLoading(true); setMsg('')
    try {
      const res = await api.get(
        `/api/payslips/bulk/${selected.id}?month=${month}&year=${year}`,
        { responseType: 'blob' })
      saveBlob(res.data, `payslips_${month}_${year}.zip`)
      setMsg(`All ${employees.length} payslips downloaded as ZIP!`)
    } catch (err) {
      setMsg(await readBlobError(err, 'Bulk download failed'))
    } finally { setLoading(false) }
  }

  return (
    <div>
      <h1 className="text-2xl font-bold text-slate-800 mb-1">Payslips</h1>
      <p className="text-slate-500 text-sm mb-4">
        {selected.companyName} — {employees.length} employees
      </p>

      {msg && <div className="bg-blue-50 text-blue-700 text-sm p-3 rounded-lg mb-4">{msg}</div>}

      {future && (
        <div className="bg-amber-50 text-amber-700 text-sm p-3 rounded-lg mb-4">
          Payslips are available only up to the current month.
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="card">
          <h3 className="font-semibold mb-3 flex items-center gap-2">
            <Download size={18} /> Single Payslip
          </h3>
          <div className="space-y-3">
            <EmployeeSelect value={employeeId} onChange={setEmployeeId} />
            <div className="grid grid-cols-2 gap-3">
              <MonthYear month={month} year={year} setMonth={setMonth} setYear={setYear} />
            </div>
            <button className="btn btn-primary w-full"
                    onClick={downloadOne} disabled={loading || future}>
              {loading ? 'Generating...' : 'Download Payslip'}
            </button>
          </div>
        </div>

        <div className="card">
          <h3 className="font-semibold mb-3 flex items-center gap-2">
            <Package size={18} /> All Payslips (ZIP)
          </h3>
          <p className="text-sm text-slate-500 mb-3">
            Download payslips for all {employees.length} employees of{' '}
            {selected.companyName} in one ZIP file.
          </p>
          <div className="grid grid-cols-2 gap-3 mb-3">
            <MonthYear month={month} year={year} setMonth={setMonth} setYear={setYear} />
          </div>
          <button className="btn btn-primary w-full"
                  onClick={downloadAll}
                  disabled={loading || future || employees.length === 0}>
            {loading ? 'Preparing ZIP...' : `Download All ${employees.length} Payslips`}
          </button>
        </div>
      </div>
    </div>
  )
}