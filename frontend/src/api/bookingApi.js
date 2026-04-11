import axiosClient from "./axiosClient"

export const bookingApi = {
  getAll: () => axiosClient.get("/api/bookings"),

  getById: (id) => axiosClient.get(`/api/bookings/${id}`),

  create: (data) => axiosClient.post("/api/bookings", data),

  createFull: (data) => axiosClient.post("/api/bookings/full", data),

  update: (id, data) => axiosClient.put(`/api/bookings/${id}`, data),

  delete: (id) => axiosClient.delete(`/api/bookings/${id}`),
}