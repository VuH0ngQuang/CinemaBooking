import { useState } from "react"
import { useAuth } from "../context/AuthContext"
import { useNavigate } from "react-router-dom"
import toast from "react-hot-toast"
import { getUserByEmail, loginWithEmailPassword } from "../lib/authApi"

const Login = () => {
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [error, setError] = useState("")
  const [isSubmitting, setIsSubmitting] = useState(false)

  const { login } = useAuth()
  const navigate = useNavigate()
  const baseUrl = import.meta.env.VITE_BASE_URL

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError("")

    if (!email || !password) {
      setError("Please enter both email and password.")
      return
    }

    try {
      setIsSubmitting(true)

    try {
      const response = await fetch(`${baseUrl.replace(/\/$/, '')}/api/auth/login`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        credentials: "include",
        body: JSON.stringify({
          email,
          password,
        }),
      })

      if (!response.ok) {
        const message = await response.text()
        throw new Error(message || "Login failed")
      }
      const token = await loginWithEmailPassword({ email, password })
      const userResponse = await getUserByEmail(email, token)

      const normalizedUser = {
        id: userResponse.user_id,
        name: userResponse.full_name || userResponse.email,
        email: userResponse.email,
        role: userResponse.role,
        status: userResponse.status,
        full_name: userResponse.full_name,
        user_id: userResponse.user_id,
      }

      const token = await response.text()
      login({ email }, token ? "cookie-authenticated" : null)
      toast.success("Login successful")
      navigate("/")
    } catch (error) {
      toast.error(error.message || "Login failed")
    }
      login(normalizedUser, token)
      navigate("/")
    } catch (err) {
      setError(err.message || "Login failed.")
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center">
      <form onSubmit={handleSubmit} className="bg-white p-8 rounded-lg w-96">
        <h2 className="text-2xl font-bold mb-6">Login</h2>

        <input
          type="email"
          placeholder="Email"
          className="w-full mb-4 p-2 border"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />

        <input
          type="password"
          placeholder="Password"
          className="w-full mb-4 p-2 border"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />

        {error && <p className="mb-4 text-sm text-red-600">{error}</p>}

        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full bg-primary text-white py-2 rounded disabled:opacity-60"
        >
          {isSubmitting ? "Logging in..." : "Login"}
        </button>
      </form>
    </div>
  )
}

export default Login