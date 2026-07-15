import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { Building2, Users, ShieldCheck, Calendar, AlertTriangle } from 'lucide-react'
import api from '../api/axios'
import { useAuth } from '../context/AuthContext'
import { useCompany } from '../context/CompanyContext'

export default function Dashboard() {
  const { user } = useAuth()
  const { companies, employees, selected } = useCompany()
  const [deadlines, setDeadlines] = useState([])

  useEffect(() => {
    api.get('/api/deadlines')
      .then((r) => setDeadlines(r.data.deadlines || []))
      .catch(() => {})
  }, [])

  const stats = [
    { label: 'Client Companies', value: companies.length, icon: Building2, to: '/companies' },
    { label: 'Employees', value: employees.length, icon: Users, to: '/employees',
      sub: selected?.companyName },
    { label: 'Upcoming Deadlines', value: deadlines.length, icon: Calendar, to: '/deadlines' },
  ]

  return (
    <div>
      <h1 className="text-2xl font-bold text-slate-800">Welcome, {user?.fullName}</h1>
      <p className="text-slate-500 mb-6">{user?.firmName}</p>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        {stats.map(({ label, value, icon: Icon, to, sub }) => (
          <Link key={label} to={to} className="card hover:shadow-md transition-shadow">
            <div className="flex items-center gap-4">
              <div className="bg-brand-50 text-brand-500 p-3 rounded-lg">
                <Icon size={22} />
              </div>
              <div>
                <p className="text-2xl font-bold text-slate-800">{value}</p>
                <p className="text-sm text-slate-500">{label}</p>
                {sub && <p className="text-xs text-slate-400">{sub}</p>}
              </div>
            </div>
          </Link>
        ))}
      </div>

      {deadlines.length > 0 && (
        <div className="card mb-6 border-amber-200 bg-amber-50">
          <div className="flex items-center gap-2 mb-3">
            <AlertTriangle size={18} className="text-amber-600" />
            <h3 className="font-semibold text-amber-800">Upcoming Deadlines</h3>
          </div>
          <ul className="space-y-1.5">
            {deadlines.map((d, i) => (
              <li key={i} className="text-sm text-slate-700">• {d}</li>
            ))}
          </ul>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <Link to="/compliance" className="card hover:shadow-md transition-shadow">
          <div className="flex items-start gap-3">
            <ShieldCheck size={22} className="text-brand-500 mt-0.5" />
            <div>
              <h3 className="font-semibold text-slate-800">Run Compliance Check</h3>
              <p className="text-sm text-slate-500 mt-0.5">
                Calculate EPF, ESI, TDS, PT and find violations
              </p>
            </div>
          </div>
        </Link>

        <div className="card bg-brand-50 border-brand-100">
          <h3 className="font-semibold text-brand-700 mb-2 text-sm">Quick Start</h3>
          <ol className="text-sm text-slate-600 space-y-1 list-decimal list-inside">
            <li>Add a client company</li>
            <li>Add employees (or upload Excel)</li>
            <li>Run compliance check</li>
            <li>Download reports and payslips</li>
          </ol>
        </div>
      </div>
    </div>
  )
}
