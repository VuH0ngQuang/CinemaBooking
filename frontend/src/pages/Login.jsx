import { useState } from "react"
import { useAuth } from "../context/AuthContext"
import { useNavigate } from "react-router-dom"
import { authApi } from "../api/authApi"

const Login = () => {
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState("")

  const { login } = useAuth()
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError("")
    setLoading(true)

    try {
      const token = await authApi.login({
        email,
        password,
      })

      // Backend hiện trả về plain token string, không trả user object.
      // Vì AuthContext hiện tại cần cả user + token,
      // nên ở bước này chỉ lưu object tối thiểu, không tự bịa thêm field.
      const minimalUser = {
        email,
      }

      login(minimalUser, token)
      navigate("/")
    } catch (err) {
      setError(err.message || "Login failed")
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center">
      <form onSubmit={handleSubmit} className="bg-white p-8 rounded-lg w-96">
        <h2 className="text-2xl font-bold mb-6">Login</h2>

        {error && (
          <p className="mb-4 text-red-500 text-sm">
            {error}
          </p>
        )}

        <input
          type="email"
          placeholder="Email"
          className="w-full mb-4 p-2 border"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />

        <input
          type="password"
          placeholder="Password"
          className="w-full mb-4 p-2 border"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />

        <button
          type="submit"
          disabled={loading}
          className="w-full bg-primary text-white py-2 rounded disabled:opacity-60"
        >
          {loading ? "Logging in..." : "Login"}
        </button>
      </form>
    </div>
  )
}

export default Login