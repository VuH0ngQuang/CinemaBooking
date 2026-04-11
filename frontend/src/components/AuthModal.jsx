import { useState } from "react"
import { XIcon } from "lucide-react"
import { useAuth } from "../context/AuthContext"
import { authApi } from "../api/authApi"

const AuthModal = ({ isOpen, onClose }) => {
  const [mode, setMode] = useState("login")
  const [fullName, setFullName] = useState("")
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState("")
  const [success, setSuccess] = useState("")

  const { login } = useAuth()

  if (!isOpen) return null

  const resetMessages = () => {
    setError("")
    setSuccess("")
  }

  const handleLogin = async (e) => {
    e.preventDefault()
    resetMessages()
    setLoading(true)

    try {
      const token = await authApi.login({
        email,
        password,
      })

      const minimalUser = { email }
      login(minimalUser, token)
      onClose()
    } catch (err) {
      setError(err.message || "Login failed")
    } finally {
      setLoading(false)
    }
  }

  const handleSignup = async (e) => {
    e.preventDefault()
    resetMessages()
    setLoading(true)

    try {
      await authApi.register({
        email,
        password,
        full_name: fullName,
      })

      setSuccess("Register successfully. Please login.")
      setMode("login")
      setPassword("")
    } catch (err) {
      setError(err.message || "Register failed")
    } finally {
      setLoading(false)
    }
  }

  const switchMode = (nextMode) => {
    setMode(nextMode)
    setError("")
    setSuccess("")
    setPassword("")
  }

  const inputClass =
    "w-full mb-3 p-2 border border-gray-300 rounded " +
    "bg-white text-black placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-primary"

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60">
      <div className="bg-white rounded-lg w-96 p-6 relative">
        <XIcon
          className="absolute top-4 right-4 cursor-pointer text-black"
          onClick={onClose}
        />

        {mode === "login" ? (
          <>
            <h2 className="text-2xl font-bold mb-4 text-black">Login</h2>

            {error && <p className="mb-3 text-sm text-red-500">{error}</p>}
            {success && <p className="mb-3 text-sm text-green-600">{success}</p>}

            <form onSubmit={handleLogin}>
              <input
                type="email"
                placeholder="Email"
                className={inputClass}
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
              <input
                type="password"
                placeholder="Password"
                className={inputClass}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />

              <button
                type="submit"
                disabled={loading}
                className="w-full bg-primary text-white py-2 rounded cursor-pointer disabled:opacity-60"
              >
                {loading ? "Logging in..." : "Login"}
              </button>
            </form>

            <p className="text-sm mt-4 text-center text-black">
              Haven't had an account?{" "}
              <span
                className="text-primary cursor-pointer font-medium"
                onClick={() => switchMode("signup")}
              >
                Sign up
              </span>
            </p>
          </>
        ) : (
          <>
            <h2 className="text-2xl font-bold mb-4 text-black">Sign Up</h2>

            {error && <p className="mb-3 text-sm text-red-500">{error}</p>}
            {success && <p className="mb-3 text-sm text-green-600">{success}</p>}

            <form onSubmit={handleSignup}>
              <input
                type="text"
                placeholder="Full name"
                className={inputClass}
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
              />
              <input
                type="email"
                placeholder="Email"
                className={inputClass}
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
              <input
                type="password"
                placeholder="Password"
                className={inputClass}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />

              <button
                type="submit"
                disabled={loading}
                className="w-full bg-primary text-white py-2 rounded cursor-pointer disabled:opacity-60"
              >
                {loading ? "Creating account..." : "Create account"}
              </button>
            </form>

            <p className="text-sm mt-4 text-center text-black">
              Already have an account?{" "}
              <span
                className="text-primary cursor-pointer font-medium"
                onClick={() => switchMode("login")}
              >
                Login
              </span>
            </p>
          </>
        )}
      </div>
    </div>
  )
}

export default AuthModal