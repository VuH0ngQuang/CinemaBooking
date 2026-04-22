import { buildApiUrl } from "./api"

export const loginWithEmailPassword = async ({ email, password }) => {
  const response = await fetch(buildApiUrl("/api/auth/login"), {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ email, password }),
  })

  const responseText = await response.text()

  if (!response.ok) {
    throw new Error(responseText || "Login failed")
  }

  return responseText
}

export const getUserByEmail = async (email, token) => {
  const response = await fetch(
    buildApiUrl(`/api/users/email/${encodeURIComponent(email)}`),
    {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    }
  )

  const responseText = await response.text()

  if (!response.ok) {
    throw new Error(responseText || "Failed to fetch user profile")
  }

  return JSON.parse(responseText)
}

/* ================= NEW ================= */

export const registerUser = async ({ email, password, full_name }) => {
  const response = await fetch(buildApiUrl("/api/auth/register"), {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      email,
      password,
      full_name,
    }),
  })

  const responseText = await response.text()

  if (!response.ok) {
    throw new Error(responseText || "Registration failed")
  }

  return responseText
}

export const logoutRequest = async (token) => {
  const response = await fetch(buildApiUrl("/api/auth/logout"), {
    method: "POST",
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })

  const responseText = await response.text()

  if (!response.ok) {
    throw new Error(responseText || "Logout failed")
  }

  return responseText
}