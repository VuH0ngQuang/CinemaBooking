import axios from "axios"

const axiosClient = axios.create({
  baseURL: "http://localhost:1325",
  headers: {
    "Content-Type": "application/json",
  },
})

axiosClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token")

    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    return config
  },
  (error) => Promise.reject(error)
)

axiosClient.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const message =
      error?.response?.data?.message ||
      error?.response?.data ||
      error?.message ||
      "Request failed"

    return Promise.reject(new Error(message))
  }
)

export default axiosClient