import { useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../api/axios'

export default function ForgotPassword() {
  const [email, setEmail] = useState('')
  const [msg, setMsg] = useState('')
  const [loading, setLoading] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setLoading(true); setMsg('')
    try {
      const res = await api.post('/api/auth/forgot-password', { email })
      setMsg(res.data.message)
    } catch {
      setMsg('Something went wrong. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-brand-700 p-4">
      <div className="bg-white rounded-2xl shadow-xl p-8 w-full max-w-md">
        <h1 className="text-2xl font-bold text-center text-slate-800 mb-1">
          Forgot Password
        </h1>
        <p className="text-slate-500 text-sm text-center mb-6">
          Enter your registered email and we'll send you a reset link
        </p>

        {msg && (
          <div className="bg-blue-50 text-blue-700 text-sm p-3 rounded-lg mb-4">
            {msg}
          </div>
        )}

        <form onSubmit={submit} className="space-y-4">
          <div>
            <label className="label">Email</label>
            <input className="input" type="email" required
              value={email} onChange={(e) => setEmail(e.target.value)} />
          </div>
          <button className="btn btn-primary w-full" disabled={loading}>
            {loading ? 'Sending...' : 'Send Reset Link'}
          </button>
        </form>

        <p className="text-center text-sm text-slate-500 mt-4">
          Remembered it? <Link to="/login" className="text-brand-500 font-medium">Login</Link>
        </p>
      </div>
    </div>
  )
}