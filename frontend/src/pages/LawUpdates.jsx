import { useState } from 'react'
import api from '../api/axios'

export default function LawUpdates() {
  const [form, setForm] = useState({ title: '', content: '', source: '', effectiveDate: '' })
  const [msg, setMsg] = useState('')
  const [loading, setLoading] = useState(false)

  const change = (e) => setForm({ ...form, [e.target.name]: e.target.value })

  const submit = async (e) => {
    e.preventDefault()
    setLoading(true); setMsg('')
    try {
      const res = await api.post('/api/law-updates/add', form)
      setMsg(res.data.message || 'Added! Knowledge base updated instantly.')
      setForm({ title: '', content: '', source: '', effectiveDate: '' })
    } catch (err) {
      setMsg(err.response?.data?.message || 'Failed to add')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      <h1 className="text-2xl font-bold text-slate-800 mb-1">Add Law Update</h1>
      <p className="text-slate-500 mb-4 text-sm">
        Admin only — add a verified government notification. It's instantly available to all users via Ask Law.
      </p>

      {msg && <div className="bg-green-50 text-green-700 text-sm p-3 rounded-lg mb-4">{msg}</div>}

      <form onSubmit={submit} className="card max-w-2xl space-y-3">
        <div>
          <label className="label">Title *</label>
          <input className="input" name="title" value={form.title} onChange={change} required
            placeholder="e.g. Maharashtra Minimum Wage Revision Oct 2026" />
        </div>
        <div>
          <label className="label">Content *</label>
          <textarea className="input h-32" name="content" value={form.content} onChange={change} required
            placeholder="Paste the notification text / rule details..." />
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="label">Source</label>
            <input className="input" name="source" value={form.source} onChange={change}
              placeholder="e.g. Maharashtra Labour Dept" />
          </div>
          <div>
            <label className="label">Effective Date</label>
            <input className="input" type="date" name="effectiveDate" value={form.effectiveDate} onChange={change} />
          </div>
        </div>
        <button className="btn btn-primary" disabled={loading}>
          {loading ? 'Adding...' : 'Add to Knowledge Base'}
        </button>
      </form>
    </div>
  )
}
