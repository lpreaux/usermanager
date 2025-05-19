import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useForm, useFieldArray } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import toast from 'react-hot-toast';
import { format } from 'date-fns';
import { userService, type RegisterUserRequest, type UpdatePersonalInfoRequest } from '../services/api';

// Schéma de validation pour la création d'utilisateur
const createUserSchema = z.object({
    login: z.string()
        .min(4, 'Le login doit contenir au moins 4 caractères')
        .max(50, 'Le login ne peut pas dépasser 50 caractères')
        .regex(/^[a-zA-Z0-9._-]+$/, 'Le login ne peut contenir que des lettres, chiffres, points, tirets et underscores'),

    password: z.string()
        .min(8, 'Le mot de passe doit contenir au moins 8 caractères')
        .max(100, 'Le mot de passe ne peut pas dépasser 100 caractères')
        .regex(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^\da-zA-Z]).{8,}$/,
            'Le mot de passe doit contenir au moins une minuscule, une majuscule, un chiffre et un caractère spécial'),

    lastName: z.string().max(100, 'Le nom ne peut pas dépasser 100 caractères'),
    firstName: z.string().max(100, 'Le prénom ne peut pas dépasser 100 caractères'),
    birthDate: z.string().refine(date => !isNaN(Date.parse(date)), 'Format de date invalide'),

    emails: z.array(
        z.object({
            value: z.string().email('Format d\'email invalide')
        })
    ).min(1, 'Au moins un email est requis'),

    phoneNumbers: z.array(
        z.object({
            value: z.string().regex(/^\+?[0-9\s-]{6,20}$/, 'Format de téléphone invalide')
        })
    ).optional()
});

// Schéma de validation pour la mise à jour d'utilisateur
const updateUserSchema = z.object({
    lastName: z.string().max(100, 'Le nom ne peut pas dépasser 100 caractères'),
    firstName: z.string().max(100, 'Le prénom ne peut pas dépasser 100 caractères'),
    birthDate: z.string().refine(date => !isNaN(Date.parse(date)), 'Format de date invalide')
});

type CreateUserFormValues = z.infer<typeof createUserSchema>;
type UpdateUserFormValues = z.infer<typeof updateUserSchema>;

