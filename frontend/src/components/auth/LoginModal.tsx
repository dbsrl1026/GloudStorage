import { useAuthStore } from "../../stores/authStore";
import { FcGoogle } from "react-icons/fc";
import { RiKakaoTalkFill } from "react-icons/ri";
import { SiNaver } from "react-icons/si";
import { useEffect, useState } from "react";
import axios from "../../services/axios";


const LoginModal = () => {
    const { showLoginModal } = useAuthStore();
    // const { showLoginModal, closeLoginModal } = useAuthStore();

    // Always call hooks unconditionally, before any returns
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [isRegisterMode, setIsRegisterMode] = useState(false);
    const [username, setUsername] = useState("");

    // One-time effect to catch OAuth2 flow errors via URL query (?error)
    // and global event to catch unexpected redirect failures (e.g. 503)
    useEffect(() => {
        // Check for OAuth2 error in query string
        if (typeof window !== "undefined") {
            const params = new URLSearchParams(window.location.search);
            if (params.has("error")) {
                alert("로그인 중 문제가 발생했습니다. 다시 시도해 주세요.");
                // Optionally, remove error param from URL
                params.delete("error");
                const newSearch = params.toString();
                const newUrl = window.location.pathname + (newSearch ? "?" + newSearch : "");
                window.history.replaceState({}, "", newUrl);
            }
        }
        // One-time event listener for unexpected redirect/page error
        const handlePageError = () => {
            // Most errors will be handled by browser, but this is a fallback for 503/unexpected
            alert("로그인 중 문제가 발생했습니다. 다시 시도해 주세요.");
        };
        window.addEventListener("redirecterror", handlePageError, { once: true });
        return () => {
            window.removeEventListener("redirecterror", handlePageError);
        };
    }, []);

    if (!showLoginModal) return null;

    const handleEmailLogin = async () => {
        try {
            await axios.post("/auth/login", { email, password });
            window.location.reload();
        } catch (err) {
            console.error("일반 로그인 실패:", err);
            alert("이메일 또는 비밀번호가 잘못되었습니다. 다시 시도해 주세요.");
        }
    };

    const handleRegister = async () => {
        if (password !== confirmPassword) {
            alert("비밀번호가 일치하지 않습니다.");
            return;
        }
        try {
            await axios.post("/auth/register", { email, password, username });
            alert("회원가입이 완료되었습니다. 로그인 해 주세요.");
            setIsRegisterMode(false);
        } catch (err) {
            console.error("회원가입 실패:", err);
            alert("회원가입에 실패했습니다. 입력값을 확인해 주세요.");
        }
    };

    const oauthUrls = {
        google: import.meta.env.VITE_API_BASE_URL + "/oauth2/authorization/google",
        naver: import.meta.env.VITE_API_BASE_URL + "/oauth2/authorization/naver",
        kakao: import.meta.env.VITE_API_BASE_URL + "/oauth2/authorization/kakao",
    };

    const openOauthRedirect = (provider: keyof typeof oauthUrls) => {
        try {
            window.location.href = oauthUrls[provider];
        } catch (err) {
            console.error("OAuth 리다이렉트 실패:", err);
            alert("로그인 중 문제가 발생했습니다. 다시 시도해 주세요.");
        }
        // Optional safety: detect unexpected redirect failures via visibility or polling
        // For full error catching, would require backend redirect fallback or global error route
    };

    return (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
            <div className="bg-white rounded-xl p-8 w-[400px] shadow-lg relative">
                {/*<button*/}
                {/*    onClick={closeLoginModal}*/}
                {/*    className="absolute right-4 top-4 text-gray-400 hover:text-gray-600 text-xl"*/}
                {/*>*/}
                {/*    &times;*/}
                {/*</button>*/}

                <h2 className="text-2xl font-bold mb-6 text-center">
                    {isRegisterMode ? "회원가입" : "시작하기"}
                </h2>

                <div className="flex justify-center gap-5 mb-6">
                    <button
                        onClick={() => openOauthRedirect("google")}
                        className="bg-white w-12 h-12 rounded-full flex items-center justify-center text-gray-700 border text-2xl shadow"
                    >
                        <FcGoogle />
                    </button>
                    <button
                        onClick={() => openOauthRedirect("naver")}
                        className="bg-green-500 w-12 h-12 rounded-full flex items-center justify-center text-white  shadow"
                    >
                        <SiNaver />
                    </button>
                    <button
                        onClick={() => openOauthRedirect("kakao")}
                        className="bg-yellow-300 w-12 h-12 rounded-full flex items-center justify-center text-2xl shadow"
                    >
                        <RiKakaoTalkFill />
                    </button>
                </div>

                <div className="space-y-3">
                    <input
                        type="email"
                        placeholder="이메일 주소를 입력해 주세요"
                        className="w-full border px-3 py-2 rounded text-sm"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        onKeyDown={(e) => {
                            if (e.key === "Enter") {
                                isRegisterMode ? handleRegister() : handleEmailLogin();
                            }
                        }}
                    />
                    <input
                        type="password"
                        placeholder="비밀번호를 입력해 주세요"
                        className="w-full border px-3 py-2 rounded text-sm"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        onKeyDown={(e) => {
                            if (e.key === "Enter") {
                                isRegisterMode ? handleRegister() : handleEmailLogin();
                            }
                        }}
                    />
                    {isRegisterMode && (
                        <>
                            <input
                                type="password"
                                placeholder="비밀번호 확인"
                                className="w-full border px-3 py-2 rounded text-sm"
                                value={confirmPassword}
                                onChange={(e) => setConfirmPassword(e.target.value)}
                                onKeyDown={(e) => {
                                    if (e.key === "Enter") handleRegister();
                                }}
                            />
                            <input
                                type="text"
                                placeholder="사용자 이름"
                                className="w-full border px-3 py-2 rounded text-sm"
                                value={username}
                                onChange={(e) => setUsername(e.target.value)}
                                onKeyDown={(e) => {
                                    if (e.key === "Enter") handleRegister();
                                }}
                            />
                        </>
                    )}

                    <button
                        onClick={isRegisterMode ? handleRegister : handleEmailLogin}
                        className="w-full bg-blue-600 hover:bg-blue-700 text-white py-2 rounded font-semibold"
                    >
                        {isRegisterMode ? "회원가입" : "로그인"}
                    </button>

                    {!isRegisterMode && (
                        <button
                            onClick={() => setIsRegisterMode(true)}
                            className="w-full border border-gray-300 py-2 rounded text-sm hover:bg-gray-100 transition"
                        >
                            이메일로 회원가입
                        </button>
                    )}
                </div>
            </div>
        </div>
    );
};

export default LoginModal;