import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { userService } from '../services/api';
import toast from 'react-hot-toast';

const UsersPage = () => {
  const [isDeleting, setIsDeleting] = useState(false);
  
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ['users'],
    queryFn: userService.getAllUsers,
    select: (response) => response.data._embedded?.userResponseList || [],
  });

  const handleDelete = async (id: string) => {
    if (!confirm('Êtes-vous sûr de vouloir supprimer cet utilisateur ?')) return;
    
    setIsDeleting(true);
    try {
      await userService.deleteUser(id);
      toast.success('Utilisateur supprimé avec succès');
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
        <h1 className="text-2xl font-bold">Utilisateurs</h1>
        <Link to="/users/new" className="btn btn-primary">
          Nouvel Utilisateur
        </Link>
      </div>
      
      <div className="overflow-x-auto">
        <table className="table w-full">
          <thead>
            <tr>
              <th>Login</th>
              <th>Nom</th>
              <th>Prénom</th>
              <th>Emails</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {data?.map((user: any) => (
              <tr key={user.id}>
                <td>{user.login}</td>
                <td>{user.lastName}</td>
                <td>{user.firstName}</td>
                <td>
                  <div className="flex flex-col">
                    {user.emails.map((email: string) => (
                      <span key={email} className="badge badge-outline">{email}</span>
                    ))}
                  </div>
                </td>
                <td>
                  <div className="flex space-x-2">
                    <Link to={`/users/${user.id}`} className="btn btn-sm btn-info">
                      Voir
                    </Link>
                    <Link to={`/users/${user.id}/edit`} className="btn btn-sm btn-warning">
                      Modifier
                    </Link>
                    <button 
                      className="btn btn-sm btn-error"
                      onClick={() => handleDelete(user.id)}
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

export default UsersPage;
