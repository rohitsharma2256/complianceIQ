import { useState } from 'react'
import Sidebar from './Sidebar'
import { useCompany } from '../context/CompanyContext'
import { Menu } from 'lucide-react'

export default function Layout({ children }) {
  const { selected } = useCompany()
  const [sidebarOpen, setSidebarOpen] = useState(false)

  return (
    <div className="flex min-h-screen">
      <Sidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />

      <div className="flex-1 flex flex-col min-w-0">
        {/* Top bar — mobile hamburger + working-on */}
        <div className="bg-white border-b border-slate-200 px-4 md:px-8 py-3 flex items-center gap-3 sticky top-0 z-20">
          {/* Hamburger — sirf mobile pe dikhega */}
          <button onClick={() => setSidebarOpen(true)}
            className="md:hidden text-slate-600">
            <Menu size={24} />
          </button>

          {selected ? (
            <p className="text-sm text-slate-500 truncate">
              Working on: <span className="font-semibold text-slate-800">{selected.companyName}</span>
              <span className="hidden sm:inline"> · {selected.city}, {selected.state}</span>
            </p>
          ) : (
            <p className="text-sm text-slate-400">No company selected</p>
          )}
        </div>

        <main className="flex-1 p-4 md:p-8 overflow-x-hidden">{children}</main>
      </div>
    </div>
  )
}