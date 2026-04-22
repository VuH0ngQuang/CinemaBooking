import { createContext, useContext, useEffect, useState } from "react"

const AuthContext = createContext()

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null)
  const [token, setToken] = useState(null)
  const [authModalOpen, setAuthModalOpen] = useState(false)
  const baseUrl = import.meta.env.VITE_BASE_URL

  const openAuthModal = () => setAuthModalOpen(true)
  const closeAuthModal = () => setAuthModalOpen(false)

  useEffect(() => {
    const cachedUser = localStorage.getItem("user")
    if (!cachedUser) {
      return
    }

    try {
      setUser(JSON.parse(cachedUser))
    } catch {
      localStorage.removeItem("user")
    }
  }, [])

  useEffect(() => {
    if (!baseUrl) {
      return
    }

    const bootstrapAuth = async () => {
      try {
        const response = await fetch(`${baseUrl.replace(/\/$/, '')}/api/auth/me`, {
          credentials: "include",
        })

        if (!response.ok) {
          setUser(null)
          setToken(null)
          return
        }

        const userData = await response.json()
        setUser(userData)
        setToken("cookie-authenticated")
        localStorage.setItem("user", JSON.stringify(userData))
      } catch {
        setUser(null)
        setToken(null)
        localStorage.removeItem("user")
      }
    }

    bootstrapAuth()
  }, [baseUrl])

  const login = (userData, accessToken) => {
    setUser(userData)
    setToken(accessToken)
    if (userData) {
      localStorage.setItem("user", JSON.stringify(userData))
    }
  }

  const logout = () => {
    setUser(null)
    setToken(null)
    localStorage.removeItem("user")
  }

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
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
