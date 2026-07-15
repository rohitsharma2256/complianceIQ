const MONTHS = ['January','February','March','April','May','June',
  'July','August','September','October','November','December']

export default function MonthYear({ month, year, setMonth, setYear }) {
  const years = [2024, 2025, 2026, 2027]
  return (
    <>
      <div>
        <label className="label">Month</label>
        <select className="input" value={month}
          onChange={(e) => setMonth(Number(e.target.value))}>
          {MONTHS.map((m, i) => (
            <option key={m} value={i + 1}>{m}</option>
          ))}
        </select>
      </div>
      <div>
        <label className="label">Year</label>
        <select className="input" value={year}
          onChange={(e) => setYear(Number(e.target.value))}>
          {years.map((y) => <option key={y} value={y}>{y}</option>)}
        </select>
      </div>
    </>
  )
}
