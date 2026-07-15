import { Link } from 'react-router-dom'
import { Building2 } from 'lucide-react'

export default function NoCompany() {
  return (
    <div className="card text-center py-12">
      <Building2 size={40} className="mx-auto text-slate-300 mb-3" />
      <h3 className="font-semibold text-slate-700">No company selected</h3>
      <p className="text-sm text-slate-500 mt-1 mb-4">
        Add a client company to get started.
      </p>
      <Link to="/companies" className="btn btn-primary inline-block">
        Go to Companies
      </Link>
    </div>
  )
}
