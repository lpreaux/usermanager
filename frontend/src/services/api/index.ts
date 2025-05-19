import axios from 'axios';
import { authService } from './authService';
import { userService, type RegisterUserRequest, type UpdatePersonalInfoRequest, type AddPhoneNumberRequest, type AddEmailRequest, type ChangePasswordRequest } from './userService';
import { roleService, type RoleDTO, type CreateRoleRequest, type UpdateRoleRequest } from './roleService';

// Créer une instance d'Axios avec configuration de base
const apiClient = axios.create({
    baseURL: '/api/v1',
    headers: {
        'Content-Type': 'application/json',
    },
});

// Intercepteur pour ajouter le token d'authentification
apiClient.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// Intercepteur pour gérer les erreurs d'API
apiClient.interceptors.response.use(
    (response) => response,
    async (error) => {
        // Si erreur 401 (non authentifié) et token refresh disponible, tentative de refresh
        const originalRequest = error.config;
        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;
            try {
                const token = localStorage.getItem('token');
                if (token) {
                    const refreshResponse = await axios.post('/api/v1/auth/refresh', { token });
                    const newToken = refreshResponse.data.token;

                    localStorage.setItem('token', newToken);
                    originalRequest.headers['Authorization'] = `Bearer ${newToken}`;
                    return apiClient(originalRequest);
                }
            } catch (refreshError) {
                // En cas d'échec du refresh, déconnexion
                localStorage.removeItem('token');
                localStorage.removeItem('user');
                window.location.href = '/login';
            }
        }
        return Promise.reject(error);
    }
);

export {apiClient, authService, userService, roleService};
export type { RegisterUserRequest, UpdatePersonalInfoRequest, RoleDTO, CreateRoleRequest, UpdateRoleRequest, AddPhoneNumberRequest, AddEmailRequest, ChangePasswordRequest };
