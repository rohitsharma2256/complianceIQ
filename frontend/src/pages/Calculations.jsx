import { useState } from 'react'
import api from '../api/axios'
import { useCompany } from '../context/CompanyContext'
import NoCompany from '../components/NoCompany'
import EmployeeSelect from '../components/EmployeeSelect'

// camelCase → "Camel Case"
const labelize = (k) =>
  k.replace(/([A-Z])/g, ' $1').replace(/^./, (c) => c.toUpperCase()).trim()

function Value({ v }) {
  if (typeof v === 'boolean')
    return (
      <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${
        v ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
        {v ? 'Yes' : 'No'}
      </span>
    )
  return <span className="font-medium text-slate-800">{String(v)}</span>
}

function Result({ data }) {
  if (!data) return null
  return (
    <div className="bg-slate-50 rounded-lg p-4 mt-3 text-sm space-y-1">
      {Object.entries(data).map(([k, v]) => {
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
                          <span className="text-xs text-right max-w-[60%]">
                            {Array.isArray(iv) ? iv.join(', ') : String(iv)}
                          </span>
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

export default function Calculations() {
  const { selected } = useCompany()
  const [empId, setEmpId] = useState('')
  const [bonusPct, setBonusPct] = useState(8.33)
  const [out, setOut] = useState({})
  const [msg, setMsg] = useState('')

  if (!selected) return <NoCompany />

  const call = async (key, url) => {
    if (!empId) return setMsg('Select an employee first')
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
      <h1 className="text-2xl font-bold text-slate-800 mb-1">Calculations</h1>
      <p className="text-slate-500 text-sm mb-4">Gratuity, Bonus & LWF — {selected.companyName}</p>

      <div className="card mb-4 max-w-md">
        <EmployeeSelect value={empId} onChange={setEmpId} />
      </div>

      {msg && <div className="bg-red-50 text-red-700 text-sm p-3 rounded-lg mb-4">{msg}</div>}

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="card">
          <h3 className="font-semibold mb-2">Gratuity</h3>
          <p className="text-xs text-slate-500 mb-3">5+ years service required</p>
          <button className="btn btn-primary w-full"
            onClick={() => call('gratuity', `/api/calculations/gratuity/${empId}`)}>Calculate</button>
          <Result data={out.gratuity} />
        </div>
        <div className="card">
          <h3 className="font-semibold mb-2">Bonus</h3>
          <label className="label">Bonus % (8.33 - 20)</label>
          <input className="input mb-3" type="number" step="0.01" value={bonusPct}
            onChange={(e) => setBonusPct(e.target.value)} />
          <button className="btn btn-primary w-full"
            onClick={() => call('bonus', `/api/calculations/bonus/${empId}?percent=${bonusPct}`)}>Calculate</button>
          <Result data={out.bonus} />
        </div>
        <div className="card">
          <h3 className="font-semibold mb-2">LWF</h3>
          <p className="text-xs text-slate-500 mb-3">Labour Welfare Fund (state-wise)</p>
          <button className="btn btn-primary w-full"
            onClick={() => call('lwf', `/api/calculations/lwf/${empId}`)}>Calculate</button>
          <Result data={out.lwf} />
        </div>
      </div>
    </div>
  )
}