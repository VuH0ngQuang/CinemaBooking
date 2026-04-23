import { createContext, useContext, useEffect, useState } from "react"
import { getCurrentUser, loginWithEmailPassword, logoutRequest } from "../lib/authApi"

const AuthContext = createContext()

const normalizeUser = (data) => {
  if (!data) return null
  return {
    id: data.user_id,
    name: data.full_name || data.email,
    email: data.email,
    role: data.role,
    status: data.status,
    full_name: data.full_name,
    user_id: data.user_id,
  }
}

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null)
  const [authModalOpen, setAuthModalOpen] = useState(false)

  const openAuthModal = () => setAuthModalOpen(true)
  const closeAuthModal = () => setAuthModalOpen(false)

  useEffect(() => {
    getCurrentUser()
      .then((data) => setUser(normalizeUser(data)))
      .catch(() => setUser(null))
  }, [])

  const login = async (email, password) => {
    await loginWithEmailPassword({ email, password })
    const data = await getCurrentUser()
    setUser(normalizeUser(data))
  }

  const logout = async () => {
    try {
      await logoutRequest()
    } catch {
      // ignore
    }
    setUser(null)
  }

  return (
    <AuthContext.Provider
      value={{
        user,
        login,
        logout,
        authModalOpen,
        openAuthModal,
        closeAuthModal,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
