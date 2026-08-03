import { useState } from 'react'
import { useSearchParams, useNavigate, Link } from 'react-router-dom'
import api from '../api/axios'

export default function ResetPassword() {
  const [params] = useSearchParams()
  const token = params.get('token')
  const navigate = useNavigate()

  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [msg, setMsg] = useState('')
  const [loading, setLoading] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    if (password !== confirm) return setMsg('Passwords do not match')
    if (password.length < 6) return setMsg('Password must be at least 6 characters')

    setLoading(true); setMsg('')
    try {
      await api.post('/api/auth/reset-password', { token, newPassword: password })
      setMsg('Password updated! Redirecting to login...')
      setTimeout(() => navigate('/login'), 2000)
    } catch (err) {
      setMsg(err.response?.data?.error || 'This link is invalid or expired')
    } finally {
      setLoading(false)
    }
  }

  if (!token) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-brand-700 p-4">
        <div className="bg-white rounded-2xl shadow-xl p-8 max-w-md text-center">
          <p className="text-red-600 mb-4">Invalid reset link.</p>
          <Link to="/forgot-password" className="btn btn-primary">
            Request a new link
          </Link>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-brand-700 p-4">
      <div className="bg-white rounded-2xl shadow-xl p-8 w-full max-w-md">
        <h1 className="text-2xl font-bold text-center text-slate-800 mb-6">
          Set New Password
        </h1>

        {msg && (
          <div className="bg-blue-50 text-blue-700 text-sm p-3 rounded-lg mb-4">
            {msg}
          </div>
        )}

        <form onSubmit={submit} className="space-y-4">
          <div>
            <label className="label">New Password (min 6 characters)</label>
            <input className="input" type="password" required
              value={password} onChange={(e) => setPassword(e.target.value)} />
          </div>
          <div>
            <label className="label">Confirm Password</label>
            <input className="input" type="password" required
              value={confirm} onChange={(e) => setConfirm(e.target.value)} />
          </div>
          <button className="btn btn-primary w-full" disabled={loading}>
            {loading ? 'Updating...' : 'Update Password'}
          </button>
        </form>
      </div>
    </div>
  )
}