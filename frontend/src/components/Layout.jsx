import Sidebar from './Sidebar'
import { useCompany } from '../context/CompanyContext'

export default function Layout({ children }) {
  const { selected } = useCompany()
  return (
    <div className="flex min-h-screen">
      <Sidebar />
      <div className="flex-1 flex flex-col overflow-x-hidden">
        {selected && (
          <div className="bg-white border-b border-slate-200 px-8 py-2.5 text-sm">
            <span className="text-slate-400">Working on:</span>{' '}
            <span className="font-semibold text-slate-800">{selected.companyName}</span>
            <span className="text-slate-400"> · {selected.city}, {selected.state}</span>
          </div>
        )}
        <main className="flex-1 p-8">{children}</main>
      </div>
    </div>
  )
}
