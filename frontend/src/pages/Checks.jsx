import { useState } from 'react'
import api from '../api/axios'
import { useCompany } from '../context/CompanyContext'
import NoCompany from '../components/NoCompany'
import EmployeeSelect from '../components/EmployeeSelect'

// camelCase → "Camel Case"
const labelize = (k) => k.replace(/([A-Z])/g, ' $1').replace(/^./, (c) => c.toUpperCase()).trim()

function Value({ v }) {
  if (typeof v === 'boolean')
    return <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${v ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>{v ? 'Yes' : 'No'}</span>
  return <span className="font-medium text-slate-800">{String(v)}</span>
}

function Result({ data }) {
  if (!data) return null
  const entries = Object.entries(data)
  return (
    <div className="bg-slate-50 rounded-lg p-4 mt-3 text-sm space-y-1">
      {entries.map(([k, v]) => {
        if (Array.isArray(v)) {
          return (
            <div key={k} className="pt-2">
              <p className="text-slate-500 mb-1">{labelize(k)} ({v.length})</p>
              {v.length === 0 ? (
                <p className="text-green-600 text-xs">None — all clear ✓</p>
              ) : (
                <div className="space-y-2">
                  {v.map((item, i) => (
                    <div key={i} className="bg-white border border-slate-200 rounded p-2">
                      {typeof item === 'object' ? Object.entries(item).map(([ik, iv]) => (
                        <div key={ik} className="flex justify-between py-0.5">
                          <span className="text-slate-500 text-xs">{labelize(ik)}</span>
                          <span className="text-xs text-right max-w-[60%]">{Array.isArray(iv) ? iv.join(', ') : String(iv)}</span>
                        </div>
                      )) : <span className="text-xs">{String(item)}</span>}
                    </div>
                  ))}
                </div>
              )}
            </div>
          )
        }
        if (typeof v === 'object' && v !== null) {
          return (
            <div key={k} className="pt-2">
              <p className="text-slate-500 mb-1">{labelize(k)}</p>
              <div className="bg-white border border-slate-200 rounded p-2">
                {Object.entries(v).map(([ik, iv]) => (
                  <div key={ik} className="flex justify-between py-0.5">
                    <span className="text-slate-500 text-xs">{labelize(ik)}</span>
                    <span className="text-xs">{String(iv)}</span>
                  </div>
                ))}
              </div>
            </div>
          )
        }
        return (
          <div key={k} className="flex justify-between py-1 border-b border-slate-100 last:border-0">
            <span className="text-slate-500">{labelize(k)}</span>
            <span className="text-right max-w-[60%]"><Value v={v} /></span>
          </div>
        )
      })}
    </div>
  )
}

export default function Checks() {
  const { selected } = useCompany()
  const [empId, setEmpId] = useState('')
  const [skill, setSkill] = useState('UNSKILLED')
  const [leaveDays, setLeaveDays] = useState(0)
  const [out, setOut] = useState({})
  const [msg, setMsg] = useState('')

  if (!selected) return <NoCompany />

  const call = async (key, url, needsEmp = false) => {
    if (needsEmp && !empId) return setMsg('Select an employee first')
    setMsg('')
    try {
      const res = await api.get(url)
      setOut({ ...out, [key]: res.data })
    } catch (err) {
      setMsg(err.response?.data?.message || 'Failed')
    }
  }

  return (
    <div>
      <h1 className="text-2xl font-bold text-slate-800 mb-1">Compliance Checks</h1>
      <p className="text-slate-500 text-sm mb-4">{selected.companyName}</p>

      {msg && <div className="bg-red-50 text-red-700 text-sm p-3 rounded-lg mb-4">{msg}</div>}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="card">
          <h3 className="font-semibold mb-3">Minimum Wage Check</h3>
          <div className="space-y-3">
            <EmployeeSelect value={empId} onChange={setEmpId} />
            <div>
              <label className="label">Skill Level</label>
              <select className="input" value={skill} onChange={(e) => setSkill(e.target.value)}>
                <option>UNSKILLED</option><option>SEMI_SKILLED</option>
                <option>SKILLED</option><option>HIGHLY_SKILLED</option>
              </select>
            </div>
            <button className="btn btn-primary w-full"
              onClick={() => call('minWage', `/api/calculations/minimum-wage/${empId}?skill=${skill}`, true)}>
              Check Minimum Wage
            </button>
            <Result data={out.minWage} />
          </div>
        </div>

        <div className="card">
          <h3 className="font-semibold mb-3">Full & Final Settlement</h3>
          <div className="space-y-3">
            <EmployeeSelect value={empId} onChange={setEmpId} />
            <div>
              <label className="label">Pending Leave Days</label>
              <input className="input" type="number" value={leaveDays}
                onChange={(e) => setLeaveDays(e.target.value)} />
            </div>
            <button className="btn btn-primary w-full"
              onClick={() => call('ff', `/api/calculations/full-final/${empId}?pendingLeaveDays=${leaveDays}`, true)}>
              Calculate F&F
            </button>
            <Result data={out.ff} />
          </div>
        </div>

        <div className="card">
          <h3 className="font-semibold mb-3">Reconciliation</h3>
          <p className="text-sm text-slate-500 mb-3">
            Finds missing PAN/UAN, wrong ESI flags and data errors before filing.
          </p>
          <button className="btn btn-primary w-full"
            onClick={() => call('recon', `/api/calculations/reconcile/${selected.id}`)}>
            Run Reconciliation
          </button>
          <Result data={out.recon} />
        </div>

        <div className="card">
          <h3 className="font-semibold mb-3">Contract Workers & UAN/KYC</h3>
          <p className="text-sm text-slate-500 mb-3">Principal employer liability + pending KYC tracking.</p>
          <div className="flex gap-2">
            <button className="btn btn-primary flex-1"
              onClick={() => call('contract', `/api/calculations/contract-workers/${selected.id}`)}>
              Contract Workers
            </button>
            <button className="btn btn-outline flex-1"
              onClick={() => call('uankyc', `/api/calculations/uan-kyc/${selected.id}`)}>
              UAN/KYC Status
            </button>
          </div>
          <Result data={out.contract} />
          <Result data={out.uankyc} />
        </div>
      </div>
    </div>
  )
}