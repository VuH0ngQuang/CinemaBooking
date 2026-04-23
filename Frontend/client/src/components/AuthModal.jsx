import { useState } from "react"
import { XIcon } from "lucide-react"
import { useAuth } from "../context/AuthContext"
import { registerUser } from "../lib/authApi"

const AuthModal = ({ isOpen, onClose }) => {
  const [mode, setMode] = useState("login")
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [fullName, setFullName] = useState("")
  const [error, setError] = useState("")
  const [successMessage, setSuccessMessage] = useState("")
  const [isSubmitting, setIsSubmitting] = useState(false)

  const { login } = useAuth()

  if (!isOpen) return null

  const resetForm = () => {
    setEmail("")
    setPassword("")
    setFullName("")
    setError("")
    setSuccessMessage("")
  }

  const closeModal = () => {
    resetForm()
    onClose()
  }

  const handleLogin = async (e) => {
    e.preventDefault()
    setError("")
    setSuccessMessage("")

    if (!email || !password) {
      setError("Please enter both email and password.")
      return
    }

    try {
      setIsSubmitting(true)
      await login(email, password)
      closeModal()
    } catch (err) {
      setError(err.message || "Login failed.")
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleSignup = async (e) => {
    e.preventDefault()
    setError("")
    setSuccessMessage("")

    if (!fullName || !email || !password) {
      setError("Please fill in all fields.")
      return
    }

    try {
      setIsSubmitting(true)
      await registerUser({ email, password, full_name: fullName })
      setSuccessMessage("Account created successfully. Please log in.")
      setMode("login")
      setPassword("")
    } catch (err) {
      setError(err.message || "Signup failed.")
    } finally {
      setIsSubmitting(false)
    }
  }

  const inputClass =
    "w-full mb-3 p-2 border border-gray-300 rounded " +
    "bg-white text-black placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-primary"

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60">
      <div className="bg-white rounded-lg w-96 p-6 relative">
        <XIcon
          className="absolute top-4 right-4 cursor-pointer text-black"
          onClick={closeModal}
        />

        {mode === "login" ? (
          <>
            <h2 className="text-2xl font-bold mb-4 text-black">Login</h2>

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

              {error && <p className="mb-3 text-sm text-red-600">{error}</p>}
              {successMessage && (
                <p className="mb-3 text-sm text-green-600">{successMessage}</p>
              )}

              <button
                type="submit"
                disabled={isSubmitting}
                className="w-full bg-primary text-white py-2 rounded cursor-pointer disabled:opacity-60"
              >
                {isSubmitting ? "Logging in..." : "Login"}
              </button>
            </form>

            <p className="text-sm mt-4 text-center text-black">
              Haven't had an account?{" "}
              <span
                className="text-primary cursor-pointer font-medium"
                onClick={() => {
                  setMode("signup")
                  setError("")
                  setSuccessMessage("")
                }}
              >
                Sign up
              </span>
            </p>
          </>
        ) : (
          <>
            <h2 className="text-2xl font-bold mb-4 text-black">Sign Up</h2>

            <form onSubmit={handleSignup}>
              <input
                type="text"
                placeholder="Name"
                className={inputClass}
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                required
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

              {error && <p className="mb-3 text-sm text-red-600">{error}</p>}
              {successMessage && (
                <p className="mb-3 text-sm text-green-600">{successMessage}</p>
              )}

              <button
                type="submit"
                disabled={isSubmitting}
                className="w-full bg-primary text-white py-2 rounded cursor-pointer disabled:opacity-60"
              >
                {isSubmitting ? "Creating account..." : "Create account"}
              </button>
            </form>

            <p className="text-sm mt-4 text-center text-black">
              Already have an account?{" "}
              <span
                className="text-primary cursor-pointer font-medium"
                onClick={() => {
                  setMode("login")
                  setError("")
                  setSuccessMessage("")
                }}
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
