import axiosClient from "./axiosClient"

export const cinemaApi = {
  getAll: () => axiosClient.get("/api/cinemas"),

  getById: (id) => axiosClient.get(`/api/cinemas/${id}`),

  create: (data) => axiosClient.post("/api/cinemas", data),

  update: (id, data) => axiosClient.put(`/api/cinemas/${id}`, data),

  delete: (id) => axiosClient.delete(`/api/cinemas/${id}`),
}