import { useState } from 'react'
import api from '../api/axios'
import { useCompany } from '../context/CompanyContext'
import { Send, Bot, User, Zap, Download } from 'lucide-react'

// AI answer mein report link dhundo
const findReportId = (text) => {
  const m = text?.match(/\/api\/reports\/compliance\/([0-9a-f-]{36})/i)
  return m ? m[1] : null
}

const inr = (v) => `₹${Number(v || 0).toLocaleString('en-IN')}`

export default function AiChat() {
  const { selected } = useCompany()
  const now = new Date()
  const month = now.getMonth() + 1
  const year = now.getFullYear()

  const [messages, setMessages] = useState([
    { role: 'ai', text: "Hi! I'm your compliance assistant. The quick actions below run instantly on your data. You can also ask me anything in your own words — I'll use the tools I need." }
  ])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [lastRunId, setLastRunId] = useState(null)

  const push = (role, text, extra = {}) =>
    setMessages((m) => [...m, { role, text, ...extra }])

  const downloadReport = async (runId) => {
    try {
      const res = await api.get(`/api/reports/compliance/${runId}`, { responseType: 'blob' })
      const a = document.createElement('a')
      a.href = window.URL.createObjectURL(new Blob([res.data]))
      a.download = 'compliance-report.pdf'
      a.click()
    } catch {
      push('ai', 'Could not download the report.')
    }
  }

  /* ============================================================
     QUICK ACTIONS - AI ke BINA, seedha REST API (0 tokens)
     Frontend ko company aur period pata hai - LLM ki zarurat nahi
     ============================================================ */
  const quickActions = selected ? [
    {
      label: 'Check Compliance',
      run: async () => {
        const r = await api.post(
          `/api/compliance/check/${selected.id}?month=${month}&year=${year}`)
        const d = r.data
        if (d?.id) setLastRunId(d.id)
        return {
          text:
`Compliance check complete — ${selected.companyName} (${month}/${year})

- Employees: ${d.totalEmployees}
- EPF: ${inr(d.totalEpfEmployee)} employee + ${inr(d.totalEpfEmployer)} employer
- ESI: ${inr(d.totalEsiEmployee)} employee + ${inr(d.totalEsiEmployer)} employer
- TDS: ${inr(d.totalTds)}
- Professional Tax: ${inr(d.totalProfessionalTax)}
- Status: ${d.status}

Use "Show Violations" to see what needs fixing.`
        }
      }
    },
    {
      label: 'Show Violations',
      run: async () => {
        let runId = lastRunId
        // Run nahi hua toh pehle chala do
        if (!runId) {
          const c = await api.post(
            `/api/compliance/check/${selected.id}?month=${month}&year=${year}`)
          runId = c.data?.id
          setLastRunId(runId)
        }
        const r = await api.get(`/api/compliance/violations/${runId}`)
        const list = Array.isArray(r.data) ? r.data : []
        if (!list.length) return { text: 'No open violations found. Everything looks compliant.' }

        const high = list.filter(v => v.severity === 'HIGH').length
        return {
          text:
`${list.length} item(s) found${high ? ` — ${high} high severity` : ''}:\n\n` +
            list.map(v =>
              `• [${v.severity}] ${v.description}\n   Fix: ${v.recommendedFix}`
            ).join('\n\n')
        }
      }
    },
    {
      label: 'Generate Report',
      run: async () => {
        let runId = lastRunId
        if (!runId) {
          const c = await api.post(
            `/api/compliance/check/${selected.id}?month=${month}&year=${year}`)
          runId = c.data?.id
          setLastRunId(runId)
        }
        return {
          text: `Compliance report is ready for ${selected.companyName} (${month}/${year}).`,
          reportId: runId
        }
      }
    },
    {
      label: 'Upcoming Deadlines',
      run: async () => {
        const r = await api.get('/api/deadlines')
        const list = Array.isArray(r.data) ? r.data : (r.data?.deadlines || [])
        return {
          text: list.length
            ? 'Upcoming statutory deadlines:\n\n' +
              list.map(d => `• ${typeof d === 'string' ? d : JSON.stringify(d)}`).join('\n')
            : 'No deadlines in the next few days.'
        }
      }
    },
  ] : []

  const runQuickAction = async (action) => {
    push('user', action.label)
    setLoading(true)
    try {
      const { text, reportId } = await action.run()
      push('ai', text, reportId ? { reportId } : {})
    } catch (err) {
      push('ai', err.response?.data?.error
        || err.response?.data?.message
        || 'Could not complete that action.')
    } finally {
      setLoading(false)
    }
  }

  /* ============================================================
     FREE-TEXT QUESTIONS - yahin AI actually value deta hai
     ============================================================ */
  const askAgent = async (question, displayText) => {
    push('user', displayText || question)
    setLoading(true)
    try {
      const res = await api.post('/api/ai/agent', { question })
      const answer = res.data.answer
      push('ai', answer, { reportId: findReportId(answer) })
    } catch (err) {
      const raw = JSON.stringify(err.response?.data || '')
      push('ai', raw.includes('rate_limit') || raw.includes('429')
        ? 'The AI is rate-limited right now. Please wait about half a minute and try again — the quick actions above still work instantly.'
        : 'Something went wrong. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  const send = () => {
    if (!input.trim()) return
    let q = input
    if (selected) {
      q = `${input}\n\n(Context: selected company is "${selected.companyName}", id ${selected.id}. Current period ${month}/${year}.)`
    }
    askAgent(q, input)
    setInput('')
  }

  return (
    <div>
      <h1 className="text-2xl font-bold text-slate-800 mb-1">AI Assistant</h1>
      <p className="text-slate-500 text-sm mb-4">
        {selected
          ? `Working on: ${selected.companyName}`
          : 'Select a company from the sidebar for company actions'}
      </p>

      {quickActions.length > 0 && (
        <div className="flex flex-wrap gap-2 mb-1">
          {quickActions.map((a) => (
            <button key={a.label} disabled={loading}
              onClick={() => runQuickAction(a)}
              className="flex items-center gap-1 text-xs bg-brand-50 text-brand-600 px-3 py-1.5 rounded-full hover:bg-brand-100 disabled:opacity-50">
              <Zap size={12} /> {a.label}
            </button>
          ))}
        </div>
      )}
      {quickActions.length > 0 && (
        <p className="text-xs text-slate-400 mb-3">
          Quick actions run directly on your data — instant, and they don't use the AI quota.
        </p>
      )}

      <div className="card h-[55vh] flex flex-col">
        <div className="flex-1 overflow-y-auto space-y-4 mb-4">
          {messages.map((m, i) => (
            <div key={i} className={`flex gap-3 ${m.role === 'user' ? 'flex-row-reverse' : ''}`}>
              <div className={`p-2 rounded-full h-9 w-9 flex items-center justify-center shrink-0 ${
                m.role === 'user' ? 'bg-brand-500 text-white' : 'bg-slate-200 text-slate-600'}`}>
                {m.role === 'user' ? <User size={18} /> : <Bot size={18} />}
              </div>
              <div className={`max-w-[75%] p-3 rounded-lg text-sm whitespace-pre-wrap ${
                m.role === 'user' ? 'bg-brand-500 text-white' : 'bg-slate-100 text-slate-800'}`}>
                {m.text}
                {m.reportId && (
                  <button onClick={() => downloadReport(m.reportId)}
                    className="mt-3 flex items-center gap-2 bg-brand-500 text-white text-xs px-3 py-2 rounded-lg hover:bg-brand-600">
                    <Download size={14} /> Download Report PDF
                  </button>
                )}
              </div>
            </div>
          ))}
          {loading && <p className="text-slate-400 text-sm">Working...</p>}
        </div>
        <div className="flex gap-2">
          <input className="input" placeholder="Ask anything — no IDs needed..."
            value={input} onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && send()} />
          <button className="btn btn-primary" onClick={send} disabled={loading}>
            <Send size={18} />
          </button>
        </div>
      </div>
    </div>
  )
}