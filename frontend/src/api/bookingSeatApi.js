import axiosClient from "./axiosClient"

export const bookingSeatApi = {
  getAll: () => axiosClient.get("/api/booking-seats"),

  getById: (id) =>
    axiosClient.get(`/api/booking-seats/${id}`),

  create: (data) =>
    axiosClient.post("/api/booking-seats", data),

  update: (id, data) =>
    axiosClient.put(`/api/booking-seats/${id}`, data),

  delete: (id) =>
    axiosClient.delete(`/api/booking-seats/${id}`),
}