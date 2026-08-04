import { useState, useRef } from 'react'
import { useNavigate, Link, useSearchParams } from 'react-router-dom'
import api from '../api/axios'
import { useAuth } from '../context/AuthContext'
import ReCAPTCHA from 'react-google-recaptcha'

export default function Login() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [info, setInfo] = useState('')
  const [loading, setLoading] = useState(false)
  const [captchaToken, setCaptchaToken] = useState(null)
  const [params] = useSearchParams()
  const { login } = useAuth()
  const navigate = useNavigate()
  const captchaRef = useRef(null)

  // Local dev pe key set nahi hoti -> captcha widget dikhta hi nahi.
  // Backend bhi secret na hone pe verification skip karta hai.
  const siteKey = import.meta.env.VITE_RECAPTCHA_SITE_KEY

  /** Failed attempt ke baad captcha reset - token single-use hota hai */
  const resetCaptcha = () => {
    captchaRef.current?.reset()
    setCaptchaToken(null)
  }

  const submit = async (e) => {
    e.preventDefault()
    setError(''); setInfo('')

    if (siteKey && !captchaToken) {
      setError("Please verify you're not a robot.")
      return
    }

    setLoading(true)
    try {
      const res = await api.post('/api/auth/login', { email, password, captchaToken })
      login(res.data)
      navigate('/')

    } catch (err) {
      const data = err.response?.data

      // Account grace period mein hai - restore ka option do
      if (data?.pendingDeletion) {
        const restore = window.confirm(
          'This account is scheduled for deletion. Would you like to restore it now?')

        if (restore) {
          try {
            await api.post('/api/account/reactivate', { email, password })
            setInfo('Account restored. Please log in again.')
          } catch (e) {
            setError(e.response?.data?.error || 'Could not restore the account.')
          }
        } else {
          setError('This account is scheduled for deletion and cannot be used.')
        }

      } else {
        setError(data?.error || data?.message || 'Login failed. Check credentials.')
      }

      resetCaptcha()      // Google token ek hi baar valid hota hai

    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-brand-500 to-brand-700 p-4">
      <div className="bg-white rounded-2xl shadow-xl p-8 w-full max-w-md">
        <div className="text-center mb-6">
          <h1 className="text-2xl font-bold text-brand-700">ComplianceIQ</h1>
          <p className="text-slate-500 text-sm mt-1">AI Payroll Compliance for CAs</p>
        </div>

        {/* Session expire hone pe axios interceptor ?expired=1 ke saath bhejta hai */}
        {params.get('expired') && (
          <div className="bg-amber-50 text-amber-700 text-sm p-3 rounded-lg mb-4">
            Your session expired. Please log in again.
          </div>
        )}

        {info && (
          <div className="bg-green-50 text-green-700 text-sm p-3 rounded-lg mb-4">{info}</div>
        )}

        {error && (
          <div className="bg-red-50 text-red-700 text-sm p-3 rounded-lg mb-4">{error}</div>
        )}

        <form onSubmit={submit} className="space-y-4">
          <div>
            <label className="label">Email</label>
            <input className="input" type="email" value={email}
              onChange={(e) => setEmail(e.target.value)} required />
          </div>
          <div>
            <label className="label">Password</label>
            <input className="input" type="password" value={password}
              onChange={(e) => setPassword(e.target.value)} required />
          </div>
          <div className="text-right">
            <Link to="/forgot-password" className="text-sm text-brand-500 hover:underline">
              Forgot Password?
            </Link>
          </div>

          {siteKey && (
            <div className="flex justify-center">
              <ReCAPTCHA
                ref={captchaRef}
                sitekey={siteKey}
                onChange={setCaptchaToken}
                onExpired={() => setCaptchaToken(null)}
              />
            </div>
          )}

          <button className="btn btn-primary w-full" disabled={loading}>
            {loading ? 'Logging in...' : 'Login'}
          </button>
        </form>

        <p className="text-center text-sm text-slate-500 mt-4">
          New firm? <Link to="/register" className="text-brand-500 font-medium">Register</Link>
        </p>
      </div>
    </div>
  )
}