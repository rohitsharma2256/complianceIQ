/**
 * Month + Year picker.
 * Future period select nahi ho sakta - payslip/compliance kisi aane wale
 * mahine ka hota hi nahi. Server pe bhi validate hota hai.
 */
export default function MonthYear({ month, year, setMonth, setYear, yearsBack = 3 }) {
  const now = new Date()
  const currentMonth = now.getMonth() + 1
  const currentYear = now.getFullYear()

  const isFuture = (m, y) => y > currentYear || (y === currentYear && m > currentMonth)

  const years = Array.from({ length: yearsBack + 1 }, (_, i) => currentYear - i)

  const onYearChange = (y) => {
    setYear(y)
    // Year badla aur ab month future ho gaya -> valid month pe le aao
    if (isFuture(month, y)) setMonth(y === currentYear ? currentMonth : 12)
  }

  return (
    <>
      <div>
        <label className="label">Month</label>
        <select className="input" value={month} onChange={(e) => setMonth(+e.target.value)}>
          {[...Array(12)].map((_, i) => {
            const m = i + 1
            const disabled = isFuture(m, year)
            return (
              <option key={m} value={m} disabled={disabled}>
                {new Date(2000, i).toLocaleString('en', { month: 'long' })}
                {disabled ? ' (upcoming)' : ''}
              </option>
            )
          })}
        </select>
      </div>

      <div>
        <label className="label">Year</label>
        <select className="input" value={year} onChange={(e) => onYearChange(+e.target.value)}>
          {years.map((y) => <option key={y} value={y}>{y}</option>)}
        </select>
      </div>
    </>
  )
}