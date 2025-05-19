import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import toast from 'react-hot-toast';
import { userService, type ChangePasswordRequest } from '../../../services/api';

// Schéma de validation pour le changement de mot de passe
const passwordChangeSchema = z.object({
  currentPassword: z.string().min(1, 'Mot de passe actuel requis'),
  newPassword: z.string()
    .min(8, 'Le mot de passe doit contenir au moins 8 caractères')
    .max(100, 'Le mot de passe ne peut pas dépasser 100 caractères')
    .regex(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^\da-zA-Z]).{8,}$/,
      'Le mot de passe doit contenir au moins une minuscule, une majuscule, un chiffre et un caractère spécial'),
  confirmPassword: z.string().min(8, 'Confirmation du mot de passe requise')
}).refine(data => data.newPassword === data.confirmPassword, {
  message: 'Les mots de passe ne correspondent pas',
  path: ['confirmPassword'],
});

type PasswordChangeFormValues = z.infer<typeof passwordChangeSchema>;

interface PasswordChangeFormProps {
  userId: string;
  onSuccess?: () => void;
}

const PasswordChangeForm = ({ userId, onSuccess }: PasswordChangeFormProps) => {
  const [isLoading, setIsLoading] = useState(false);
  const [isVisible, setIsVisible] = useState(false);

  const { register, handleSubmit, formState: { errors }, reset } = useForm<PasswordChangeFormValues>({
    resolver: zodResolver(passwordChangeSchema),
    defaultValues: {
      currentPassword: '',
      newPassword: '',
      confirmPassword: ''
    }
  });

  const onSubmit = async (data: PasswordChangeFormValues) => {
    setIsLoading(true);

    try {
      const request: ChangePasswordRequest = {
        currentPassword: data.currentPassword,
        newPassword: data.newPassword
      };

      await userService.changePassword(userId, request);
      toast.success('Mot de passe modifié avec succès');
      reset();
      setIsVisible(false);
      if (onSuccess) {
        onSuccess();
      }
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Erreur lors du changement de mot de passe');
    } finally {
      setIsLoading(false);
    }
  };

  if (!isVisible) {
    return (
      <button
        className="btn btn-primary btn-sm mt-4"
        onClick={() => setIsVisible(true)}
      >
        Changer le mot de passe
      </button>
    );
  }

  return (
    <div className="card bg-base-100 shadow-lg mt-4">
      <div className="card-body">
        <h3 className="card-title text-lg">Changer le mot de passe</h3>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 mt-2">
          <div className="form-control">
            <label className="label">
              <span className="label-text">Mot de passe actuel</span>
            </label>
            <input
              type="password"
              className={`input input-bordered ${errors.currentPassword ? 'input-error' : ''}`}
              {...register('currentPassword')}
            />
            {errors.currentPassword && (
              <label className="label">
                <span className="label-text-alt text-error">{errors.currentPassword.message}</span>
              </label>
            )}
          </div>

          <div className="form-control">
            <label className="label">
              <span className="label-text">Nouveau mot de passe</span>
            </label>
            <input
              type="password"
              className={`input input-bordered ${errors.newPassword ? 'input-error' : ''}`}
              {...register('newPassword')}
            />
            {errors.newPassword && (
              <label className="label">
                <span className="label-text-alt text-error">{errors.newPassword.message}</span>
              </label>
            )}
          </div>

          <div className="form-control">
            <label className="label">
              <span className="label-text">Confirmer le mot de passe</span>
            </label>
            <input
              type="password"
              className={`input input-bordered ${errors.confirmPassword ? 'input-error' : ''}`}
              {...register('confirmPassword')}
            />
            {errors.confirmPassword && (
              <label className="label">
                <span className="label-text-alt text-error">{errors.confirmPassword.message}</span>
              </label>
            )}
          </div>

          <div className="flex space-x-2">
            <button
              type="submit"
              className={`btn btn-primary ${isLoading ? 'loading' : ''}`}
              disabled={isLoading}
            >
              {isLoading ? 'Traitement...' : 'Changer le mot de passe'}
            </button>
            <button
              type="button"
              className="btn btn-outline"
              onClick={() => setIsVisible(false)}
            >
              Annuler
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default PasswordChangeForm;