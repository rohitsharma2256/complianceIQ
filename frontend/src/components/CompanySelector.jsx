import { Building2 } from 'lucide-react'
import { useCompany } from '../context/CompanyContext'

export default function CompanySelector() {
  const { companies, selected, selectCompany } = useCompany()

  if (companies.length === 0) {
    return (
      <div className="px-4 py-3 bg-brand-600/40 rounded-lg mx-3 mb-3">
        <p className="text-xs text-brand-100">No companies yet</p>
        <p className="text-xs text-brand-100/70">Add one to get started</p>
      </div>
    )
  }

  return (
    <div className="px-3 mb-3">
      <label className="flex items-center gap-1.5 text-xs text-brand-100 mb-1.5">
        <Building2 size={13} /> Active Company
      </label>
      <select
        className="w-full bg-brand-600 text-white text-sm px-3 py-2 rounded-lg border border-brand-500 focus:outline-none focus:ring-1 focus:ring-white"
        value={selected?.id || ''}
        onChange={(e) => {
          const c = companies.find((x) => x.id === e.target.value)
          selectCompany(c)
        }}
      >
        {companies.map((c) => (
          <option key={c.id} value={c.id}>{c.companyName}</option>
        ))}
      </select>
    </div>
  )
}
