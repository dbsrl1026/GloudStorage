import { useEffect } from "react";
import { useRoutes } from "react-router-dom";
import routes from "./routes";
import { useAuthStore } from "./stores/authStore";
import LoginModal from "./components/auth/LoginModal";
import axios from "./services/axios"; // 반드시 withCredentials 포함되어 있어야 함

function App() {
    const content = useRoutes(routes);
    const { login, openLoginModal } = useAuthStore();

    useEffect(() => {
        axios.get("/auth/me", { withCredentials: true })
            .then((res) => {
                console.log(res);
                login(res.data);
            })
            .catch((err) => {
                if (err.response?.status === 401) {
                    // 정상적인 비로그인 상태 → 로그인 모달
                    openLoginModal();
                }
            });
    }, []);
    return (
        <>
            {content}
            <LoginModal />
        </>
    );
}

export default App;