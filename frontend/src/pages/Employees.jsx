import { useState } from 'react'
import api from '../api/axios'
import { useCompany } from '../context/CompanyContext'
import NoCompany from '../components/NoCompany'
import { Pencil, Trash2, X } from 'lucide-react'

const emptyForm = {
  fullName: '', employeeCode: '', designation: '', basicSalary: '',
  hra: '', specialAllowance: '', totalCtc: '', panNumber: '',
  uanNumber: '', workState: '', dateOfJoining: '', employmentType: 'PERMANENT'
}

export default function Employees() {
  const { selected, employees, loadEmployees } = useCompany()
  const [tab, setTab] = useState('list')
  const [msg, setMsg] = useState('')
  const [file, setFile] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [editId, setEditId] = useState(null)

  if (!selected) return <NoCompany />

  const change = (e) => setForm({ ...form, [e.target.name]: e.target.value })

  const startEdit = (emp) => {
    setEditId(emp.id)
    setForm({
      fullName: emp.fullName || '', employeeCode: emp.employeeCode || '',
      designation: emp.designation || '', basicSalary: emp.basicSalary || '',
      hra: emp.hra || '', specialAllowance: emp.specialAllowance || '',
      totalCtc: emp.totalCtc || '', panNumber: emp.panNumber || '',
      uanNumber: emp.uanNumber || '', workState: emp.workState || '',
      dateOfJoining: emp.dateOfJoining || '', employmentType: emp.employmentType || 'PERMANENT'
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
      setMsg(err.response?.data?.message || 'Failed to save')
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
      <p className="text-slate-500 text-sm mb-4">{employees.length} employees in {selected.companyName}</p>

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

      {tab === 'add' && (
        <form onSubmit={save} className="card grid grid-cols-1 md:grid-cols-3 gap-4">
          {editId && (
            <div className="md:col-span-3 flex items-center justify-between bg-amber-50 p-2 rounded">
              <span className="text-sm text-amber-700">Editing: {form.fullName}</span>
              <button type="button" onClick={cancelEdit}><X size={16} /></button>
            </div>
          )}
          {[
            { name: 'fullName', label: 'Full Name *' },
            { name: 'employeeCode', label: 'Employee Code' },
            { name: 'designation', label: 'Designation' },
            { name: 'basicSalary', label: 'Basic Salary *', type: 'number' },
            { name: 'hra', label: 'HRA', type: 'number' },
            { name: 'specialAllowance', label: 'Special Allowance', type: 'number' },
            { name: 'totalCtc', label: 'Total CTC *', type: 'number' },
            { name: 'panNumber', label: 'PAN Number' },
            { name: 'uanNumber', label: 'UAN Number' },
            { name: 'workState', label: 'Work State *' },
            { name: 'dateOfJoining', label: 'Date of Joining', type: 'date' },
          ].map((f) => (
            <div key={f.name}>
              <label className="label">{f.label}</label>
              <input className="input" name={f.name} type={f.type || 'text'}
                value={form[f.name]} onChange={change} required={f.label.includes('*')} />
            </div>
          ))}
          <div>
            <label className="label">Employment Type</label>
            <select className="input" name="employmentType" value={form.employmentType} onChange={change}>
              <option>PERMANENT</option><option>CONTRACT</option><option>INTERN</option>
            </select>
          </div>
          <div className="md:col-span-3">
            <button className="btn btn-primary">{editId ? 'Update Employee' : 'Add Employee'}</button>
          </div>
        </form>
      )}

      {tab === 'upload' && (
        <form onSubmit={uploadExcel} className="card">
          <p className="text-sm text-slate-500 mb-3">
            Excel columns in order: <b>Name | Basic | HRA | Special | CTC | PAN | State</b> (row 1 = headers).
            Employees will be added to <b>{selected.companyName}</b>.
          </p>
          <input type="file" accept=".xlsx,.xls" onChange={(e) => setFile(e.target.files[0])} className="mb-3" />
          <div><button className="btn btn-primary">Upload Excel</button></div>
        </form>
      )}

      {tab === 'list' && (
        <div className="card overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left border-b text-slate-500">
                <th className="py-2">Name</th><th>Designation</th><th>Basic</th>
                <th>CTC</th><th>State</th><th>ESI</th><th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {employees.map((e) => (
                <tr key={e.id} className="border-b hover:bg-slate-50">
                  <td className="py-2 font-medium">{e.fullName}</td>
                  <td>{e.designation || '-'}</td>
                  <td>₹{e.basicSalary}</td>
                  <td>₹{e.totalCtc}</td>
                  <td>{e.workState}</td>
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
              ))}
              {employees.length === 0 && (
                <tr><td colSpan="7" className="py-4 text-slate-400">
                  No employees yet. Add one or upload Excel.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
