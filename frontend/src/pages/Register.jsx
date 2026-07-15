import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import api from '../api/axios'
import { useAuth } from '../context/AuthContext'

export default function Register() {
  const [form, setForm] = useState({
    firmName: '', fullName: '', email: '', phone: '', password: ''
  })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const { login } = useAuth()
  const navigate = useNavigate()

  const change = (e) => setForm({ ...form, [e.target.name]: e.target.value })

  const submit = async (e) => {
    e.preventDefault()
    setError(''); setLoading(true)
    try {
      const res = await api.post('/api/auth/register', form)
      login(res.data)
      navigate('/')
    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-brand-500 to-brand-700 p-4">
      <div className="bg-white rounded-2xl shadow-xl p-8 w-full max-w-md">
        <div className="text-center mb-6">
          <h1 className="text-2xl font-bold text-brand-700">Register CA Firm</h1>
          <p className="text-slate-500 text-sm mt-1">Start managing payroll compliance</p>
        </div>
        {error && <div className="bg-red-50 text-red-700 text-sm p-3 rounded-lg mb-4">{error}</div>}
        <form onSubmit={submit} className="space-y-3">
          {[
            { name: 'firmName', label: 'Firm Name', type: 'text' },
            { name: 'fullName', label: 'Your Name', type: 'text' },
            { name: 'email', label: 'Email', type: 'email' },
            { name: 'phone', label: 'Phone', type: 'text' },
            { name: 'password', label: 'Password (min 6 chars)', type: 'password' },
          ].map((f) => (
            <div key={f.name}>
              <label className="label">{f.label}</label>
              <input className="input" name={f.name} type={f.type}
                value={form[f.name]} onChange={change} required />
            </div>
          ))}
          <button className="btn btn-primary w-full" disabled={loading}>
            {loading ? 'Creating...' : 'Register'}
          </button>
        </form>
        <p className="text-center text-sm text-slate-500 mt-4">
          Already registered? <Link to="/login" className="text-brand-500 font-medium">Login</Link>
        </p>
      </div>
    </div>
  )
}
