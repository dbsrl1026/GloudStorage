import axios from "axios";
import { useAuthStore } from "../stores/authStore";

const axiosInstance = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL,
    withCredentials: true,
});

function getCookie(name: string): string | null {
    const match = document.cookie.match(new RegExp("(^| )" + name + "=([^;]+)"));
    return match ? decodeURIComponent(match[2]) : null;
}


axiosInstance.interceptors.request.use(
    (config) => {
        const csrfToken = getCookie("csrfToken");
        if (csrfToken) {
            config.headers["X-CSRF-Token"] = csrfToken;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

// 자동 토큰 재발급 인터셉터
axiosInstance.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;

        // 무한 루프 방지: /auth/refresh 자체에서 401이 또 오면 종료
        if (originalRequest.url?.includes("/auth/refresh")) {
            useAuthStore.getState().logout();
            useAuthStore.getState().openLoginModal();
            return Promise.reject(error);
        }

        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;
            try {
                await axiosInstance.post("/auth/refresh", null, { withCredentials: true });
                return axiosInstance(originalRequest); // 원 요청 재시도
            } catch (refreshError) {
                useAuthStore.getState().logout();
                useAuthStore.getState().openLoginModal();
                return Promise.reject(refreshError);
            }
        }

        return Promise.reject(error);
    }
);
export default axiosInstance;