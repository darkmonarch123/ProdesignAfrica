import axios from 'axios';
import { useAuthStore } from '../store/authStore';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
});

apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let refreshInFlight: Promise<string | null> | null = null;

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      const refreshToken = useAuthStore.getState().refreshToken;
      if (!refreshToken) {
        useAuthStore.getState().logout();
        return Promise.reject(error);
      }
      if (!refreshInFlight) {
        refreshInFlight = axios
          .post(`${API_BASE_URL}/auth/refresh`, { refreshToken })
          .then((res) => {
            const { accessToken, refreshToken: newRefresh } = res.data;
            useAuthStore.getState().setTokens(accessToken, newRefresh);
            return accessToken as string;
          })
          .catch(() => {
            useAuthStore.getState().logout();
            return null;
          })
          .finally(() => {
            refreshInFlight = null;
          });
      }
      const newAccessToken = await refreshInFlight;
      if (newAccessToken) {
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        return apiClient(originalRequest);
      }
    }
    return Promise.reject(error);
  }
);
