const baseUrl = import.meta.env.VITE_BASE_URL?.replace(/\/$/, "")

export const API_BASE_URL = baseUrl || ""

export const buildApiUrl = (path) => {
  const normalizedPath = path.startsWith("/") ? path : `/${path}`
  return `${API_BASE_URL}${normalizedPath}`
}