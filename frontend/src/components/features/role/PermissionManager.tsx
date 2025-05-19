// src/components/role/PermissionManager.tsx
import { useState } from 'react';
import toast from 'react-hot-toast';
import { roleService } from '../../../services/api';

// Liste des permissions disponibles (à adapter en fonction de votre application)
const availablePermissions = [
    'USER_CREATE',
    'USER_READ',
    'USER_UPDATE',
    'USER_DELETE',
    'ROLE_CREATE',
    'ROLE_READ',
    'ROLE_UPDATE',
    'ROLE_DELETE',
    'ADMIN'
];

interface PermissionManagerProps {
    roleId: string;
    currentPermissions: string[];
    onUpdate: () => void;
}

const PermissionManager = ({ roleId, currentPermissions, onUpdate }: PermissionManagerProps) => {
    const [isLoading, setIsLoading] = useState(false);
    const [selectedPermission, setSelectedPermission] = useState<string>('');

    // Ajouter une permission
    const handleAddPermission = async () => {
        if (!selectedPermission) return;

        setIsLoading(true);
        try {
            await roleService.addPermission(roleId, selectedPermission);
            toast.success('Permission ajoutée avec succès');
            setSelectedPermission('');
            onUpdate();
        } catch (error) {
            console.error('Erreur lors de l\'ajout de la permission:', error);
            toast.error('Erreur lors de l\'ajout de la permission');
        } finally {
            setIsLoading(false);
        }
    };

    // Supprimer une permission
    const handleRemovePermission = async (permission: string) => {
        setIsLoading(true);
        try {
            await roleService.removePermission(roleId, permission);
            toast.success('Permission supprimée avec succès');
            onUpdate();
        } catch (error) {
            console.error('Erreur lors de la suppression de la permission:', error);
            toast.error('Erreur lors de la suppression de la permission');
        } finally {
            setIsLoading(false);
        }
    };

    // Filtrer les permissions disponibles (celles qui ne sont pas déjà assignées)
    const unusedPermissions = availablePermissions.filter(p => !currentPermissions.includes(p));

    return (
        <div className="card bg-base-100 shadow-lg mt-6">
            <div className="card-body">
                <h2 className="card-title">Gestion des permissions</h2>

                <div className="space-y-4">
                    <h3 className="font-bold">Permissions actuelles</h3>

                    <div className="flex flex-wrap gap-2">
                        {currentPermissions.length > 0 ? (
                            currentPermissions.map(permission => (
                                <div key={permission} className="badge badge-lg p-4 gap-2">
                                    <span>{permission}</span>
                                    <button
                                        className="btn btn-circle btn-xs btn-error"
                                        onClick={() => handleRemovePermission(permission)}
                                        disabled={isLoading}
                                    >
                                        <svg xmlns="http://www.w3.org/2000/svg" className="h-3 w-3" viewBox="0 0 20 20" fill="currentColor">
                                            <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
                                        </svg>
                                    </button>
                                </div>
                            ))
                        ) : (
                            <p>Aucune permission assignée</p>
                        )}
                    </div>

                    <h3 className="font-bold mt-4">Ajouter une permission</h3>

                    {unusedPermissions.length > 0 ? (
                        <div className="flex gap-2">
                            <select
                                className="select select-bordered flex-1"
                                value={selectedPermission}
                                onChange={(e) => setSelectedPermission(e.target.value)}
                            >
                                <option value="">Sélectionner une permission...</option>
                                {unusedPermissions.map(permission => (
                                    <option key={permission} value={permission}>
                                        {permission}
                                    </option>
                                ))}
                            </select>

                            <button
                                className="btn btn-primary"
                                onClick={handleAddPermission}
                                disabled={!selectedPermission || isLoading}
                            >
                                {isLoading ? 'Ajout...' : 'Ajouter'}
                            </button>
                        </div>
                    ) : (
                        <p>Toutes les permissions disponibles sont déjà assignées</p>
                    )}
                </div>
            </div>
        </div>
    );
};

export default PermissionManager;