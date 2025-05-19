import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { roleService } from '../services/api';
import PermissionManager from '../components/features/role/PermissionManager';

const RoleDetailPage = () => {
    const { id } = useParams<{ id: string }>();
    const [showPermissionManager, setShowPermissionManager] = useState(false);

    const { data: role, isLoading, error, refetch } = useQuery({
        queryKey: ['role', id],
        queryFn: () => roleService.getRoleById(id!),
        select: (response) => response.data,
        enabled: !!id,
    });

    if (isLoading) return <div className="loading loading-spinner loading-lg"></div>;

    if (error) return (
        <div className="alert alert-error">
            Une erreur est survenue: {(error as Error).message}
        </div>
    );

    if (!role) return (
        <div className="alert alert-warning">
            Rôle non trouvé
        </div>
    );

    return (
        <div>
            <div className="flex justify-between items-center mb-6">
                <h1 className="text-2xl font-bold">Détail du rôle</h1>
                <div className="flex space-x-2">
                    <Link to="/roles" className="btn btn-outline">
                        Retour
                    </Link>
                    <Link to={`/roles/${id}/edit`} className="btn btn-warning">
                        Modifier
                    </Link>
                </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <div className="card bg-base-100 shadow-xl">
                    <div className="card-body">
                        <h2 className="card-title">{role.name}</h2>

                        <div className="grid grid-cols-1 gap-4 mt-4">
                            <div>
                                <h3 className="font-bold">Informations générales</h3>
                                <ul className="mt-2 space-y-2">
                                    <li><strong>ID:</strong> {role.id}</li>
                                    <li><strong>Nom:</strong> {role.name}</li>
                                    <li><strong>Description:</strong> {role.description || 'Aucune description'}</li>
                                </ul>
                            </div>

                            <div>
                                <h3 className="font-bold">Permissions</h3>
                                <div className="mt-2">
                                    <div className="flex flex-wrap gap-2">
                                        {role.permissions.length > 0 ? (
                                            role.permissions.map(permission => (
                                                <span key={permission} className="badge badge-secondary p-3">
                          {permission}
                        </span>
                                            ))
                                        ) : (
                                            <p>Aucune permission</p>
                                        )}
                                    </div>
                                </div>
                            </div>

                            <div className="card-actions justify-end">
                                <button
                                    className="btn btn-primary btn-sm"
                                    onClick={() => setShowPermissionManager(!showPermissionManager)}
                                >
                                    {showPermissionManager ? 'Masquer' : 'Gérer les permissions'}
                                </button>
                            </div>
                        </div>
                    </div>
                </div>

                <div>
                    {showPermissionManager && (
                        <PermissionManager
                            roleId={role.id}
                            currentPermissions={role.permissions}
                            onUpdate={refetch}
                        />
                    )}
                </div>
            </div>
        </div>
    );
};

export default RoleDetailPage;