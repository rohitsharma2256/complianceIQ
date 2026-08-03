import axios from 'axios'

const api = axios.create({ baseURL: '' })

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// Public routes - inpe redirect nahi karna (warna loop ban jaayega)
const PUBLIC_PATHS = ['/login', '/register', '/forgot-password', '/reset-password']

api.interceptors.response.use(
  (res) => res,
  (err) => {
    const status = err.response?.status
    const onPublicPage = PUBLIC_PATHS.includes(window.location.pathname)

    // 401 = token nahi/expired | 403 = token invalid ya permission nahi
    // 403 pe sirf tab logout karo jab token hai hi nahi ya expire ho gaya -
    // warna "admin only" wale 403 pe bhi user logout ho jaayega
    if (status === 401 && !onPublicPage) {
      localStorage.clear()
      window.location.href = '/login?expired=1'
    }
    return Promise.reject(err)
  }
)

export default api