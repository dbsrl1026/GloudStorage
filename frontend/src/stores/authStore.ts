// src/stores/authStore.ts
import { create } from 'zustand';

interface MemberDTO {
  email: string;
  username: string;
}

interface AuthStore {
  isLoggedIn: boolean;
  user: MemberDTO | null;
  email: string | null;
  showLoginModal: boolean;
  openLoginModal: () => void;
  closeLoginModal: () => void;
  login: (user: MemberDTO) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthStore>((set) => ({
  isLoggedIn: localStorage.getItem('isLoggedIn') === 'true',
  user: localStorage.getItem('email') && localStorage.getItem('username')
    ? { email: localStorage.getItem('email')!, username: localStorage.getItem('username')! }
    : null,
  email: localStorage.getItem('email'),
  showLoginModal: false,
  openLoginModal: () => set({ showLoginModal: true }),
  closeLoginModal: () => set({ showLoginModal: false }),
  login: (user) => {
    set({ isLoggedIn: true, user, email: user.email, showLoginModal: false });
    localStorage.setItem('email', user.email);
    localStorage.setItem('username', user.username);
    localStorage.setItem('isLoggedIn', 'true');
  },
  logout: () => {
    set({ isLoggedIn: false, user: null, email: null });
    localStorage.removeItem('email');
    localStorage.removeItem('username');
    localStorage.removeItem('isLoggedIn');
  },
}));