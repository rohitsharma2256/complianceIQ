import { useState } from 'react'
import api from '../api/axios'
import { BookOpen } from 'lucide-react'

export default function AskLaw() {
  const [question, setQuestion] = useState('')
  const [answer, setAnswer] = useState('')
  const [loading, setLoading] = useState(false)

  const ask = async () => {
    if (!question.trim()) return
    setLoading(true); setAnswer('')
    try {
      const res = await api.post('/api/ai/ask-rag', { question })
      setAnswer(res.data.answer)
    } catch (err) {
      const raw = JSON.stringify(err.response?.data || '')
      setAnswer(raw.includes('rate_limit') || raw.includes('429')
        ? 'AI is busy (free tier limit). Wait a minute and try again.'
        : 'Something went wrong. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  const samples = [
    'What is the ESI contribution rate and applicability limit?',
    'What are the new tax regime slabs for 2025-26?',
    'How is gratuity calculated in India?',
    'What is professional tax in Karnataka?',
    'What is the penalty for late EPF payment?',
  ]

  return (
    <div>
      <h1 className="text-2xl font-bold text-slate-800 mb-1">Ask Law</h1>
      <p className="text-slate-500 mb-4 text-sm">
        Answers come from verified law documents with source citations — kept up to date with the latest notifications.
      </p>

      <div className="card mb-4">
        <textarea className="input h-24 mb-3" placeholder="Ask any Indian payroll compliance / law question..."
          value={question} onChange={(e) => setQuestion(e.target.value)} />
        <button className="btn btn-primary" onClick={ask} disabled={loading}>
          {loading ? 'Searching law documents...' : 'Ask'}
        </button>
        <div className="flex flex-wrap gap-2 mt-3">
          {samples.map((s) => (
            <button key={s} className="text-xs bg-slate-100 hover:bg-slate-200 px-3 py-1 rounded-full"
              onClick={() => setQuestion(s)}>{s}</button>
          ))}
        </div>
      </div>

      {answer && (
        <div className="card">
          <h3 className="font-semibold mb-2 flex items-center gap-2"><BookOpen size={18} /> Answer</h3>
          <p className="text-sm text-slate-700 whitespace-pre-wrap">{answer}</p>
        </div>
      )}
    </div>
  )
}
