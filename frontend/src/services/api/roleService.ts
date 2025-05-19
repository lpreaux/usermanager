import { apiClient } from './index';

export interface RoleDTO {
    id: string;
    name: string;
    description: string;
    permissions: string[];
}

export interface CreateRoleRequest {
    name: string;
    description?: string;
    permissions: string[];
}

export interface UpdateRoleRequest {
    name: string;
    description?: string;
}

export const roleService = {
    getAllRoles: async () => {
        return await apiClient.get<RoleDTO[]>('/roles');
    },

    getRoleById: async (roleId: string) => {
        return await apiClient.get<RoleDTO>(`/roles/${roleId}`);
    },

    createRole: async (roleData: CreateRoleRequest) => {
        return await apiClient.post('/roles', roleData);
    },

    updateRole: async (roleId: string, roleData: UpdateRoleRequest) => {
        return await apiClient.put(`/roles/${roleId}`, roleData);
    },

    deleteRole: async (roleId: string) => {
        return await apiClient.delete(`/roles/${roleId}`);
    },

    addPermission: async (roleId: string, permission: string) => {
        return await apiClient.post(`/roles/${roleId}/permissions/${permission}`);
    },

    removePermission: async (roleId: string, permission: string) => {
        return await apiClient.delete(`/roles/${roleId}/permissions/${permission}`);
    }
};