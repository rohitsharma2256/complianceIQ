import { createContext, useContext, useState, useEffect, useCallback } from 'react'
import api from '../api/axios'
import { useAuth } from './AuthContext'

const CompanyContext = createContext()

export function CompanyProvider({ children }) {
  const { user } = useAuth()
  const [companies, setCompanies] = useState([])
  const [selected, setSelected] = useState(null)
  const [employees, setEmployees] = useState([])
  const [loading, setLoading] = useState(false)

  const selectCompany = (company) => {
    setSelected(company)
    if (company) localStorage.setItem('selectedCompanyId', company.id)
  }

  const loadCompanies = useCallback(async () => {
    const tenantId = localStorage.getItem('tenantId')
    if (!tenantId) return
    setLoading(true)
    try {
      const res = await api.get(`/api/companies/tenant/${tenantId}`)
      setCompanies(res.data)
      const savedId = localStorage.getItem('selectedCompanyId')
      const found = res.data.find((c) => c.id === savedId) || res.data[0]
      if (found) selectCompany(found)
      else setSelected(null)
    } catch {
      setCompanies([])
    } finally {
      setLoading(false)
    }
  }, [])

  const loadEmployees = useCallback(async () => {
    if (!selected) return setEmployees([])
    try {
      const res = await api.get(`/api/employees/company/${selected.id}`)
      setEmployees(res.data)
    } catch {
      setEmployees([])
    }
  }, [selected])

  useEffect(() => { if (user) loadCompanies() }, [user, loadCompanies])
  useEffect(() => { loadEmployees() }, [selected, loadEmployees])

  return (
    <CompanyContext.Provider value={{
      companies, selected, employees, loading,
      selectCompany, loadCompanies, loadEmployees,
    }}>
      {children}
    </CompanyContext.Provider>
  )
}

export const useCompany = () => useContext(CompanyContext)
