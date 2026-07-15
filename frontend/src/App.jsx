import { Routes, Route } from 'react-router-dom'
import ProtectedRoute from './components/ProtectedRoute'
import Layout from './components/Layout'
import Login from './pages/Login'
import Register from './pages/Register'
import Dashboard from './pages/Dashboard'
import Companies from './pages/Companies'
import Employees from './pages/Employees'
import Compliance from './pages/Compliance'
import Reports from './pages/Reports'
import Payslips from './pages/Payslips'
import Forms from './pages/Forms'
import Calculations from './pages/Calculations'
import Checks from './pages/Checks'
import Deadlines from './pages/Deadlines'
import AiChat from './pages/AiChat'
import AskLaw from './pages/AskLaw'
import LawUpdates from './pages/LawUpdates'

const wrap = (Page) => (
  <ProtectedRoute><Layout><Page /></Layout></ProtectedRoute>
)

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/" element={wrap(Dashboard)} />
      <Route path="/companies" element={wrap(Companies)} />
      <Route path="/employees" element={wrap(Employees)} />
      <Route path="/compliance" element={wrap(Compliance)} />
      <Route path="/reports" element={wrap(Reports)} />
      <Route path="/payslips" element={wrap(Payslips)} />
      <Route path="/forms" element={wrap(Forms)} />
      <Route path="/calculations" element={wrap(Calculations)} />
      <Route path="/checks" element={wrap(Checks)} />
      <Route path="/deadlines" element={wrap(Deadlines)} />
      <Route path="/ai-chat" element={wrap(AiChat)} />
      <Route path="/ask-law" element={wrap(AskLaw)} />
      <Route path="/law-updates" element={wrap(LawUpdates)} />
    </Routes>
  )
}
