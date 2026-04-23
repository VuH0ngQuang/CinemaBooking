import { buildApiUrl } from "./api"

export const loginWithEmailPassword = async ({ email, password }) => {
  const response = await fetch(buildApiUrl("/api/auth/login"), {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify({ email, password }),
  })

  const text = await response.text()

  if (!response.ok) {
    throw new Error(text || "Login failed")
  }
}

export const getCurrentUser = async () => {
  const response = await fetch(buildApiUrl("/api/auth/me"), {
    method: "GET",
    credentials: "include",
  })

  if (response.status === 401) return null

  const text = await response.text()

  if (!response.ok) {
    throw new Error(text || "Failed to fetch current user")
  }

  return JSON.parse(text)
}

export const getUserByEmail = async (email) => {
  const response = await fetch(
    buildApiUrl(`/api/users/email/${encodeURIComponent(email)}`),
    {
      method: "GET",
      credentials: "include",
    }
  )

  const text = await response.text()

  if (!response.ok) {
    throw new Error(text || "Failed to fetch user profile")
  }

  return JSON.parse(text)
}

export const registerUser = async ({ email, password, full_name }) => {
  const response = await fetch(buildApiUrl("/api/auth/register"), {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify({ email, password, full_name }),
  })

  const text = await response.text()

  if (!response.ok) {
    throw new Error(text || "Registration failed")
  }

  return text
}

export const logoutRequest = async () => {
  const response = await fetch(buildApiUrl("/api/auth/logout"), {
    method: "POST",
    credentials: "include",
  })

  const text = await response.text()

  if (!response.ok) {
    throw new Error(text || "Logout failed")
  }

  return text
}
