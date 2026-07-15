import { useState, useEffect } from 'react'
import api from '../api/axios'
import { useAuth } from '../context/AuthContext'

export default function Deadlines() {
  const { user } = useAuth()
  const [deadlines, setDeadlines] = useState([])
  const [penalty, setPenalty] = useState('')
  const [taskType, setTaskType] = useState('EPF')
  const [email, setEmail] = useState(user?.email || '')
  const [msg, setMsg] = useState('')

  useEffect(() => {
    const load = async () => {
      try {
        const res = await api.get('/api/deadlines')
        setDeadlines(res.data.deadlines || [])
      } catch { setMsg('Failed to load deadlines') }
    }
    load()
  }, [])

  const getPenalty = async () => {
    try {
      const res = await api.get(`/api/deadlines/penalty/${taskType}`)
      setPenalty(res.data.penaltyInfo)
    } catch { setMsg('Failed') }
  }

  const sendEmail = async () => {
    if (!email) return setMsg('Enter an email')
    try {
      const res = await api.post('/api/deadlines/send-test-email', { email })
      setMsg(res.data.message)
    } catch { setMsg('Email failed') }
  }

  return (
    <div>
      <h1 className="text-2xl font-bold text-slate-800 mb-4">Statutory Deadlines</h1>
      {msg && <div className="bg-blue-50 text-blue-700 text-sm p-3 rounded-lg mb-4">{msg}</div>}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="card">
          <h3 className="font-semibold mb-3">Upcoming Deadlines</h3>
          {deadlines.length === 0 ? (
            <p className="text-slate-400 text-sm">No upcoming deadlines right now.</p>
          ) : (
            <ul className="space-y-2">
              {deadlines.map((d, i) => (
                <li key={i} className="bg-amber-50 border-l-4 border-amber-400 p-3 rounded text-sm">{d}</li>
              ))}
            </ul>
          )}
          <div className="mt-4 text-xs text-slate-400">
            TDS: 7th monthly · EPF & ESI: 15th monthly
          </div>
        </div>

        <div className="card">
          <h3 className="font-semibold mb-3">Late Payment Penalty Info</h3>
          <div className="flex gap-2 mb-3">
            <select className="input" value={taskType} onChange={(e) => setTaskType(e.target.value)}>
              <option>EPF</option><option>ESI</option><option>TDS</option>
            </select>
            <button className="btn btn-primary whitespace-nowrap" onClick={getPenalty}>Show</button>
          </div>
          {penalty && <pre className="bg-slate-50 p-3 rounded text-xs whitespace-pre-wrap">{penalty}</pre>}
        </div>

      <div className="card md:col-span-2">
          <h3 className="font-semibold mb-2">📧 Automatic Email Alerts</h3>
          <div className="bg-green-50 border-l-4 border-green-400 p-3 rounded text-sm text-slate-700 mb-3">
            <b>You don't need to do anything.</b> ComplianceIQ automatically emails you
            every morning at 9 AM whenever a TDS, EPF or ESI deadline is approaching —
            sent to your registered email ({email}).
          </div>
          <p className="text-sm text-slate-500 mb-2">Want to verify it's working? Send yourself a test:</p>
          <div className="flex gap-2">
            <input className="input" type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
            <button className="btn btn-outline whitespace-nowrap" onClick={sendEmail}>Send Test Email</button>
          </div>
        </div>
      </div>
    </div>
  )
}
