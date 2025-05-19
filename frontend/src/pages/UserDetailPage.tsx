import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { userService } from '../services/api';
import UserContactManager from '../components/features/user/UserContactManager';
import UserRoleManager from '../components/features/user/UserRoleManager';
import PasswordChangeForm from '../components/features/user/PasswordChangeForm';

const UserDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const [showContactManager, setShowContactManager] = useState(false);
  const [showRoleManager, setShowRoleManager] = useState(false);

  const { data: user, isLoading, error, refetch } = useQuery({
    queryKey: ['user', id],
    queryFn: () => userService.getUserById(id!),
    select: (response) => response.data,
    enabled: !!id,
  });

  if (isLoading) return <div className="flex justify-center"><div className="loading loading-spinner loading-lg"></div></div>;

  if (error) return (
      <div className="alert alert-error">
        Une erreur est survenue: {(error as Error).message}
      </div>
  );

  if (!user) return (
      <div className="alert alert-warning">
        Utilisateur non trouvé
      </div>
  );

  return (
      <div>
        <div className="flex justify-between items-center mb-6">
          <h1 className="text-2xl font-bold">Détail de l'utilisateur</h1>
          <div className="flex space-x-2">
            <Link to="/users" className="btn btn-outline">
              Retour
            </Link>
            <Link to={`/users/${id}/edit`} className="btn btn-warning">
              Modifier
            </Link>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Carte d'informations utilisateur */}
          <div className="card bg-base-100 shadow-xl">
            <div className="card-body">
              <h2 className="card-title">{user.firstName} {user.lastName}</h2>

              <div className="grid grid-cols-1 gap-4 mt-4">
                <div>
                  <h3 className="font-bold">Informations générales</h3>
                  <ul className="mt-2 space-y-2">
                    <li><strong>ID:</strong> {user.id}</li>
                    <li><strong>Login:</strong> {user.login}</li>
                    <li><strong>Date de naissance:</strong> {user.birthDate}</li>
                    <li><strong>Âge:</strong> {user.age} ans</li>
                    <li><strong>Adulte:</strong> {user.isAdult ? 'Oui' : 'Non'}</li>
                  </ul>
                </div>

                <div>
                  <h3 className="font-bold">Contacts</h3>
                  <div className="mt-2">
                    <h4 className="font-semibold">Emails:</h4>
                    <ul className="list-disc list-inside">
                      {user.emails.map((email: string) => (
                          <li key={email}>{email}</li>
                      ))}
                    </ul>
                  </div>

                  {user.phoneNumbers && user.phoneNumbers.length > 0 && (
                      <div className="mt-4">
                        <h4 className="font-semibold">Téléphones:</h4>
                        <ul className="list-disc list-inside">
                          {user.phoneNumbers.map((phone: string) => (
                              <li key={phone}>{phone}</li>
                          ))}
                        </ul>
                      </div>
                  )}
                </div>

                <div className="card-actions justify-end">
                  <button
                      className="btn btn-primary btn-sm"
                      onClick={() => setShowContactManager(!showContactManager)}
                  >
                    {showContactManager ? 'Masquer' : 'Gérer les contacts'}
                  </button>
                  <button
                      className="btn btn-secondary btn-sm"
                      onClick={() => setShowRoleManager(!showRoleManager)}
                  >
                    {showRoleManager ? 'Masquer' : 'Gérer les rôles'}
                  </button>
                </div>

                <div>
                  <PasswordChangeForm userId={user.id} />
                </div>
              </div>
            </div>
          </div>

          {/* Zone de gestion des contacts et rôles */}
          <div>
            {showContactManager && (
                <UserContactManager
                    userId={user.id}
                    emails={user.emails}
                    phoneNumbers={user.phoneNumbers}
                    onUpdate={refetch}
                />
            )}

            {showRoleManager && (
                <UserRoleManager userId={user.id} />
            )}
          </div>
        </div>
      </div>
  );
};

export default UserDetailPage;