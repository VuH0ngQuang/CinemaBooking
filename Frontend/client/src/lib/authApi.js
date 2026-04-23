import { buildApiUrl } from "./api"

const getErrorMessage = (text, fallbackMessage) => {
  if (text === "Failed to fetch") {
    return "Cannot connect to server. Please make sure backend is running and try again."
  }

  if (!text) return fallbackMessage

  try {
    const parsed = JSON.parse(text)
    if (parsed?.message === "User email already exists") {
      return "This email is already registered. Please use another email."
    }
    return parsed?.message || fallbackMessage
  } catch {
    return text
  }
}

const fetchWithNetworkHandling = async (...args) => {
  try {
    return await fetch(...args)
  } catch {
    throw new Error("Cannot connect to server. Please make sure backend is running and try again.")
  }
}

export const loginWithEmailPassword = async ({ email, password }) => {
  const response = await fetchWithNetworkHandling(buildApiUrl("/api/auth/login"), {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify({ email, password }),
  })

  const text = await response.text()

  if (!response.ok) {
    throw new Error(getErrorMessage(text, "Login failed"))
  }
}

export const getCurrentUser = async () => {
  const response = await fetchWithNetworkHandling(buildApiUrl("/api/auth/me"), {
    method: "GET",
    credentials: "include",
  })

  if (response.status === 401) return null

  const text = await response.text()

  if (!response.ok) {
    throw new Error(getErrorMessage(text, "Failed to fetch current user"))
  }

  return JSON.parse(text)
}

export const getUserByEmail = async (email) => {
  const response = await fetchWithNetworkHandling(
    buildApiUrl(`/api/users/email/${encodeURIComponent(email)}`),
    {
      method: "GET",
      credentials: "include",
    }
  )

  const text = await response.text()

  if (!response.ok) {
    throw new Error(getErrorMessage(text, "Failed to fetch user profile"))
  }

  return JSON.parse(text)
}

export const registerUser = async ({ email, password, full_name }) => {
  const response = await fetchWithNetworkHandling(buildApiUrl("/api/auth/register"), {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify({ email, password, full_name }),
  })

  const text = await response.text()

  if (!response.ok) {
    throw new Error(getErrorMessage(text, "Registration failed"))
  }

  return text
}

export const logoutRequest = async () => {
  const response = await fetchWithNetworkHandling(buildApiUrl("/api/auth/logout"), {
    method: "POST",
    credentials: "include",
  })

  const text = await response.text()

  if (!response.ok) {
    throw new Error(getErrorMessage(text, "Logout failed"))
  }

  return text
}
