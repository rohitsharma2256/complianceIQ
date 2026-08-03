import { useState, useEffect } from 'react'
import { NavLink } from 'react-router-dom'
import {
  LayoutDashboard, Building2, Users, CalendarCheck, ShieldCheck, FileText,
  Receipt, FileSpreadsheet, Calculator, ClipboardCheck,
  Calendar, Bot, BookOpen, RefreshCw, History, Settings, LogOut, X
} from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import api from '../api/axios'
import CompanySelector from './CompanySelector'

const links = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/companies', label: 'Companies', icon: Building2 },
  { to: '/employees', label: 'Employees', icon: Users },
  { to: '/attendance', label: 'Attendance', icon: CalendarCheck },
  { to: '/compliance', label: 'Compliance', icon: ShieldCheck },
  { to: '/reports', label: 'Reports', icon: FileText },
  { to: '/payslips', label: 'Payslips', icon: Receipt },
  { to: '/forms', label: 'Forms', icon: FileSpreadsheet },
  { to: '/calculations', label: 'Calculations', icon: Calculator },
  { to: '/checks', label: 'Checks', icon: ClipboardCheck },
  { to: '/deadlines', label: 'Deadlines', icon: Calendar },
  { to: '/ai-chat', label: 'AI Assistant', icon: Bot },
  { to: '/ask-law', label: 'Ask Law', icon: BookOpen },
  // Platform admin only - law updates saare firms ke RAG answers affect karte hain
  { to: '/law-updates', label: 'Add Law Update', icon: RefreshCw, adminOnly: true },
  { to: '/audit', label: 'Activity Log', icon: History },
  { to: '/settings', label: 'Settings', icon: Settings },
]

export default function Sidebar({ open, onClose }) {
  const { user, logout } = useAuth()
  const [isAdmin, setIsAdmin] = useState(false)

  useEffect(() => {
    api.get('/api/law-updates/is-admin')
      .then(r => setIsAdmin(!!r.data.isAdmin))
      .catch(() => setIsAdmin(false))
  }, [])

  // Admin nahi hai toh admin-only links hata do.
  // NOTE: yeh sirf UX hai - asli enforcement backend pe hai (403).
  const visibleLinks = links.filter(l => !l.adminOnly || isAdmin)

  return (
    <>
      {/* Mobile overlay — sidebar khula ho toh peeche dark */}
      {open && (
        <div className="fixed inset-0 bg-black/50 z-30 md:hidden"
          onClick={onClose} />
      )}

      <aside className={`
        w-64 bg-brand-700 text-white flex flex-col
        fixed md:sticky top-0 h-screen z-40
        transition-transform duration-200
        ${open ? 'translate-x-0' : '-translate-x-full'}
        md:translate-x-0
      `}>
        <div className="p-5 border-b border-brand-600 flex items-center justify-between">
          <div>
            <h1 className="text-xl font-bold">ComplianceIQ</h1>
            <p className="text-xs text-brand-100 mt-1">AI Payroll Compliance</p>
          </div>
          {/* Mobile close button */}
          <button onClick={onClose} className="md:hidden text-white">
            <X size={22} />
          </button>
        </div>

        <div className="p-4 border-b border-brand-600">
          <CompanySelector />
        </div>

        <nav className="flex-1 overflow-y-auto py-3">
          {visibleLinks.map(({ to, label, icon: Icon, adminOnly }) => (
            <NavLink
              key={to}
              to={to}
              end={to === '/'}
              onClick={onClose}
              className={({ isActive }) =>
                `flex items-center gap-3 px-5 py-2.5 text-sm transition-colors ${
                  isActive ? 'bg-brand-600 border-r-4 border-white' : 'hover:bg-brand-600/50'
                }`
              }
            >
              <Icon size={18} />
              <span className="flex-1">{label}</span>
              {adminOnly && (
                <span className="text-[9px] bg-amber-400 text-brand-900
                                 px-1.5 py-0.5 rounded font-semibold">
                  ADMIN
                </span>
              )}
            </NavLink>
          ))}
        </nav>

        <div className="p-4 border-t border-brand-600">
          <p className="text-sm font-medium truncate">{user?.firmName}</p>
          <p className="text-xs text-brand-100 truncate mb-2">{user?.email}</p>
          {isAdmin && (
            <span className="inline-block text-[10px] bg-amber-400 text-brand-900
                             px-2 py-0.5 rounded-full font-semibold mb-2">
              PLATFORM ADMIN
            </span>
          )}
          <button onClick={logout}
                  className="flex items-center gap-2 text-sm hover:text-red-300 mt-1">
            <LogOut size={16} /> Logout
          </button>
        </div>
      </aside>
    </>
  )
}