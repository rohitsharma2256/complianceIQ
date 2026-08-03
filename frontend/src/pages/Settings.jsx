import { useState } from 'react'
import api from '../api/axios'
import { useAuth } from '../context/AuthContext'
import { AlertTriangle, KeyRound } from 'lucide-react'

export default function Settings() {
  const { user, logout } = useAuth()

  /* ---------- change password ---------- */
  const [pwd, setPwd] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' })
  const [pwdMsg, setPwdMsg] = useState('')
  const [pwdErr, setPwdErr] = useState('')
  const [pwdLoading, setPwdLoading] = useState(false)

  /* ---------- delete account ---------- */
  const [open, setOpen] = useState(false)
  const [password, setPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [msg, setMsg] = useState('')
  const [loading, setLoading] = useState(false)

  const changePwd = async (e) => {
    e.preventDefault()
    setPwdMsg(''); setPwdErr(''); setPwdLoading(true)
    try {
      const res = await api.post('/api/account/change-password', pwd)
      setPwdMsg(res.data.message)
      setPwd({ currentPassword: '', newPassword: '', confirmPassword: '' })
    } catch (err) {
      setPwdErr(err.response?.data?.error || 'Could not change the password.')
    } finally { setPwdLoading(false) }
  }

  const deleteAccount = async () => {
    setMsg(''); setLoading(true)
    try {
      const res = await api.post('/api/account/delete', { password, confirmation })
      alert(res.data.message)
      logout()
    } catch (err) {
      setMsg(err.response?.data?.error || 'Could not delete the account.')
    } finally { setLoading(false) }
  }

  return (
    <div className="max-w-2xl">
      <h1 className="text-2xl font-bold text-slate-800 mb-1">Settings</h1>
      <p className="text-slate-500 text-sm mb-6">{user?.firmName} · {user?.email}</p>

      {/* ==================== CHANGE PASSWORD ==================== */}
      <div className="card mb-4">
        <h3 className="font-semibold flex items-center gap-2 mb-1">
          <KeyRound size={18} /> Change Password
        </h3>
        <p className="text-sm text-slate-500 mb-4">
          You'll need your current password. A confirmation email is sent after the change.
        </p>

        {pwdMsg && (
          <div className="bg-green-50 text-green-700 text-sm p-3 rounded-lg mb-3">{pwdMsg}</div>
        )}
        {pwdErr && (
          <div className="bg-red-50 text-red-700 text-sm p-3 rounded-lg mb-3">{pwdErr}</div>
        )}

        <form onSubmit={changePwd} className="space-y-3">
          <div>
            <label className="label">Current Password</label>
            <input className="input" type="password" required
                   value={pwd.currentPassword}
                   onChange={(e) => setPwd({ ...pwd, currentPassword: e.target.value })} />
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            <div>
              <label className="label">New Password (min 6 characters)</label>
              <input className="input" type="password" required
                     value={pwd.newPassword}
                     onChange={(e) => setPwd({ ...pwd, newPassword: e.target.value })} />
            </div>
            <div>
              <label className="label">Confirm New Password</label>
              <input className="input" type="password" required
                     value={pwd.confirmPassword}
                     onChange={(e) => setPwd({ ...pwd, confirmPassword: e.target.value })} />
            </div>
          </div>
          {pwd.newPassword && pwd.confirmPassword
            && pwd.newPassword !== pwd.confirmPassword && (
            <p className="text-xs text-red-600">The new passwords do not match.</p>
          )}
          <button className="btn btn-primary" disabled={pwdLoading}>
            {pwdLoading ? 'Updating...' : 'Change Password'}
          </button>
        </form>
      </div>

      {/* ==================== DELETE ACCOUNT ==================== */}
      <div className="card border border-red-200">
        <h3 className="font-semibold text-red-700 flex items-center gap-2 mb-1">
          <AlertTriangle size={18} /> Delete Account
        </h3>
        <p className="text-sm text-slate-600 mb-4">
          Deleting your account deactivates it immediately and removes all firm data —
          companies, employees and payroll records — permanently after 30 days.
          You can restore it by logging in before that date.
        </p>

        {!open ? (
          <button className="btn btn-outline text-red-600 border-red-300"
                  onClick={() => setOpen(true)}>
            Delete my account
          </button>
        ) : (
          <div className="space-y-3">
            {msg && (
              <div className="bg-red-50 text-red-700 text-sm p-3 rounded-lg">{msg}</div>
            )}
            <div>
              <label className="label">Confirm your password</label>
              <input className="input" type="password" value={password}
                     onChange={(e) => setPassword(e.target.value)} />
            </div>
            <div>
              <label className="label">Type <b>DELETE</b> to confirm</label>
              <input className="input" value={confirmation}
                     onChange={(e) => setConfirmation(e.target.value)}
                     placeholder="DELETE" />
            </div>
            <div className="flex gap-2">
              <button className="btn bg-red-600 text-white hover:bg-red-700"
                      onClick={deleteAccount}
                      disabled={loading || confirmation !== 'DELETE' || !password}>
                {loading ? 'Processing...' : 'Permanently delete account'}
              </button>
              <button className="btn btn-outline"
                      onClick={() => { setOpen(false); setMsg('')
                                       setPassword(''); setConfirmation('') }}>
                Cancel
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}