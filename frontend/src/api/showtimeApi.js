import axiosClient from "./axiosClient"

export const showtimeApi = {
  getAll: () => axiosClient.get("/api/showtimes"),

  getById: (id) => axiosClient.get(`/api/showtimes/${id}`),

  create: (data) => axiosClient.post("/api/showtimes", data),

  update: (id, data) => axiosClient.put(`/api/showtimes/${id}`, data),

  delete: (id) => axiosClient.delete(`/api/showtimes/${id}`),
}