const UserFormPage = () => {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const isEditMode = !!id;
    const [isLoading, setIsLoading] = useState(false);
    const [user, setUser] = useState<any>(null);

    // Formulaire pour la création d'utilisateur
    const createForm = useForm<CreateUserFormValues>({
        resolver: zodResolver(createUserSchema),
        defaultValues: {
            login: '',
            password: '',
            lastName: '',
            firstName: '',
            birthDate: format(new Date(), 'yyyy-MM-dd'),
            emails: [{ value: '' }],
            phoneNumbers: []
        }
    });

    // Formulaire pour la mise à jour d'utilisateur
    const updateForm = useForm<UpdateUserFormValues>({
        resolver: zodResolver(updateUserSchema),
        defaultValues: {
            lastName: '',
            firstName: '',
            birthDate: format(new Date(), 'yyyy-MM-dd')
        }
    });

    // Field arrays pour les emails et téléphones
    const { fields: emailFields, append: appendEmail, remove: removeEmail } = useFieldArray({
        control: createForm.control,
        name: 'emails'
    });

    const { fields: phoneFields, append: appendPhone, remove: removePhone } = useFieldArray({
        control: createForm.control,
        name: 'phoneNumbers'
    });

    // Charger les données de l'utilisateur en mode édition
    useEffect(() => {
        if (isEditMode && id) {
            const fetchUser = async () => {
                setIsLoading(true);
                try {
                    const response = await userService.getUserById(id);
                    const userData = response.data;
                    setUser(userData);

                    // Mise à jour du formulaire d'édition
                    updateForm.reset({
                        lastName: userData.lastName,
                        firstName: userData.firstName,
                        birthDate: userData.birthDate
                    });
                } catch (error) {
                    console.error('Erreur lors de la récupération de l\'utilisateur:', error);
                    toast.error('Erreur lors de la récupération de l\'utilisateur');
                } finally {
                    setIsLoading(false);
                }
            };

            fetchUser();
        }
    }, [id, isEditMode, updateForm]);

    // Soumission du formulaire de création
    const onCreateSubmit = async (data: CreateUserFormValues) => {
        setIsLoading(true);
        try {
            // Transformer les données pour l'API
            const userData: RegisterUserRequest = {
                login: data.login,
                password: data.password,
                lastName: data.lastName,
                firstName: data.firstName,
                birthDate: data.birthDate,
                emails: data.emails.map(e => e.value),
                phoneNumbers: data.phoneNumbers?.map(p => p.value)
            };

            const response = await userService.registerUser(userData);
            toast.success('Utilisateur créé avec succès');
            navigate(`/users/${response.data.id}`);
        } catch (error: any) {
            console.error('Erreur lors de la création de l\'utilisateur:', error);
            toast.error(error.response?.data?.message || 'Erreur lors de la création de l\'utilisateur');
        } finally {
            setIsLoading(false);
        }
    };

    // Soumission du formulaire de mise à jour
    const onUpdateSubmit = async (data: UpdateUserFormValues) => {
        if (!id) return;

        setIsLoading(true);
        try {
            const updateData: UpdatePersonalInfoRequest = {
                lastName: data.lastName,
                firstName: data.firstName,
                birthDate: data.birthDate
            };

            await userService.updatePersonalInfo(id, updateData);
            toast.success('Informations mises à jour avec succès');
            navigate(`/users/${id}`);
        } catch (error: any) {
            console.error('Erreur lors de la mise à jour:', error);
            toast.error(error.response?.data?.message || 'Erreur lors de la mise à jour');
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
                    {isEditMode ? 'Modifier l\'utilisateur' : 'Ajouter un utilisateur'}
                </h2>

                {isEditMode ? (
                    // Formulaire de mise à jour
                    <form onSubmit={updateForm.handleSubmit(onUpdateSubmit)}>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <div className="form-control">
                                <label className="label">
                                    <span className="label-text">Nom</span>
                                </label>
                                <input
                                    type="text"
                                    className={`input input-bordered ${updateForm.formState.errors.lastName ? 'input-error' : ''}`}
                                    {...updateForm.register('lastName')}
                                />
                                {updateForm.formState.errors.lastName && (
                                    <label className="label">
                                        <span className="label-text-alt text-error">{updateForm.formState.errors.lastName.message}</span>
                                    </label>
                                )}
                            </div>

                            <div className="form-control">
                                <label className="label">
                                    <span className="label-text">Prénom</span>
                                </label>
                                <input
                                    type="text"
                                    className={`input input-bordered ${updateForm.formState.errors.firstName ? 'input-error' : ''}`}
                                    {...updateForm.register('firstName')}
                                />
                                {updateForm.formState.errors.firstName && (
                                    <label className="label">
                                        <span className="label-text-alt text-error">{updateForm.formState.errors.firstName.message}</span>
                                    </label>
                                )}
                            </div>
                        </div>

                        <div className="form-control mt-4">
                            <label className="label">
                                <span className="label-text">Date de naissance</span>
                            </label>
                            <input
                                type="date"
                                className={`input input-bordered ${updateForm.formState.errors.birthDate ? 'input-error' : ''}`}
                                {...updateForm.register('birthDate')}
                            />
                            {updateForm.formState.errors.birthDate && (
                                <label className="label">
                                    <span className="label-text-alt text-error">{updateForm.formState.errors.birthDate.message}</span>
                                </label>
                            )}
                        </div>

                        <div className="form-control mt-6">
                            <button
                                type="submit"
                                className={`btn btn-primary ${isLoading ? 'loading' : ''}`}
                                disabled={isLoading}
                            >
                                {isLoading ? 'Enregistrement...' : 'Enregistrer les modifications'}
                            </button>
                        </div>
                    </form>
                ) : (
                    // Formulaire de création
                    <form onSubmit={createForm.handleSubmit(onCreateSubmit)}>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <div className="form-control">
                                <label className="label">
                                    <span className="label-text">Login *</span>
                                </label>
                                <input
                                    type="text"
                                    className={`input input-bordered ${createForm.formState.errors.login ? 'input-error' : ''}`}
                                    {...createForm.register('login')}
                                />
                                {createForm.formState.errors.login && (
                                    <label className="label">
                                        <span className="label-text-alt text-error">{createForm.formState.errors.login.message}</span>
                                    </label>
                                )}
                            </div>

                            <div className="form-control">
                                <label className="label">
                                    <span className="label-text">Mot de passe *</span>
                                </label>
                                <input
                                    type="password"
                                    className={`input input-bordered ${createForm.formState.errors.password ? 'input-error' : ''}`}
                                    {...createForm.register('password')}
                                />
                                {createForm.formState.errors.password && (
                                    <label className="label">
                                        <span className="label-text-alt text-error">{createForm.formState.errors.password.message}</span>
                                    </label>
                                )}
                            </div>
                        </div>

                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-4">
                            <div className="form-control">
                                <label className="label">
                                    <span className="label-text">Nom</span>
                                </label>
                                <input
                                    type="text"
                                    className={`input input-bordered ${createForm.formState.errors.lastName ? 'input-error' : ''}`}
                                    {...createForm.register('lastName')}
                                />
                                {createForm.formState.errors.lastName && (
                                    <label className="label">
                                        <span className="label-text-alt text-error">{createForm.formState.errors.lastName.message}</span>
                                    </label>
                                )}
                            </div>

                            <div className="form-control">
                                <label className="label">
                                    <span className="label-text">Prénom</span>
                                </label>
                                <input
                                    type="text"
                                    className={`input input-bordered ${createForm.formState.errors.firstName ? 'input-error' : ''}`}
                                    {...createForm.register('firstName')}
                                />
                                {createForm.formState.errors.firstName && (
                                    <label className="label">
                                        <span className="label-text-alt text-error">{createForm.formState.errors.firstName.message}</span>
                                    </label>
                                )}
                            </div>
                        </div>

                        <div className="form-control mt-4">
                            <label className="label">
                                <span className="label-text">Date de naissance *</span>
                            </label>
                            <input
                                type="date"
                                className={`input input-bordered ${createForm.formState.errors.birthDate ? 'input-error' : ''}`}
                                {...createForm.register('birthDate')}
                            />
                            {createForm.formState.errors.birthDate && (
                                <label className="label">
                                    <span className="label-text-alt text-error">{createForm.formState.errors.birthDate.message}</span>
                                </label>
                            )}
                        </div>

                        <div className="form-control mt-4">
                            <label className="label">
                                <span className="label-text">Emails *</span>
                            </label>

                            {emailFields.map((field, index) => (
                                <div key={field.id} className="flex items-center gap-2 mb-2">
                                    <input
                                        type="email"
                                        className={`input input-bordered flex-grow ${
                                            createForm.formState.errors.emails?.[index]?.value ? 'input-error' : ''
                                        }`}
                                        {...createForm.register(`emails.${index}.value`)}
                                    />

                                    <button
                                        type="button"
                                        className="btn btn-error btn-square btn-sm"
                                        onClick={() => index > 0 && removeEmail(index)}
                                        disabled={index === 0}
                                    >
                                        <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                                            <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
                                        </svg>
                                    </button>
                                </div>
                            ))}

                            {createForm.formState.errors.emails && (
                                <label className="label">
                  <span className="label-text-alt text-error">
                    {typeof createForm.formState.errors.emails.message === 'string'
                        ? createForm.formState.errors.emails.message
                        : 'Format d\'email invalide'}
                  </span>
                                </label>
                            )}

                            <button
                                type="button"
                                className="btn btn-outline btn-sm mt-1"
                                onClick={() => appendEmail({ value: '' })}
                            >
                                Ajouter un email
                            </button>
                        </div>

                        <div className="form-control mt-4">
                            <label className="label">
                                <span className="label-text">Numéros de téléphone</span>
                            </label>

                            {phoneFields.map((field, index) => (
                                <div key={field.id} className="flex items-center gap-2 mb-2">
                                    <input
                                        type="text"
                                        className={`input input-bordered flex-grow ${
                                            createForm.formState.errors.phoneNumbers?.[index]?.value ? 'input-error' : ''
                                        }`}
                                        placeholder="+33612345678"
                                        {...createForm.register(`phoneNumbers.${index}.value`)}
                                    />

                                    <button
                                        type="button"
                                        className="btn btn-error btn-square btn-sm"
                                        onClick={() => removePhone(index)}
                                    >
                                        <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                                            <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
                                        </svg>
                                    </button>
                                </div>
                            ))}

                            {createForm.formState.errors.phoneNumbers && (
                                <label className="label">
                  <span className="label-text-alt text-error">
                    Format de téléphone invalide
                  </span>
                                </label>
                            )}

                            <button
                                type="button"
                                className="btn btn-outline btn-sm mt-1"
                                onClick={() => appendPhone({ value: '' })}
                            >
                                Ajouter un numéro de téléphone
                            </button>
                        </div>

                        <div className="form-control mt-6">
                            <button
                                type="submit"
                                className={`btn btn-primary ${isLoading ? 'loading' : ''}`}
                                disabled={isLoading}
                            >
                                {isLoading ? 'Création en cours...' : 'Créer l\'utilisateur'}
                            </button>
                        </div>
                    </form>
                )}

                <div className="mt-4">
                    <button
                        className="btn btn-outline"
                        onClick={() => navigate('/users')}
                    >
                        Retour à la liste
                    </button>
                </div>
            </div>
        </div>
    );
};

export default UserFormPage;