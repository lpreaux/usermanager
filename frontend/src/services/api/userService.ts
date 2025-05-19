import { apiClient } from './index';

export interface UserResponse {
    id: string;
    login: string;
    lastName: string;
    firstName: string;
    birthDate: string;
    age: number;
    isAdult: boolean;
    emails: string[];
    phoneNumbers: string[];
}

export interface RegisterUserRequest {
    login: string;
    password: string;
    lastName: string;
    firstName: string;
    birthDate: string;
    emails: string[];
    phoneNumbers?: string[];
}

export interface UpdatePersonalInfoRequest {
    lastName?: string;
    firstName?: string;
    birthDate: string;
}

export interface AddEmailRequest {
    email: string;
}

export interface AddPhoneNumberRequest {
    phoneNumber: string;
}

export interface ChangePasswordRequest {
    currentPassword: string;
    newPassword: string;
}

export const userService = {
    getAllUsers: async () => {
        return await apiClient.get('/users');
    },

    getUserById: async (userId: string) => {
        return await apiClient.get<UserResponse>(`/users/${userId}`);
    },

    getUserByLogin: async (login: string) => {
        return await apiClient.get('/users/search/by-login', { params: { login } });
    },

    getUserByEmail: async (email: string) => {
        return await apiClient.get('/users/search/by-email', { params: { email } });
    },

    registerUser: async (userData: RegisterUserRequest) => {
        return await apiClient.post<UserResponse>('/users', userData);
    },

    deleteUser: async (userId: string) => {
        return await apiClient.delete(`/users/${userId}`);
    },

    updatePersonalInfo: async (userId: string, data: UpdatePersonalInfoRequest) => {
        return await apiClient.put(`/users/${userId}/personal-info`, data);
    },

    addEmail: async (userId: string, data: AddEmailRequest) => {
        return await apiClient.post(`/users/${userId}/emails`, data);
    },

    removeEmail: async (userId: string, email: string) => {
        return await apiClient.delete(`/users/${userId}/emails/${email}`);
    },

    addPhoneNumber: async (userId: string, data: AddPhoneNumberRequest) => {
        return await apiClient.post(`/users/${userId}/phone-numbers`, data);
    },

    removePhoneNumber: async (userId: string, phoneNumber: string) => {
        return await apiClient.delete(`/users/${userId}/phone-numbers/${phoneNumber}`);
    },

    changePassword: async (userId: string, data: ChangePasswordRequest) => {
        return await apiClient.post(`/users/${userId}/password/change`, data);
    },

    getUserRoles: async (userId: string) => {
        return await apiClient.get(`/users/${userId}/roles`);
    },

    getUserPermissions: async (userId: string) => {
        return await apiClient.get(`/users/${userId}/roles/permissions`);
    },

    assignRole: async (userId: string, roleId: string) => {
        return await apiClient.post(`/users/${userId}/roles/${roleId}`);
    },

    removeRole: async (userId: string, roleId: string) => {
        return await apiClient.delete(`/users/${userId}/roles/${roleId}`);
    }
};