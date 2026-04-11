import axiosClient from "./axiosClient"

export const authApi = {
  login: (data) => axiosClient.post("/api/auth/login", data),
  register: (data) => axiosClient.post("/api/auth/register", data),
  logout: () => axiosClient.post("/api/auth/logout"),
}