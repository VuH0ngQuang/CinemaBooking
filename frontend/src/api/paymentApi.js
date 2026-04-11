import axiosClient from "./axiosClient"

export const paymentApi = {
  getAll: () => axiosClient.get("/api/payments"),

  getById: (id) => axiosClient.get(`/api/payments/${id}`),

  create: (data) => axiosClient.post("/api/payments", data),

  update: (id, data) => axiosClient.put(`/api/payments/${id}`, data),

  delete: (id) => axiosClient.delete(`/api/payments/${id}`),

  markSuccess: (id) =>
    axiosClient.post(`/api/payments/${id}/mark-success`),

  markSuccessByRef: (ref) =>
    axiosClient.post(`/api/payments/ref/${ref}/mark-success`),
}