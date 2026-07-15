import { useCompany } from '../context/CompanyContext'

export default function EmployeeSelect({ value, onChange, label = 'Employee' }) {
  const { employees } = useCompany()

  return (
    <div>
      <label className="label">{label}</label>
      <select className="input" value={value || ''}
        onChange={(e) => onChange(e.target.value)}>
        <option value="">-- Select employee --</option>
        {employees.map((e) => (
          <option key={e.id} value={e.id}>
            {e.fullName}{e.employeeCode ? ` (${e.employeeCode})` : ''}
          </option>
        ))}
      </select>
    </div>
  )
}
