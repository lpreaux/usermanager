import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import toast from 'react-hot-toast';
import { roleService, type CreateRoleRequest, type UpdateRoleRequest } from '../services/api';

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

// Schéma de validation pour la création/modification de rôle
const roleSchema = z.object({
    name: z.string()
        .min(2, 'Le nom doit contenir au moins 2 caractères')
        .max(50, 'Le nom ne peut pas dépasser 50 caractères'),
    description: z.string().max(255, 'La description ne peut pas dépasser 255 caractères').optional(),
    permissions: z.array(z.string())
});

type RoleFormValues = z.infer<typeof roleSchema>;

const RoleFormPage = () => {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const isEditMode = !!id;
    const [isLoading, setIsLoading] = useState(false);

    const { register, handleSubmit, setValue, watch, formState: { errors }, reset } = useForm<RoleFormValues>({
        resolver: zodResolver(roleSchema),
        defaultValues: {
            name: '',
            description: '',
            permissions: []
        }
    });

    const selectedPermissions = watch('permissions') || [];

    // Charger les données du rôle en mode édition
    useEffect(() => {
        if (isEditMode && id) {
            const fetchRole = async () => {
                setIsLoading(true);
                try {
                    const response = await roleService.getRoleById(id);
                    const roleData = response.data;

                    reset({
                        name: roleData.name,
                        description: roleData.description || '',
                        permissions: roleData.permissions || []
                    });
                } catch (error) {
                    console.error('Erreur lors de la récupération du rôle:', error);
                    toast.error('Erreur lors de la récupération du rôle');
                } finally {
                    setIsLoading(false);
                }
            };

            fetchRole();
        }
    }, [id, isEditMode, reset]);

    // Gestion des permissions
    const handlePermissionToggle = (permission: string) => {
        const isSelected = selectedPermissions.includes(permission);
        const newPermissions = isSelected
            ? selectedPermissions.filter(p => p !== permission)
            : [...selectedPermissions, permission];

        setValue('permissions', newPermissions, { shouldValidate: true });
    };

    const onSubmit = async (data: RoleFormValues) => {
        setIsLoading(true);
        try {
            if (isEditMode && id) {
                // Mise à jour du rôle
                const updateData: UpdateRoleRequest = {
                    name: data.name,
                    description: data.description
                };

                await roleService.updateRole(id, updateData);

                // Mettre à jour les permissions une par une
                const currentRole = await roleService.getRoleById(id);
                const currentPermissions = currentRole.data.permissions || [];

                // Ajouter les nouvelles permissions
                for (const permission of data.permissions) {
                    if (!currentPermissions.includes(permission)) {
                        await roleService.addPermission(id, permission);
                    }
                }

                // Supprimer les permissions retirées
                for (const permission of currentPermissions) {
                    if (!data.permissions.includes(permission)) {
                        await roleService.removePermission(id, permission);
                    }
                }

                toast.success('Rôle mis à jour avec succès');
            } else {
                // Création d'un nouveau rôle
                const createData: CreateRoleRequest = {
                    name: data.name,
                    description: data.description,
                    permissions: data.permissions
                };

                await roleService.createRole(createData);
                toast.success('Rôle créé avec succès');
            }

            navigate('/roles');
        } catch (error: any) {
            console.error('Erreur lors de l\'opération:', error);
            toast.error(error.response?.data?.message || 'Erreur lors de l\'opération');
        } finally {
            setIsLoading(false);
        }
    };

    if (isLoading && isEditMode) {
        return <div className="flex justify-center p-8"><span className="loading loading-spinner loading-lg"></span></div>;
    }

    return (
        <div className="card bg-base-100 shadow-xl">
            <div className="card-body">
                <h2 className="card-title text-2xl mb-6">
                    {isEditMode ? 'Modifier le rôle' : 'Créer un nouveau rôle'}
                </h2>

                <form onSubmit={handleSubmit(onSubmit)}>
                    <div className="form-control">
                        <label className="label">
                            <span className="label-text">Nom *</span>
                        </label>
                        <input
                            type="text"
                            className={`input input-bordered ${errors.name ? 'input-error' : ''}`}
                            {...register('name')}
                        />
                        {errors.name && (
                            <label className="label">
                                <span className="label-text-alt text-error">{errors.name.message}</span>
                            </label>
                        )}
                    </div>

                    <div className="form-control mt-4">
                        <label className="label">
                            <span className="label-text">Description</span>
                        </label>
                        <textarea
                            className={`textarea textarea-bordered h-24 ${errors.description ? 'textarea-error' : ''}`}
                            {...register('description')}
                        />
                        {errors.description && (
                            <label className="label">
                                <span className="label-text-alt text-error">{errors.description.message}</span>
                            </label>
                        )}
                    </div>

                    <div className="form-control mt-4">
                        <label className="label">
                            <span className="label-text">Permissions</span>
                        </label>

                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-2 mt-2">
                            {availablePermissions.map(permission => (
                                <div key={permission} className="form-control">
                                    <label className="label cursor-pointer justify-start">
                                        <input
                                            type="checkbox"
                                            className="checkbox checkbox-primary mr-2"
                                            checked={selectedPermissions.includes(permission)}
                                            onChange={() => handlePermissionToggle(permission)}
                                        />
                                        <span className="label-text">{permission}</span>
                                    </label>
                                </div>
                            ))}
                        </div>

                        {errors.permissions && (
                            <label className="label">
                                <span className="label-text-alt text-error">{errors.permissions.message}</span>
                            </label>
                        )}
                    </div>

                    <div className="form-control mt-6">
                        <button
                            type="submit"
                            className={`btn btn-primary ${isLoading ? 'loading' : ''}`}
                            disabled={isLoading}
                        >
                            {isLoading
                                ? (isEditMode ? 'Mise à jour...' : 'Création...')
                                : (isEditMode ? 'Mettre à jour' : 'Créer')
                            }
                        </button>
                    </div>
                </form>

                <div className="mt-4">
                    <button
                        className="btn btn-outline"
                        onClick={() => navigate('/roles')}
                    >
                        Retour à la liste
                    </button>
                </div>
            </div>
        </div>
    );
};

export default RoleFormPage;