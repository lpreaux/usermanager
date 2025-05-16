import { Link } from 'react-router-dom';

const NotFoundPage = () => {
  return (
    <div className="flex flex-col items-center justify-center min-h-[50vh]">
      <h1 className="text-4xl font-bold mb-4">404</h1>
      <p className="text-xl mb-6">Page non trouvée</p>
      <Link to="/" className="btn btn-primary">
        Retourner à l'accueil
      </Link>
    </div>
  );
};

export default NotFoundPage;
