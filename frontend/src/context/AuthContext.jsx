import { createContext, useContext, useState, useEffect } from 'react'

const AuthContext = createContext()

/** JWT ka exp claim padho - expired hai toh session shuru hi mat karo */
const isTokenValid = (token) => {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return payload.exp * 1000 > Date.now()
  } catch {
    return false
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const token = localStorage.getItem('token')
    const saved = localStorage.getItem('user')

    // Purana token pada hai toh session mat banao - warna UI logged-in
    // dikhega par har API call fail hoti rahegi
    if (!token || !isTokenValid(token)) {
      localStorage.clear()
      return null
    }
    return saved ? JSON.parse(saved) : null
  })

  /* App khula rehne ke dauran token expire ho jaaye toh bhi pakdo */
  useEffect(() => {
    const token = localStorage.getItem('token')
    if (!token) return

    let expiryMs
    try {
      expiryMs = JSON.parse(atob(token.split('.')[1])).exp * 1000 - Date.now()
    } catch {
      return
    }
    if (expiryMs <= 0) return

    const timer = setTimeout(() => {
      localStorage.clear()
      setUser(null)
      window.location.href = '/login?expired=1'
    }, expiryMs)

    return () => clearTimeout(timer)
  }, [user])

  const login = (data) => {
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(data))
    localStorage.setItem('tenantId', data.tenantId)
    setUser(data)
  }

  const logout = () => {
    localStorage.clear()
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)