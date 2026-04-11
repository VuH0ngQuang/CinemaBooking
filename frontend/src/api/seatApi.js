import axiosClient from "./axiosClient"

export const seatApi = {
  getAll: () => axiosClient.get("/api/seats"),

  getById: (id) => axiosClient.get(`/api/seats/${id}`),

  getByRoom: (roomId) =>
    axiosClient.get(`/api/seats/room/${roomId}`),

  update: (id, data) => axiosClient.put(`/api/seats/${id}`, data),

  delete: (id) => axiosClient.delete(`/api/seats/${id}`),
}