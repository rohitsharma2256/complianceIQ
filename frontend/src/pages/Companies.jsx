import { useState } from 'react'
import { Pencil, Trash2, Check } from 'lucide-react'
import api from '../api/axios'
import { useCompany } from '../context/CompanyContext'

const EMPTY = {
  companyName: '', state: '', city: '', industryType: '',
  epfRegistrationNumber: '', esicRegistrationNumber: '', tanNumber: ''
}

export default function Companies() {
  const { companies, selected, selectCompany, loadCompanies } = useCompany()
  const [showForm, setShowForm] = useState(false)
  const [editId, setEditId] = useState(null)
  const [form, setForm] = useState(EMPTY)
  const [msg, setMsg] = useState('')

  const change = (e) => setForm({ ...form, [e.target.name]: e.target.value })

  const startAdd = () => {
    setEditId(null); setForm(EMPTY); setShowForm(true)
  }

  const startEdit = (c) => {
    setEditId(c.id)
    setForm({
      companyName: c.companyName || '', state: c.state || '',
      city: c.city || '', industryType: c.industryType || '',
      epfRegistrationNumber: c.epfRegistrationNumber || '',
      esicRegistrationNumber: c.esicRegistrationNumber || '',
      tanNumber: c.tanNumber || ''
    })
    setShowForm(true)
  }

  const submit = async (e) => {
    e.preventDefault()
    setMsg('')
    try {
      if (editId) {
        await api.put(`/api/companies/${editId}`, form)
        setMsg('Company updated')
      } else {
        const tenantId = localStorage.getItem('tenantId')
        await api.post('/api/companies', { ...form, tenantId })
        setMsg('Company added')
      }
      setShowForm(false); setEditId(null); setForm(EMPTY)
      loadCompanies()
    } catch (err) {
      setMsg(err.response?.data?.message || 'Failed to save')
    }
  }

  const remove = async (c) => {
    if (!window.confirm(`Remove ${c.companyName}?`)) return
    try {
      await api.delete(`/api/companies/${c.id}`)
      setMsg('Company removed')
      localStorage.removeItem('selectedCompanyId')
      loadCompanies()
    } catch {
      setMsg('Failed to remove')
    }
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-slate-800">Client Companies</h1>
        <button className="btn btn-primary" onClick={showForm ? () => setShowForm(false) : startAdd}>
          {showForm ? 'Cancel' : '+ Add Company'}
        </button>
      </div>

      {msg && <div className="bg-blue-50 text-blue-700 text-sm p-3 rounded-lg mb-4">{msg}</div>}

      {showForm && (
        <form onSubmit={submit} className="card mb-6">
          <h3 className="font-semibold mb-4">
            {editId ? 'Edit Company' : 'New Company'}
          </h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {[
              { name: 'companyName', label: 'Company Name *' },
              { name: 'state', label: 'State *' },
              { name: 'city', label: 'City' },
              { name: 'industryType', label: 'Industry Type' },
              { name: 'epfRegistrationNumber', label: 'EPF Reg Number' },
              { name: 'esicRegistrationNumber', label: 'ESIC Reg Number' },
              { name: 'tanNumber', label: 'TAN Number' },
            ].map((f) => (
              <div key={f.name}>
                <label className="label">{f.label}</label>
                <input className="input" name={f.name} value={form[f.name]}
                  onChange={change} required={f.label.includes('*')} />
              </div>
            ))}
          </div>
          <button className="btn btn-primary mt-4">
            {editId ? 'Update Company' : 'Save Company'}
          </button>
        </form>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {companies.map((c) => (
          <div key={c.id}
            className={`card ${selected?.id === c.id ? 'ring-2 ring-brand-500' : ''}`}>
            <div className="flex items-start justify-between">
              <div>
                <h3 className="font-semibold text-slate-800">{c.companyName}</h3>
                <p className="text-sm text-slate-500">{c.city}, {c.state}</p>
              </div>
              {selected?.id === c.id && (
                <span className="flex items-center gap-1 text-xs bg-brand-50 text-brand-600 px-2 py-0.5 rounded-full">
                  <Check size={11} /> Active
                </span>
              )}
            </div>

            <div className="mt-3 text-xs text-slate-500 space-y-0.5">
              <p>EPF: {c.epfRegistrationNumber || '-'}</p>
              <p>TAN: {c.tanNumber || '-'}</p>
            </div>

            <div className="flex gap-2 mt-4">
              {selected?.id !== c.id && (
                <button className="btn btn-outline text-xs py-1 flex-1"
                  onClick={() => selectCompany(c)}>
                  Select
                </button>
              )}
              <button className="btn btn-outline text-xs py-1 px-2"
                onClick={() => startEdit(c)} title="Edit">
                <Pencil size={13} />
              </button>
              <button className="btn btn-danger text-xs py-1 px-2"
                onClick={() => remove(c)} title="Remove">
                <Trash2 size={13} />
              </button>
            </div>
          </div>
        ))}
        {companies.length === 0 && (
          <p className="text-slate-400">No companies yet. Add your first client.</p>
        )}
      </div>
    </div>
  )
}
