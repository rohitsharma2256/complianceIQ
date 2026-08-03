import { useState, useEffect } from 'react'
import api from '../api/axios'
import { useCompany } from '../context/CompanyContext'
import NoCompany from '../components/NoCompany'

export default function Attendance() {
  const { selected, employees } = useCompany()
  const companyId = selected?.id

  const now = new Date()
  const [month, setMonth] = useState(now.getMonth() + 1)
  const [year, setYear] = useState(now.getFullYear())
  const [rows, setRows] = useState([])
  const [workingDays, setWorkingDays] = useState(26)
  const [summary, setSummary] = useState(null)
  const [loading, setLoading] = useState(false)
  const [msg, setMsg] = useState('')

  // Us month mein kitne din hain - working days isse zyada nahi ho sakte
  const daysInMonth = new Date(year, month, 0).getDate()

  const clamp = (v, min, max) => Math.min(max, Math.max(min, Number(v) || 0))

  const load = async () => {
    if (!companyId) return
    setLoading(true)
    try {
      const [wd, att, sum] = await Promise.all([
        api.get(`/api/attendance/working-days?month=${month}&year=${year}`),
        api.get(`/api/attendance/company/${companyId}?month=${month}&year=${year}`),
        api.get(`/api/attendance/company/${companyId}/summary?month=${month}&year=${year}`),
      ])
      setWorkingDays(wd.data.workingDays)
      setSummary(sum.data)

      // Existing attendance ko employee list se merge karo
      const map = {}
      att.data.forEach(a => { map[a.employee.id] = a })

      setRows(employees.map(e => {
        const a = map[e.id]
        return {
          employeeId: e.id,
          name: e.fullName,
          code: e.employeeCode || '-',
          workingDays: a?.workingDays ?? wd.data.workingDays,
          presentDays: a?.presentDays ?? wd.data.workingDays,
          paidLeaveDays: a?.paidLeaveDays ?? 0,
          unpaidLeaveDays: a?.unpaidLeaveDays ?? 0,
          overtimeHours: a?.overtimeHours ?? 0,
          saved: !!a,
        }
      }))
    } catch (e) {
      setMsg(e.response?.data?.error || 'Could not load attendance')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [companyId, month, year, employees])

  const update = (i, field, value) => {
    const copy = [...rows]
    const r = { ...copy[i] }

    if (field === 'workingDays') {
      r.workingDays = clamp(value, 1, daysInMonth)          // month se zyada nahi
      // Working days kam hue toh baaki fields bhi adjust karo
      r.presentDays     = clamp(r.presentDays, 0, r.workingDays)
      r.paidLeaveDays   = clamp(r.paidLeaveDays, 0, r.workingDays)
      r.unpaidLeaveDays = clamp(r.unpaidLeaveDays, 0, r.workingDays)
    } else if (field === 'overtimeHours') {
      r.overtimeHours = clamp(value, 0, 300)
    } else {
      // present / paidLeave / unpaidLeave - working days se zyada nahi
      r[field] = clamp(value, 0, r.workingDays)
    }

    // Teeno ka total working days se zyada na ho - baaki adjust karo
    const total = Number(r.presentDays) + Number(r.paidLeaveDays) + Number(r.unpaidLeaveDays)
    if (total > r.workingDays) {
      const excess = total - r.workingDays
      if (field !== 'presentDays') {
        r.presentDays = Math.max(0, r.presentDays - excess)   // present se ghatao
      } else {
        r.unpaidLeaveDays = Math.max(0, r.unpaidLeaveDays - excess)
      }
    }

    copy[i] = r
    setRows(copy)
  }

  const saveRow = async (r) => {
    try {
      await api.post('/api/attendance', {
        employeeId: r.employeeId, month, year,
        workingDays: r.workingDays,
        presentDays: r.presentDays,
        paidLeaveDays: r.paidLeaveDays,
        unpaidLeaveDays: r.unpaidLeaveDays,
        overtimeHours: r.overtimeHours,
      })
      setMsg(`Saved ${r.name}`)
      load()
    } catch (e) {
      setMsg(e.response?.data?.error || e.response?.data?.message || `Failed for ${r.name}`)
    }
  }

  const markAllFull = async () => {
    if (!confirm('Mark full attendance for all employees?')) return
    try {
      await api.post(`/api/attendance/company/${companyId}/mark-full?month=${month}&year=${year}`)
      setMsg('Full attendance marked for all')
      load()
    } catch (e) {
      setMsg(e.response?.data?.error || 'Failed to mark attendance')
    }
  }

  const lop = (r) => Math.max(0, Number(r.unpaidLeaveDays || 0))
  const factor = (r) => r.workingDays
    ? Math.min(1, (Number(r.presentDays) + Number(r.paidLeaveDays)) / r.workingDays)
    : 1

  if (!selected) return <NoCompany />

  return (
    <div>
      <h1 className="text-2xl font-bold text-slate-800 mb-1">Attendance</h1>
      <p className="text-slate-500 text-sm mb-4">
        Enter monthly attendance for {selected.companyName}. Unpaid leave becomes
        Loss of Pay and reduces earned wages, PF and ESI.
      </p>

      {/* Controls */}
      <div className="flex flex-wrap gap-3 items-end mb-4">
        <div>
          <label className="label">Month</label>
          <select className="input" value={month} onChange={e => setMonth(+e.target.value)}>
            {[...Array(12)].map((_, i) =>
              <option key={i + 1} value={i + 1}>
                {new Date(2000, i).toLocaleString('en', { month: 'long' })}
              </option>)}
          </select>
        </div>
        <div>
          <label className="label">Year</label>
          <input className="input w-24" type="number" min="2020" max="2100" value={year}
            onChange={e => setYear(+e.target.value)} />
        </div>
        <div className="text-sm text-slate-600 pb-2">
          Working days: <b>{workingDays}</b>
          <span className="text-slate-400"> of {daysInMonth} days (Sundays excluded)</span>
        </div>
        <button className="btn btn-primary" onClick={markAllFull}>Mark All Full</button>
      </div>

      {msg && <div className="bg-blue-50 text-blue-700 text-sm p-2 rounded mb-3">{msg}</div>}

      {/* Summary */}
      {summary && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-4">
          {[
            ['Total Employees', summary.totalEmployees, 'text-slate-700'],
            ['Entered', summary.recordsEntered, 'text-green-600'],
            ['Missing', summary.recordsMissing, 'text-orange-600'],
            ['With LOP', summary.employeesWithLop, 'text-red-600'],
          ].map(([label, val, cls]) => (
            <div key={label} className="bg-white rounded-lg border p-3">
              <div className="text-xs text-slate-500">{label}</div>
              <div className={`text-xl font-bold ${cls}`}>{val}</div>
            </div>
          ))}
        </div>
      )}

      {/* Table */}
      {loading ? <p className="text-slate-500">Loading...</p> : (
        <div className="bg-white rounded-lg border overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-slate-50 text-slate-600">
              <tr>
                <th className="p-2 text-left">Code</th>
                <th className="p-2 text-left">Employee</th>
                <th className="p-2">Working</th>
                <th className="p-2">Present</th>
                <th className="p-2">Paid Leave</th>
                <th className="p-2">Unpaid (LOP)</th>
                <th className="p-2">OT Hrs</th>
                <th className="p-2">Paid %</th>
                <th className="p-2"></th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r, i) => (
                <tr key={r.employeeId} className={lop(r) > 0 ? 'bg-red-50' : ''}>
                  <td className="p-2 text-slate-500">{r.code}</td>
                  <td className="p-2 font-medium">{r.name}</td>
                  <td className="p-2"><input className="input w-16 text-center"
                    type="number" min="1" max={daysInMonth} value={r.workingDays}
                    onChange={e => update(i, 'workingDays', e.target.value)} /></td>
                  <td className="p-2"><input className="input w-16 text-center"
                    type="number" min="0" max={r.workingDays} step="0.5" value={r.presentDays}
                    onChange={e => update(i, 'presentDays', e.target.value)} /></td>
                  <td className="p-2"><input className="input w-16 text-center"
                    type="number" min="0" max={r.workingDays} step="0.5" value={r.paidLeaveDays}
                    onChange={e => update(i, 'paidLeaveDays', e.target.value)} /></td>
                  <td className="p-2"><input className="input w-16 text-center"
                    type="number" min="0" max={r.workingDays} step="0.5" value={r.unpaidLeaveDays}
                    onChange={e => update(i, 'unpaidLeaveDays', e.target.value)} /></td>
                  <td className="p-2"><input className="input w-16 text-center"
                    type="number" min="0" max="300" value={r.overtimeHours}
                    onChange={e => update(i, 'overtimeHours', e.target.value)} /></td>
                  <td className="p-2 text-center font-medium">
                    {(factor(r) * 100).toFixed(1)}%</td>
                  <td className="p-2">
                    <button className="text-brand-500 hover:underline text-xs"
                      onClick={() => saveRow(r)}>Save</button>
                  </td>
                </tr>
              ))}
              {rows.length === 0 && (
                <tr><td colSpan="9" className="p-4 text-slate-400 text-center">
                  No employees yet. Add employees first.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}