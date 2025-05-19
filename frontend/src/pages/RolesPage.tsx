import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import { roleService, type RoleDTO } from '../services/api';

const RolesPage = () => {
    const [isDeleting, setIsDeleting] = useState(false);

    const { data, isLoading, error, refetch } = useQuery({
        queryKey: ['roles'],
        queryFn: roleService.getAllRoles,
        select: (response) => response.data || [],
    });

    const handleDelete = async (id: string) => {
        if (!confirm('Êtes-vous sûr de vouloir supprimer ce rôle ?')) return;

        setIsDeleting(true);
        try {
            await roleService.deleteRole(id);
            toast.success('Rôle supprimé avec succès');
            refetch();
        } catch (error) {
            toast.error('Erreur lors de la suppression');
        } finally {
            setIsDeleting(false);
        }
    };

    if (isLoading) return <div className="loading loading-spinner loading-lg"></div>;

    if (error) return (
        <div className="alert alert-error">
            Une erreur est survenue: {(error as Error).message}
        </div>
    );

    return (
        <div>
            <div className="flex justify-between items-center mb-6">
                <h1 className="text-2xl font-bold">Gestion des rôles</h1>
                <Link to="/roles/new" className="btn btn-primary">
                    Nouveau Rôle
                </Link>
            </div>

            <div className="overflow-x-auto">
                <table className="table w-full">
                    <thead>
                    <tr>
                        <th>Nom</th>
                        <th>Description</th>
                        <th>Permissions</th>
                        <th>Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    {data?.map((role: RoleDTO) => (
                        <tr key={role.id}>
                            <td className="font-medium">{role.name}</td>
                            <td>{role.description}</td>
                            <td>
                                <div className="flex flex-wrap gap-1">
                                    {role.permissions.map((permission: string) => (
                                        <span key={permission} className="badge badge-secondary badge-sm">
                        {permission}
                      </span>
                                    ))}
                                </div>
                            </td>
                            <td>
                                <div className="flex space-x-2">
                                    <Link to={`/roles/${role.id}`} className="btn btn-sm btn-info">
                                        Voir
                                    </Link>
                                    <Link to={`/roles/${role.id}/edit`} className="btn btn-sm btn-warning">
                                        Modifier
                                    </Link>
                                    <button
                                        className="btn btn-sm btn-error"
                                        onClick={() => handleDelete(role.id)}
                                        disabled={isDeleting}
                                    >
                                        Supprimer
                                    </button>
                                </div>
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

export default RolesPage;