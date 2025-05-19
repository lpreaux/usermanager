import { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { userService, roleService, type RoleDTO } from '../../../services/api';

interface UserRoleManagerProps {
    userId: string;
}

const UserRoleManager = ({ userId }: UserRoleManagerProps) => {
    const [isLoading, setIsLoading] = useState(false);
    const [userRoles, setUserRoles] = useState<string[]>([]);
    const [selectedRole, setSelectedRole] = useState<string>('');

    // Récupérer la liste des rôles disponibles
    const { data: allRoles = [] } = useQuery({
        queryKey: ['roles'],
        queryFn: roleService.getAllRoles,
        select: (response) => response.data || [],
    });

    // Récupérer les rôles de l'utilisateur
    const fetchUserRoles = async () => {
        try {
            const response = await userService.getUserRoles(userId);
            const rolesList = response.data || [];
            // Si la réponse est un tableau d'objets RoleDTO, extraire les IDs
            if (rolesList.length > 0 && typeof rolesList[0] === 'object' && 'id' in rolesList[0]) {
                setUserRoles(rolesList.map((role: RoleDTO) => role.id));
            } else if (Array.isArray(rolesList)) {
                // Si c'est un tableau simple de chaînes
                setUserRoles(rolesList);
            }
        } catch (error) {
            console.error('Erreur lors de la récupération des rôles de l\'utilisateur:', error);
            toast.error('Erreur lors de la récupération des rôles');
        }
    };

    useEffect(() => {
        fetchUserRoles();
    }, [userId]);

    // Assigner un rôle à l'utilisateur
    const handleAssignRole = async () => {
        if (!selectedRole) return;

        setIsLoading(true);
        try {
            await userService.assignRole(userId, selectedRole);
            toast.success('Rôle assigné avec succès');
            fetchUserRoles(); // Actualiser la liste des rôles
            setSelectedRole(''); // Réinitialiser la sélection
        } catch (error) {
            console.error('Erreur lors de l\'assignation du rôle:', error);
            toast.error('Erreur lors de l\'assignation du rôle');
        } finally {
            setIsLoading(false);
        }
    };

    // Retirer un rôle de l'utilisateur
    const handleRemoveRole = async (roleId: string) => {
        setIsLoading(true);
        try {
            await userService.removeRole(userId, roleId);
            toast.success('Rôle retiré avec succès');
            fetchUserRoles(); // Actualiser la liste des rôles
        } catch (error) {
            console.error('Erreur lors du retrait du rôle:', error);
            toast.error('Erreur lors du retrait du rôle');
        } finally {
            setIsLoading(false);
        }
    };

    // Filtrer les rôles disponibles (ceux qui ne sont pas déjà assignés)
    const availableRoles = allRoles.filter((role: RoleDTO) => !userRoles.includes(role.id));

    return (
        <div className="card bg-base-100 shadow-lg mt-6">
            <div className="card-body">
                <h2 className="card-title">Gestion des rôles</h2>

                <div className="space-y-4">
                    <h3 className="font-bold">Rôles actuels</h3>

                    {userRoles.length > 0 ? (
                        <div className="space-y-2">
                            {allRoles
                                .filter((role: RoleDTO) => userRoles.includes(role.id))
                                .map((role: RoleDTO) => (
                                    <div key={role.id} className="flex justify-between items-center p-2 bg-base-200 rounded-lg">
                                        <div>
                                            <span className="font-medium">{role.name}</span>
                                            {role.description && (
                                                <p className="text-sm text-base-content/70">{role.description}</p>
                                            )}
                                        </div>
                                        <button
                                            className="btn btn-sm btn-error"
                                            onClick={() => handleRemoveRole(role.id)}
                                            disabled={isLoading}
                                        >
                                            Retirer
                                        </button>
                                    </div>
                                ))}
                        </div>
                    ) : (
                        <p>Aucun rôle assigné</p>
                    )}

                    <h3 className="font-bold mt-4">Ajouter un rôle</h3>

                    {availableRoles.length > 0 ? (
                        <div className="flex gap-2">
                            <select
                                className="select select-bordered flex-1"
                                value={selectedRole}
                                onChange={(e) => setSelectedRole(e.target.value)}
                            >
                                <option value="">Sélectionner un rôle...</option>
                                {availableRoles.map((role: RoleDTO) => (
                                    <option key={role.id} value={role.id}>
                                        {role.name}
                                    </option>
                                ))}
                            </select>

                            <button
                                className="btn btn-primary"
                                onClick={handleAssignRole}
                                disabled={!selectedRole || isLoading}
                            >
                                {isLoading ? 'Ajout...' : 'Ajouter'}
                            </button>
                        </div>
                    ) : (
                        <p>Tous les rôles disponibles sont déjà assignés</p>
                    )}
                </div>
            </div>
        </div>
    );
};

export default UserRoleManager;