import { apiClient } from './index';

export interface LoginRequest {
    login: string;
    password: string;
}

export interface RefreshTokenRequest {
    token: string;
}

export interface AuthResponse {
    token: string;
    userId: string;
    login: string;
    roles: string[];
    permissions: string[];
    expiresAt: number;
}

export const authService = {
    login: async (credentials: LoginRequest) => {
        return await apiClient.post<AuthResponse>('/auth/login', credentials);
    },

    logout: async () => {
        return await apiClient.post('/auth/logout');
    },

    logoutAll: async () => {
        return await apiClient.post('/auth/logout-all');
    },

    logoutOthers: async () => {
        return await apiClient.post('/auth/logout-others');
    },

    refreshToken: async (request: RefreshTokenRequest) => {
        return await apiClient.post<AuthResponse>('/auth/refresh', request);
    }
};