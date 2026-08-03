import { useState } from 'react'
import api from '../api/axios'
import { useCompany } from '../context/CompanyContext'
import NoCompany from '../components/NoCompany'
import { Pencil, Trash2, X } from 'lucide-react'

const emptyForm = {
  // Identity
  employeeCode: '', fullName: '', email: '', phone: '',
  pan: '', uanNumber: '', esicIpNumber: '', aadhaarNumber: '',
  // Dates
  dateOfBirth: '', dateOfJoining: '',
  // Organisation
  designation: '', department: '', workLocation: '', workState: '',
  // Bank
  bankName: '', bankAccountNumber: '', bankIfsc: '',
  // Salary
  basicSalary: '', hra: '', conveyanceAllowance: '',
  specialAllowance: '', medicalAllowance: '', otherAllowance: '',
  totalCtc: '',
  // Flags
  pfApplicable: true, ptApplicable: true, taxRegime: 'NEW',
}

// PT/LWF state ke hisaab se lagta hai - isliye dropdown
const STATES = [
  'MAHARASHTRA', 'KARNATAKA', 'TELANGANA', 'ANDHRA PRADESH', 'TAMIL NADU',
  'KERALA', 'GUJARAT', 'WEST BENGAL', 'MADHYA PRADESH', 'ODISHA',
  'BIHAR', 'ASSAM', 'JHARKHAND',
  'HARYANA', 'DELHI', 'UTTAR PRADESH', 'RAJASTHAN', 'PUNJAB',
  'UTTARAKHAND', 'HIMACHAL PRADESH', 'GOA', 'CHANDIGARH',
]
const NO_PT_STATES = ['HARYANA', 'DELHI', 'UTTAR PRADESH', 'RAJASTHAN', 'PUNJAB',
                      'UTTARAKHAND', 'HIMACHAL PRADESH', 'GOA', 'CHANDIGARH']

    function Field({ name, label, type = 'text', required, placeholder, value, onChange }) {
  return (
    <div>
      <label className="label">{label}</label>
      <input className="input" name={name} type={type} placeholder={placeholder}
        value={value} onChange={onChange} required={required} />
    </div>
  )
}

