import { useState } from "react"
import { XIcon } from "lucide-react"
import { useAuth } from "../context/AuthContext"

const AuthModal = ({ isOpen, onClose }) => {
  const [mode, setMode] = useState("login")
  const { login } = useAuth()

  if (!isOpen) return null

  const handleLogin = (e) => {
    e.preventDefault()
    login(
      { id: 1, name: "Quan", email: "quan@gmail.com" },
      "fake-jwt-token"
    )
    onClose()
  }

  const handleSignup = (e) => {
    e.preventDefault()
    login(
      { id: 2, name: "New User", email: "new@gmail.com" },
      "fake-jwt-token"
    )
    onClose()
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

            <form onSubmit={handleLogin}>
              <input
                type="email"
                placeholder="Email"
                className={inputClass}
              />
              <input
                type="password"
                placeholder="Password"
                className={inputClass}
              />

              <button className="w-full bg-primary text-white py-2 rounded cursor-pointer">
                Login
              </button>
            </form>

            <p className="text-sm mt-4 text-center text-black">
              Haven't had an account?{" "}
              <span
                className="text-primary cursor-pointer font-medium"
                onClick={() => setMode("signup")}
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
              />
              <input
                type="email"
                placeholder="Email"
                className={inputClass}
              />
              <input
                type="password"
                placeholder="Password"
                className={inputClass}
              />

              <button className="w-full bg-primary text-white py-2 rounded cursor-pointer">
                Create account
              </button>
            </form>

            <p className="text-sm mt-4 text-center text-black">
              Already have an account?{" "}
              <span
                className="text-primary cursor-pointer font-medium"
                onClick={() => setMode("login")}
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
