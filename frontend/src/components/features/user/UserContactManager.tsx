import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import toast from 'react-hot-toast';
import { userService, type AddEmailRequest, type AddPhoneNumberRequest } from '../../../services/api';

// Schémas de validation
const emailSchema = z.object({
    email: z.string().email('Format d\'email invalide'),
});

const phoneSchema = z.object({
    phoneNumber: z.string().regex(/^\+?[0-9\s-]{6,20}$/, 'Format de téléphone invalide'),
});

type EmailFormValues = z.infer<typeof emailSchema>;
type PhoneFormValues = z.infer<typeof phoneSchema>;

interface UserContactManagerProps {
    userId: string;
    emails: string[];
    phoneNumbers?: string[];
    onUpdate: () => void;
}

const UserContactManager = ({ userId, emails, phoneNumbers = [], onUpdate }: UserContactManagerProps) => {
    const [activeTab, setActiveTab] = useState<'emails' | 'phones'>('emails');
    const [isAddingEmail, setIsAddingEmail] = useState(false);
    const [isAddingPhone, setIsAddingPhone] = useState(false);
    const [isProcessing, setIsProcessing] = useState(false);

    // Formulaire pour l'ajout d'email
    const emailForm = useForm<EmailFormValues>({
        resolver: zodResolver(emailSchema),
        defaultValues: { email: '' }
    });

    // Formulaire pour l'ajout de téléphone
    const phoneForm = useForm<PhoneFormValues>({
        resolver: zodResolver(phoneSchema),
        defaultValues: { phoneNumber: '' }
    });

    // Ajout d'un email
    const handleAddEmail = async (data: EmailFormValues) => {
        setIsProcessing(true);
        try {
            const request: AddEmailRequest = { email: data.email };
            await userService.addEmail(userId, request);
            toast.success('Email ajouté avec succès');
            emailForm.reset();
            setIsAddingEmail(false);
            onUpdate();
        } catch (error: any) {
            toast.error(error.response?.data?.message || 'Erreur lors de l\'ajout de l\'email');
        } finally {
            setIsProcessing(false);
        }
    };

    // Suppression d'un email
    const handleRemoveEmail = async (email: string) => {
        if (emails.length <= 1) {
            toast.error('Impossible de supprimer le dernier email');
            return;
        }

        setIsProcessing(true);
        try {
            await userService.removeEmail(userId, email);
            toast.success('Email supprimé avec succès');
            onUpdate();
        } catch (error: any) {
            toast.error(error.response?.data?.message || 'Erreur lors de la suppression de l\'email');
        } finally {
            setIsProcessing(false);
        }
    };

    // Ajout d'un numéro de téléphone
    const handleAddPhone = async (data: PhoneFormValues) => {
        setIsProcessing(true);
        try {
            const request: AddPhoneNumberRequest = { phoneNumber: data.phoneNumber };
            await userService.addPhoneNumber(userId, request);
            toast.success('Numéro de téléphone ajouté avec succès');
            phoneForm.reset();
            setIsAddingPhone(false);
            onUpdate();
        } catch (error: any) {
            toast.error(error.response?.data?.message || 'Erreur lors de l\'ajout du numéro de téléphone');
        } finally {
            setIsProcessing(false);
        }
    };

    // Suppression d'un numéro de téléphone
    const handleRemovePhone = async (phoneNumber: string) => {
        setIsProcessing(true);
        try {
            await userService.removePhoneNumber(userId, phoneNumber);
            toast.success('Numéro de téléphone supprimé avec succès');
            onUpdate();
        } catch (error: any) {
            toast.error(error.response?.data?.message || 'Erreur lors de la suppression du numéro de téléphone');
        } finally {
            setIsProcessing(false);
        }
    };

    return (
        <div className="card bg-base-100 shadow-lg mt-6">
            <div className="card-body">
                <h2 className="card-title">Gestion des contacts</h2>

                <div className="tabs tabs-boxed mb-4">
                    <button
                        className={`tab ${activeTab === 'emails' ? 'tab-active' : ''}`}
                        onClick={() => setActiveTab('emails')}
                    >
                        Emails
                    </button>
                    <button
                        className={`tab ${activeTab === 'phones' ? 'tab-active' : ''}`}
                        onClick={() => setActiveTab('phones')}
                    >
                        Téléphones
                    </button>
                </div>

                {activeTab === 'emails' && (
                    <div>
                        <div className="space-y-2 mb-4">
                            {emails.map(email => (
                                <div key={email} className="flex justify-between items-center p-2 bg-base-200 rounded-lg">
                                    <span>{email}</span>
                                    <button
                                        className="btn btn-sm btn-error"
                                        onClick={() => handleRemoveEmail(email)}
                                        disabled={isProcessing || emails.length <= 1}
                                    >
                                        Supprimer
                                    </button>
                                </div>
                            ))}
                        </div>

                        {isAddingEmail ? (
                            <form onSubmit={emailForm.handleSubmit(handleAddEmail)} className="space-y-2">
                                <div className="form-control">
                                    <input
                                        type="email"
                                        placeholder="Nouvel email"
                                        className={`input input-bordered ${emailForm.formState.errors.email ? 'input-error' : ''}`}
                                        {...emailForm.register('email')}
                                    />
                                    {emailForm.formState.errors.email && (
                                        <label className="label">
                                            <span className="label-text-alt text-error">{emailForm.formState.errors.email.message}</span>
                                        </label>
                                    )}
                                </div>

                                <div className="flex space-x-2">
                                    <button
                                        type="submit"
                                        className="btn btn-sm btn-primary"
                                        disabled={isProcessing}
                                    >
                                        {isProcessing ? 'Ajout...' : 'Ajouter'}
                                    </button>
                                    <button
                                        type="button"
                                        className="btn btn-sm btn-outline"
                                        onClick={() => setIsAddingEmail(false)}
                                    >
                                        Annuler
                                    </button>
                                </div>
                            </form>
                        ) : (
                            <button
                                className="btn btn-sm btn-primary"
                                onClick={() => setIsAddingEmail(true)}
                            >
                                Ajouter un email
                            </button>
                        )}
                    </div>
                )}

                {activeTab === 'phones' && (
                    <div>
                        <div className="space-y-2 mb-4">
                            {phoneNumbers.length > 0 ? (
                                phoneNumbers.map(phone => (
                                    <div key={phone} className="flex justify-between items-center p-2 bg-base-200 rounded-lg">
                                        <span>{phone}</span>
                                        <button
                                            className="btn btn-sm btn-error"
                                            onClick={() => handleRemovePhone(phone)}
                                            disabled={isProcessing}
                                        >
                                            Supprimer
                                        </button>
                                    </div>
                                ))
                            ) : (
                                <p>Aucun numéro de téléphone</p>
                            )}
                        </div>

                        {isAddingPhone ? (
                            <form onSubmit={phoneForm.handleSubmit(handleAddPhone)} className="space-y-2">
                                <div className="form-control">
                                    <input
                                        type="text"
                                        placeholder="+33612345678"
                                        className={`input input-bordered ${phoneForm.formState.errors.phoneNumber ? 'input-error' : ''}`}
                                        {...phoneForm.register('phoneNumber')}
                                    />
                                    {phoneForm.formState.errors.phoneNumber && (
                                        <label className="label">
                                            <span className="label-text-alt text-error">{phoneForm.formState.errors.phoneNumber.message}</span>
                                        </label>
                                    )}
                                </div>

                                <div className="flex space-x-2">
                                    <button
                                        type="submit"
                                        className="btn btn-sm btn-primary"
                                        disabled={isProcessing}
                                    >
                                        {isProcessing ? 'Ajout...' : 'Ajouter'}
                                    </button>
                                    <button
                                        type="button"
                                        className="btn btn-sm btn-outline"
                                        onClick={() => setIsAddingPhone(false)}
                                    >
                                        Annuler
                                    </button>
                                </div>
                            </form>
                        ) : (
                            <button
                                className="btn btn-sm btn-primary"
                                onClick={() => setIsAddingPhone(true)}
                            >
                                Ajouter un téléphone
                            </button>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
};

export default UserContactManager;