export default function Employees() {
  const { selected, employees, loadEmployees } = useCompany()
  const [tab, setTab] = useState('list')
  const [msg, setMsg] = useState('')
  const [file, setFile] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [editId, setEditId] = useState(null)

  if (!selected) return <NoCompany />

  const change = (e) => {
    const { name, value, type, checked } = e.target
    setForm({ ...form, [name]: type === 'checkbox' ? checked : value })
  }

  // Live gross - ESI aur PT dono isi pe lagte hain
  const gross = ['basicSalary', 'hra', 'conveyanceAllowance', 'specialAllowance',
                 'medicalAllowance', 'otherAllowance']
                .reduce((sum, k) => sum + Number(form[k] || 0), 0)

  const startEdit = (emp) => {
    setEditId(emp.id)
    setForm({
      employeeCode: emp.employeeCode || '', fullName: emp.fullName || '',
      email: emp.email || '', phone: emp.phone || '',
      pan: emp.pan || '', uanNumber: emp.uanNumber || '',
      esicIpNumber: emp.esicIpNumber || '', aadhaarNumber: emp.aadhaarNumber || '',
      dateOfBirth: emp.dateOfBirth || '', dateOfJoining: emp.dateOfJoining || '',
      designation: emp.designation || '', department: emp.department || '',
      workLocation: emp.workLocation || '', workState: emp.workState || '',
      bankName: emp.bankName || '', bankAccountNumber: emp.bankAccountNumber || '',
      bankIfsc: emp.bankIfsc || '',
      basicSalary: emp.basicSalary || '', hra: emp.hra || '',
      conveyanceAllowance: emp.conveyanceAllowance || '',
      specialAllowance: emp.specialAllowance || '',
      medicalAllowance: emp.medicalAllowance || '',
      otherAllowance: emp.otherAllowance || '', totalCtc: emp.totalCtc || '',
      pfApplicable: emp.pfApplicable ?? true,
      ptApplicable: emp.ptApplicable ?? true,
      taxRegime: emp.taxRegime || 'NEW',
    })
    setTab('add')
  }

  const cancelEdit = () => { setEditId(null); setForm(emptyForm); setTab('list') }

  const save = async (e) => {
    e.preventDefault()
    try {
      if (editId) {
        await api.put(`/api/employees/${editId}`, form)
        setMsg('Employee updated!')
      } else {
        await api.post('/api/employees', { ...form, companyId: selected.id })
        setMsg('Employee added!')
      }
      cancelEdit()
      loadEmployees()
    } catch (err) {
      setMsg(err.response?.data?.message || err.response?.data?.error || 'Failed to save')
    }
  }

  const remove = async (emp) => {
    if (!confirm(`Remove ${emp.fullName}?`)) return
    try {
      await api.delete(`/api/employees/${emp.id}`)
      setMsg(`${emp.fullName} removed`)
      loadEmployees()
    } catch (err) {
      setMsg(err.response?.data?.message || 'Failed to remove')
    }
  }

  const uploadExcel = async (e) => {
    e.preventDefault()
    if (!file) return setMsg('Select an Excel file first')
    const fd = new FormData()
    fd.append('file', file)
    try {
      const res = await api.post(`/api/employees/upload/${selected.id}`, fd)
      setMsg(res.data.result || 'Uploaded!')
      setFile(null)
      loadEmployees()
      setTab('list')
    } catch (err) {
      setMsg(err.response?.data?.message || 'Upload failed')
    }
  }


  return (
    <div>
      <h1 className="text-2xl font-bold text-slate-800 mb-1">Employees</h1>
      <p className="text-slate-500 text-sm mb-4">
        {employees.length} employees in {selected.companyName}
      </p>

      {msg && <div className="bg-blue-50 text-blue-700 text-sm p-3 rounded-lg mb-4">{msg}</div>}

      <div className="flex gap-2 mb-4">
        <button onClick={() => setTab('list')}
          className={`btn ${tab === 'list' ? 'btn-primary' : 'btn-outline'}`}>Employee List</button>
        <button onClick={() => { setEditId(null); setForm(emptyForm); setTab('add') }}
          className={`btn ${tab === 'add' ? 'btn-primary' : 'btn-outline'}`}>
          {editId ? 'Edit Employee' : '+ Add Employee'}</button>
        <button onClick={() => setTab('upload')}
          className={`btn ${tab === 'upload' ? 'btn-primary' : 'btn-outline'}`}>Excel Upload</button>
      </div>

      {/* ==================== ADD / EDIT FORM ==================== */}
      {tab === 'add' && (
        <form onSubmit={save} className="card space-y-5">

          {editId && (
            <div className="flex items-center justify-between bg-amber-50 p-2 rounded">
              <span className="text-sm text-amber-700">Editing: {form.fullName}</span>
              <button type="button" onClick={cancelEdit}><X size={16} /></button>
            </div>
          )}

          {/* ---------- Basic Details ---------- */}
          <div>
            <h3 className="font-semibold text-slate-700 mb-2">Basic Details</h3>
           {/* ---------- Basic Details ---------- */}
<div className="grid grid-cols-1 md:grid-cols-3 gap-4">
  <Field name="employeeCode" label="Employee Code" placeholder="EMP001"
         value={form.employeeCode} onChange={change} />
  <Field name="fullName" label="Full Name *" required
         value={form.fullName} onChange={change} />
  <Field name="email" label="Email" type="email"
         value={form.email} onChange={change} />
  <Field name="phone" label="Phone"
         value={form.phone} onChange={change} />
  <Field name="dateOfBirth" label="Date of Birth" type="date"
         value={form.dateOfBirth} onChange={change} />
  <Field name="dateOfJoining" label="Date of Joining" type="date"
         value={form.dateOfJoining} onChange={change} />
</div>

{/* ---------- Statutory IDs ---------- */}
<div className="grid grid-cols-1 md:grid-cols-4 gap-4">
  <Field name="pan" label="PAN" placeholder="ABCDE1234F"
         value={form.pan} onChange={change} />
  <Field name="uanNumber" label="UAN (12 digits)"
         value={form.uanNumber} onChange={change} />
  <Field name="esicIpNumber" label="ESIC IP Number"
         value={form.esicIpNumber} onChange={change} />
  <Field name="aadhaarNumber" label="Aadhaar (optional)"
         value={form.aadhaarNumber} onChange={change} />
</div>

{/* ---------- Organisation ---------- */}
<div className="grid grid-cols-1 md:grid-cols-4 gap-4">
  <Field name="designation" label="Designation"
         value={form.designation} onChange={change} />
  <Field name="department" label="Department"
         value={form.department} onChange={change} />
  <Field name="workLocation" label="Work Location"
         value={form.workLocation} onChange={change} />
  {/* work state ka select waise hi rehne do */}
  <div>
    <label className="label">Work State *</label>
    <select className="input" name="workState" value={form.workState}
      onChange={change} required>
      <option value="">Select state</option>
      {STATES.map(s => (
        <option key={s} value={s}>
          {s}{NO_PT_STATES.includes(s) ? ' (PT: Nil)' : ''}
        </option>
      ))}
    </select>
  </div>
</div>

{/* ---------- Bank ---------- */}
<div className="grid grid-cols-1 md:grid-cols-3 gap-4">
  <Field name="bankName" label="Bank Name"
         value={form.bankName} onChange={change} />
  <Field name="bankAccountNumber" label="Account Number"
         value={form.bankAccountNumber} onChange={change} />
  <Field name="bankIfsc" label="IFSC"
         value={form.bankIfsc} onChange={change} />
</div>

{/* ---------- Salary ---------- */}
<div className="grid grid-cols-1 md:grid-cols-4 gap-4">
  <Field name="basicSalary" label="Basic Salary *" type="number" required
         value={form.basicSalary} onChange={change} />
  <Field name="hra" label="HRA" type="number"
         value={form.hra} onChange={change} />
  <Field name="conveyanceAllowance" label="Conveyance" type="number"
         value={form.conveyanceAllowance} onChange={change} />
  <Field name="specialAllowance" label="Special Allowance" type="number"
         value={form.specialAllowance} onChange={change} />
  <Field name="medicalAllowance" label="Medical Allowance" type="number"
         value={form.medicalAllowance} onChange={change} />
  <Field name="otherAllowance" label="Other Allowance" type="number"
         value={form.otherAllowance} onChange={change} />
  <Field name="totalCtc" label="Total CTC *" type="number" required
         value={form.totalCtc} onChange={change} />
  <div>
    <label className="label">Tax Regime</label>
    <select className="input" name="taxRegime" value={form.taxRegime} onChange={change}>
      <option value="NEW">New Regime</option>
      <option value="OLD">Old Regime</option>
    </select>
  </div>
</div>

            {/* Live gross preview */}
            <div className="bg-blue-50 p-3 rounded-lg mt-3 text-sm">
              <b>Monthly Gross:</b> ₹{gross.toLocaleString('en-IN')}
              <span className="text-slate-500 ml-2">
                — ESI eligibility and PT are calculated on this, not on CTC
              </span>
              {gross > 0 && gross <= 21000 && (
                <span className="text-green-700 ml-2">· ESI applicable</span>
              )}
              {gross > 21000 && (
                <span className="text-slate-500 ml-2">· ESI not applicable (above ₹21,000)</span>
              )}
            </div>
          </div>

          {/* ---------- Flags ---------- */}
          <div className="flex gap-6">
            <label className="flex items-center gap-2">
              <input type="checkbox" name="pfApplicable" checked={form.pfApplicable}
                onChange={change} />
              <span className="text-sm">PF Applicable</span>
            </label>
            <label className="flex items-center gap-2">
              <input type="checkbox" name="ptApplicable" checked={form.ptApplicable}
                onChange={change} />
              <span className="text-sm">PT Applicable</span>
            </label>
          </div>

          <div>
            <button className="btn btn-primary">
              {editId ? 'Update Employee' : 'Add Employee'}</button>
          </div>
        </form>
      )}

      {/* ==================== EXCEL UPLOAD ==================== */}
      {tab === 'upload' && (
        <form onSubmit={uploadExcel} className="card">
          <p className="text-sm text-slate-500 mb-2">
            Employees will be added to <b>{selected.companyName}</b>. Row 1 must be headers.
          </p>
          <div className="bg-slate-50 p-3 rounded text-xs font-mono mb-3 overflow-x-auto">
            Employee Code | Full Name | Email | Phone | PAN | UAN | ESIC IP |<br />
            DOB | DOJ | Designation | Department | Work State | Bank Name |<br />
            Account No | IFSC | Basic | HRA | Conveyance | Special | Medical |<br />
            Other | Total CTC | Tax Regime
          </div>
          <input type="file" accept=".xlsx,.xls"
            onChange={(e) => setFile(e.target.files[0])} className="mb-3" />
          <div><button className="btn btn-primary">Upload Excel</button></div>
        </form>
      )}

      {/* ==================== LIST ==================== */}
      {tab === 'list' && (
        <div className="card overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left border-b text-slate-500">
                <th className="py-2">Code</th><th>Name</th><th>Designation</th>
                <th>Basic</th><th>Gross</th><th>State</th>
                <th>PAN</th><th>UAN</th><th>ESI</th><th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {employees.map((e) => {
                const g = Number(e.basicSalary || 0) + Number(e.hra || 0) +
                          Number(e.conveyanceAllowance || 0) + Number(e.specialAllowance || 0) +
                          Number(e.medicalAllowance || 0) + Number(e.otherAllowance || 0)
                return (
                  <tr key={e.id} className="border-b hover:bg-slate-50">
                    <td className="py-2 text-slate-500">{e.employeeCode || '-'}</td>
                    <td className="font-medium">{e.fullName}</td>
                    <td>{e.designation || '-'}</td>
                    <td>₹{e.basicSalary}</td>
                    <td>₹{g.toLocaleString('en-IN')}</td>
                    <td>{e.workState}</td>
                    <td>{e.pan
                      ? <span className="text-green-600">✓</span>
                      : <span className="text-red-500" title="Form 16 needs PAN">✗</span>}</td>
                    <td>{e.uanNumber
                      ? <span className="text-green-600">✓</span>
                      : <span className="text-red-500" title="ECR needs UAN">✗</span>}</td>
                    <td>{e.isEsiApplicable ? 'Yes' : 'No'}</td>
                    <td>
                      <div className="flex gap-2">
                        <button onClick={() => startEdit(e)} title="Edit"
                          className="text-brand-500 hover:text-brand-700"><Pencil size={16} /></button>
                        <button onClick={() => remove(e)} title="Remove"
                          className="text-red-500 hover:text-red-700"><Trash2 size={16} /></button>
                      </div>
                    </td>
                  </tr>
                )
              })}
              {employees.length === 0 && (
                <tr><td colSpan="10" className="py-4 text-slate-400">
                  No employees yet. Add one or upload Excel.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}