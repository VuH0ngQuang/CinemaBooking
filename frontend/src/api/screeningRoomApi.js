import axiosClient from "./axiosClient"

export const screeningRoomApi = {
  getAll: () => axiosClient.get("/api/screeningrooms"),

  getById: (id) =>
    axiosClient.get(`/api/screeningrooms/${id}`),

  create: (data) =>
    axiosClient.post("/api/screeningrooms", data),

  update: (id, data) =>
    axiosClient.put(`/api/screeningrooms/${id}`, data),

  delete: (id) =>
    axiosClient.delete(`/api/screeningrooms/${id}`),
}