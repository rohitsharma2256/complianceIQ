import { useState } from 'react'
import api from '../api/axios'
import { useCompany } from '../context/CompanyContext'
import { Send, Bot, User, Zap, Download } from 'lucide-react'

// AI answer mein report link dhundo
const findReportId = (text) => {
  const m = text?.match(/\/api\/reports\/compliance\/([0-9a-f-]{36})/i)
  return m ? m[1] : null
}

export default function AiChat() {
  const { selected } = useCompany()
  const now = new Date()
  const [messages, setMessages] = useState([
    { role: 'ai', text: 'Hi! I\'m your compliance assistant. I can check compliance, find violations, generate downloadable reports and answer payroll questions — all for your selected company. Use the quick actions below or just ask!' }
  ])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)

  const downloadReport = async (runId) => {
    try {
      const res = await api.get(`/api/reports/compliance/${runId}`, { responseType: 'blob' })
      const a = document.createElement('a')
      a.href = window.URL.createObjectURL(new Blob([res.data]))
      a.download = 'compliance-report.pdf'
      a.click()
    } catch { /* ignore */ }
  }

  const askAgent = async (question, displayText) => {
    setMessages((m) => [...m, { role: 'user', text: displayText || question }])
    setLoading(true)
    try {
      const res = await api.post('/api/ai/agent', { question })
      const answer = res.data.answer
      const reportId = findReportId(answer)
      setMessages((m) => [...m, { role: 'ai', text: answer, reportId }])
    } catch (err) {
      const raw = JSON.stringify(err.response?.data || '')
      const text = raw.includes('rate_limit') || raw.includes('429')
        ? 'AI is busy right now (free tier limit). Please wait a minute and try again.'
        : 'Something went wrong. Please try again.'
      setMessages((m) => [...m, { role: 'ai', text }])
    } finally {
      setLoading(false)
    }
  }

  const send = () => {
    if (!input.trim()) return
    let q = input
    if (selected) {
      q = `${input}\n\n(Context: The user's selected company is "${selected.companyName}" with company ID ${selected.id}. Use this company for any company-related actions unless another is specified.)`
    }
    askAgent(q, input)
    setInput('')
  }

  const month = now.getMonth() + 1
  const year = now.getFullYear()

  const quickActions = selected ? [
    { label: 'Check Compliance',
      q: `Run compliance check for company ${selected.id} for month ${month} year ${year}`,
      d: `Check compliance for ${selected.companyName} (${month}/${year})` },
    { label: 'Show Violations',
      q: `Run compliance check for company ${selected.id} for month ${month} year ${year} and show me all violations with fixes`,
      d: `Show violations for ${selected.companyName}` },
    { label: 'Generate Report',
      q: `Generate a compliance report for company ${selected.id} for month ${month} year ${year}. Include the download link in your answer.`,
      d: `Generate report for ${selected.companyName}` },
    { label: 'Upcoming Deadlines',
      q: 'What compliance deadlines are coming up? List each with days remaining.',
      d: 'What deadlines are coming up?' },
  ] : []

  return (
    <div>
      <h1 className="text-2xl font-bold text-slate-800 mb-1">AI Assistant</h1>
      <p className="text-slate-500 text-sm mb-4">
        {selected ? `Working on: ${selected.companyName}` : 'Select a company from the sidebar for company actions'}
      </p>

      {quickActions.length > 0 && (
        <div className="flex flex-wrap gap-2 mb-3">
          {quickActions.map((a) => (
            <button key={a.label} disabled={loading}
              onClick={() => askAgent(a.q, a.d)}
              className="flex items-center gap-1 text-xs bg-brand-50 text-brand-600 px-3 py-1.5 rounded-full hover:bg-brand-100 disabled:opacity-50">
              <Zap size={12} /> {a.label}
            </button>
          ))}
        </div>
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
          {loading && <p className="text-slate-400 text-sm">AI is working...</p>}
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