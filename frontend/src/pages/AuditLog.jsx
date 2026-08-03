import { useState, useEffect } from 'react'
import api from '../api/axios'
import { History, Filter } from 'lucide-react'

const ACTION_STYLE = {
  CREATE:            'bg-green-100 text-green-700',
  UPDATE:            'bg-blue-100 text-blue-700',
  DELETE:            'bg-red-100 text-red-700',
  PAYROLL_RUN:       'bg-purple-100 text-purple-700',
  VIOLATION_RESOLVED:'bg-emerald-100 text-emerald-700',
  PASSWORD_CHANGE:   'bg-amber-100 text-amber-700',
  LAW_UPDATE:        'bg-indigo-100 text-indigo-700',
}

export default function AuditLog() {
  const [logs, setLogs] = useState([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(false)
  const [msg, setMsg] = useState('')

  const load = async () => {
    setLoading(true)
    try {
      const res = await api.get(`/api/audit?page=${page}&size=30`)
      setLogs(res.data.content || [])
      setTotalPages(res.data.totalPages || 0)
    } catch (err) {
      setMsg(err.response?.data?.error || 'Could not load the activity log.')
    } finally { setLoading(false) }
  }

  useEffect(() => { load() }, [page])

  const when = (iso) => {
    if (!iso) return '-'
    const d = new Date(iso)
    return d.toLocaleString('en-IN', {
      day: '2-digit', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit',
    })
  }

  return (
    <div>
      <h1 className="text-2xl font-bold text-slate-800 mb-1 flex items-center gap-2">
        <History size={22} /> Activity Log
      </h1>
      <p className="text-slate-500 text-sm mb-4">
        Every change to your firm's records — who did what, and when.
        This log is append-only and cannot be edited.
      </p>

      {msg && <div className="bg-red-50 text-red-700 text-sm p-3 rounded-lg mb-4">{msg}</div>}

      <div className="card overflow-x-auto">
        {loading ? (
          <p className="text-slate-500 text-sm">Loading...</p>
        ) : logs.length === 0 ? (
          <p className="text-slate-400 text-sm">No activity recorded yet.</p>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left border-b text-slate-500">
                <th className="py-2">When</th>
                <th>Action</th>
                <th>Description</th>
                <th>Changes</th>
                <th>By</th>
              </tr>
            </thead>
            <tbody>
              {logs.map((l) => (
                <tr key={l.id} className="border-b hover:bg-slate-50 align-top">
                  <td className="py-2 text-slate-500 whitespace-nowrap">
                    {when(l.createdAt)}
                  </td>
                  <td>
                    <span className={`text-xs px-2 py-0.5 rounded ${
                      ACTION_STYLE[l.action] || 'bg-slate-100 text-slate-600'}`}>
                      {l.action}
                    </span>
                  </td>
                  <td className="max-w-md">{l.description}</td>
                  <td className="text-xs text-slate-500 max-w-xs">
                    {l.changes || '-'}
                  </td>
                  <td className="text-xs text-slate-500">{l.userEmail}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {totalPages > 1 && (
        <div className="flex items-center gap-2 mt-3">
          <button className="btn btn-outline text-sm" disabled={page === 0}
                  onClick={() => setPage(page - 1)}>Previous</button>
          <span className="text-sm text-slate-500">
            Page {page + 1} of {totalPages}
          </span>
          <button className="btn btn-outline text-sm" disabled={page + 1 >= totalPages}
                  onClick={() => setPage(page + 1)}>Next</button>
        </div>
      )}
    </div>
  )
}