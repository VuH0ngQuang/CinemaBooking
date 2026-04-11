import axiosClient from "./axiosClient"

export const movieApi = {
  getAll: () => axiosClient.get("/api/movies"),

  getById: (id) => axiosClient.get(`/api/movies/${id}`),

  create: (data) => axiosClient.post("/api/movies", data),

  update: (id, data) => axiosClient.put(`/api/movies/${id}`, data),

  delete: (id) => axiosClient.delete(`/api/movies/${id}`),
}