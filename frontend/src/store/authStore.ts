import { create } from 'zustand';
import { authService } from '../services/api';

interface User {
    id: string;
    login: string;
    roles: string[];
    permissions: string[];
}

interface AuthState {
    isAuthenticated: boolean;
    user: User | null;
    token: string | null;
    login: (username: string, password: string) => Promise<boolean>;
    logout: () => Promise<void>;
    logoutAll: () => Promise<void>;
    logoutOthers: () => Promise<void>;
    refreshToken: () => Promise<boolean>;
}

// Fonction pour récupérer l'utilisateur du localStorage
const getStoredUser = (): User | null => {
    const storedUser = localStorage.getItem('user');
    if (storedUser) {
        try {
            return JSON.parse(storedUser);
        } catch (error) {
            console.error('Erreur lors de la récupération de l\'utilisateur depuis localStorage:', error);
            return null;
        }
    }
    return null;
};

// Fonction pour récupérer le token du localStorage
const getStoredToken = (): string | null => {
    return localStorage.getItem('token');
};

export const useAuthStore = create<AuthState>((set, get) => ({
    isAuthenticated: !!getStoredToken(),
    user: getStoredUser(),
    token: getStoredToken(),

    login: async (username: string, password: string) => {
        try {
            const response = await authService.login({ login: username, password });
            const { token, userId, login, roles, permissions } = response.data;

            // Sauvegarder les données d'authentification
            localStorage.setItem('token', token);
            const user = { id: userId, login, roles, permissions };
            localStorage.setItem('user', JSON.stringify(user));

            set({ isAuthenticated: true, user, token });
            return true;
        } catch (error) {
            console.error('Erreur de connexion:', error);
            return false;
        }
    },

    logout: async () => {
        try {
            await authService.logout();
        } catch (error) {
            console.error('Erreur lors de la déconnexion:', error);
        } finally {
            // Même en cas d'erreur du serveur, on nettoie le localStorage
            localStorage.removeItem('token');
            localStorage.removeItem('user');
            set({ isAuthenticated: false, user: null, token: null });
        }
    },

    logoutAll: async () => {
        try {
            await authService.logoutAll();
            localStorage.removeItem('token');
            localStorage.removeItem('user');
            set({ isAuthenticated: false, user: null, token: null });
        } catch (error) {
            console.error('Erreur lors de la déconnexion de toutes les sessions:', error);
        }
    },

    logoutOthers: async () => {
        try {
            await authService.logoutOthers();
        } catch (error) {
            console.error('Erreur lors de la déconnexion des autres sessions:', error);
        }
    },

    refreshToken: async () => {
        const currentToken = get().token;
        if (!currentToken) return false;

        try {
            const response = await authService.refreshToken({ token: currentToken });
            const { token } = response.data;

            localStorage.setItem('token', token);
            set({ token });
            return true;
        } catch (error) {
            console.error('Erreur lors du rafraîchissement du token:', error);
            localStorage.removeItem('token');
            localStorage.removeItem('user');
            set({ isAuthenticated: false, user: null, token: null });
            return false;
        }
    }
}));