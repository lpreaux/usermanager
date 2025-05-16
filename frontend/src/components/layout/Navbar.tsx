import { Link } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';

const Navbar = () => {
    const { isAuthenticated, logout } = useAuthStore();

    return (
        <div className="navbar bg-base-100 shadow-md">
            <div className="container mx-auto">
                <div className="flex-1">
                    <Link to="/" className="btn btn-ghost normal-case text-xl">User Manager</Link>
                </div>
                <div className="flex-none">
                    <ul className="menu menu-horizontal px-1">
                        {isAuthenticated ? (
                            <>
                                <li><Link to="/users">Utilisateurs</Link></li>
                                <li><Link to="/roles">Rôles</Link></li>
                                <li><button onClick={logout} className="btn btn-ghost">Déconnexion</button></li>
                            </>
                        ) : (
                            <li><Link to="/login">Connexion</Link></li>
                        )}
                    </ul>
                </div>
            </div>
        </div>
    );
};

export default Navbar;
