import axiosClient from "./axiosClient"

export const ticketApi = {
  getAll: () => axiosClient.get("/api/tickets"),

  getById: (id) => axiosClient.get(`/api/tickets/${id}`),

  getByCode: (code) =>
    axiosClient.get(`/api/tickets/code/${code}`),

  generate: (paymentId) =>
    axiosClient.post(`/api/tickets/generate/payment/${paymentId}`),

  validate: (data) =>
    axiosClient.post("/api/tickets/validate", data),
}