import { useAuthStore } from "../../stores/authStore";
import axios from "../../services/axios";
import typo from '/src/assets/gs_typo.png';
import profile_default from '/src/assets/profile_sample.png';

const Header = ({ searchQuery, setSearchQuery }: { searchQuery: string; setSearchQuery: (q: string) => void }) => {
    const { user, isLoggedIn, logout } = useAuthStore();

    const handleLogout = async () => {
        try {
            await axios.post('/auth/logout', null, { withCredentials: true });
            logout();
            window.location.reload();
        } catch (err) {
            console.error("로그아웃 실패:", err);
            alert("로그아웃에 실패했습니다.");
        }
    };

    return (
        <header className="bg-white border-b h-16 flex items-center px-4">
            <div className="mr-16">
                <img src={typo} alt="Gloud Storage 로고" className="h-10" />
            </div>
            <div>
                <input
                    type="text"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    placeholder="파일명 검색..."
                    className="border rounded px-3 py-2 text-sm w-80"
                />
            </div>
            {isLoggedIn && (
                <div className="ml-auto flex items-center gap-4">
                    <span className="text-sm font-medium">{user?.username}</span>
                    <img
                        src={profile_default}
                        alt="프로필"
                        className="w-9 h-9 border-b-black rounded-full"
                    />
                    <button
                        onClick={handleLogout}
                        className="px-3 py-1 border rounded text-sm hover:bg-gray-100"
                    >
                        logout
                    </button>
                </div>
            )}
        </header>
    );
};

export default Header;