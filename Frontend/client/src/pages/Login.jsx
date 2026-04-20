import { useState } from "react"
import { useAuth } from "../context/AuthContext"
import { useNavigate } from "react-router-dom"
import toast from "react-hot-toast"

const Login = () => {
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const { login } = useAuth()
  const navigate = useNavigate()
  const baseUrl = import.meta.env.VITE_BASE_URL

  const handleSubmit = async (e) => {
    e.preventDefault()

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

      const token = await response.text()
      login({ email }, token ? "cookie-authenticated" : null)
      toast.success("Login successful")
      navigate("/")
    } catch (error) {
      toast.error(error.message || "Login failed")
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

        <button className="w-full bg-primary text-white py-2 rounded">
          Login
        </button>
      </form>
    </div>
  )
}

export default Login
