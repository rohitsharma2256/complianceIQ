import { NavLink } from 'react-router-dom'
import {
  LayoutDashboard, Building2, Users, ShieldCheck, FileText,
  Receipt, FileSpreadsheet, Calculator, ClipboardCheck,
  Calendar, Bot, BookOpen, RefreshCw, LogOut
} from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import CompanySelector from './CompanySelector'

const links = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/companies', label: 'Companies', icon: Building2 },
  { to: '/employees', label: 'Employees', icon: Users },
  { to: '/compliance', label: 'Compliance', icon: ShieldCheck },
  { to: '/reports', label: 'Reports', icon: FileText },
  { to: '/payslips', label: 'Payslips', icon: Receipt },
  { to: '/forms', label: 'Forms', icon: FileSpreadsheet },
  { to: '/calculations', label: 'Calculations', icon: Calculator },
  { to: '/checks', label: 'Checks', icon: ClipboardCheck },
  { to: '/deadlines', label: 'Deadlines', icon: Calendar },
  { to: '/ai-chat', label: 'AI Assistant', icon: Bot },
  { to: '/ask-law', label: 'Ask Law', icon: BookOpen },
  { to: '/law-updates', label: 'Add Law Update', icon: RefreshCw },
]

export default function Sidebar() {
  const { user, logout } = useAuth()
  return (
    <aside className="w-60 bg-brand-700 text-white flex flex-col h-screen sticky top-0">
      <div className="p-5 border-b border-brand-600">
        <h1 className="text-xl font-bold">ComplianceIQ</h1>
        <p className="text-xs text-brand-100 mt-0.5">AI Payroll Compliance</p>
      </div>

      <div className="py-3 border-b border-brand-600">
        <CompanySelector />
      </div>

      <nav className="flex-1 overflow-y-auto py-2">
        {links.map(({ to, label, icon: Icon }) => (
          <NavLink key={to} to={to} end={to === '/'}
            className={({ isActive }) =>
              `flex items-center gap-3 px-5 py-2 text-sm transition-colors ${
                isActive ? 'bg-brand-600 border-r-4 border-white' : 'hover:bg-brand-600/50'
              }`}>
            <Icon size={17} />
            {label}
          </NavLink>
        ))}
      </nav>

      <div className="p-4 border-t border-brand-600">
        <p className="text-sm font-medium truncate">{user?.firmName}</p>
        <p className="text-xs text-brand-100 truncate mb-2">{user?.email}</p>
        <button onClick={logout} className="flex items-center gap-2 text-sm hover:text-red-300">
          <LogOut size={15} /> Logout
        </button>
      </div>
    </aside>
  )
}
