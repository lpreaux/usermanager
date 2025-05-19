import { Route, Routes } from 'react-router-dom';
import { Suspense, lazy } from 'react';
import MainLayout from './components/layout/MainLayout';
import { useAuthStore } from './store/authStore';

// Lazy loading des pages
const HomePage = lazy(() => import('./pages/HomePage'));
const LoginPage = lazy(() => import('./pages/LoginPage'));
const UsersPage = lazy(() => import('./pages/UsersPage'));
const UserDetailPage = lazy(() => import('./pages/UserDetailPage'));
const UserFormPage = lazy(() => import('./pages/UserFormPage'));
const RolesPage = lazy(() => import('./pages/RolesPage'));
const RoleDetailPage = lazy(() => import('./pages/RoleDetailPage'));
const RoleFormPage = lazy(() => import('./pages/RoleFormPage'));
const NotFoundPage = lazy(() => import('./pages/NotFoundPage'));

// Loader pour les composants lazy loaded
const PageLoader = () => (
    <div className="flex justify-center items-center h-screen">
      <span className="loading loading-spinner loading-lg"></span>
    </div>
);

// Composant de protection des routes qui nécessitent une authentification
const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
  const { isAuthenticated } = useAuthStore();

  if (!isAuthenticated) {
    return <LoginPage />;
  }

  return <>{children}</>;
};

const AppRoutes = () => {
  return (
      <Routes>
        <Route path="/" element={<MainLayout />}>
          <Route index element={
            <Suspense fallback={<PageLoader />}>
              <HomePage />
            </Suspense>
          } />

          <Route path="login" element={
            <Suspense fallback={<PageLoader />}>
              <LoginPage />
            </Suspense>
          } />

          {/* Routes des utilisateurs */}
          <Route path="users" element={
            <Suspense fallback={<PageLoader />}>
              <ProtectedRoute>
                <UsersPage />
              </ProtectedRoute>
            </Suspense>
          } />

          <Route path="users/new" element={
            <Suspense fallback={<PageLoader />}>
              <ProtectedRoute>
                <UserFormPage />
              </ProtectedRoute>
            </Suspense>
          } />

          <Route path="users/:id" element={
            <Suspense fallback={<PageLoader />}>
              <ProtectedRoute>
                <UserDetailPage />
              </ProtectedRoute>
            </Suspense>
          } />

          <Route path="users/:id/edit" element={
            <Suspense fallback={<PageLoader />}>
              <ProtectedRoute>
                <UserFormPage />
              </ProtectedRoute>
            </Suspense>
          } />

          {/* Routes des rôles */}
          <Route path="roles" element={
            <Suspense fallback={<PageLoader />}>
              <ProtectedRoute>
                <RolesPage />
              </ProtectedRoute>
            </Suspense>
          } />

          <Route path="roles/new" element={
            <Suspense fallback={<PageLoader />}>
              <ProtectedRoute>
                <RoleFormPage />
              </ProtectedRoute>
            </Suspense>
          } />

          <Route path="roles/:id" element={
            <Suspense fallback={<PageLoader />}>
              <ProtectedRoute>
                <RoleDetailPage />
              </ProtectedRoute>
            </Suspense>
          } />

          <Route path="roles/:id/edit" element={
            <Suspense fallback={<PageLoader />}>
              <ProtectedRoute>
                <RoleFormPage />
              </ProtectedRoute>
            </Suspense>
          } />

          {/* Route 404 */}
          <Route path="*" element={
            <Suspense fallback={<PageLoader />}>
              <NotFoundPage />
            </Suspense>
          } />
        </Route>
      </Routes>
  );
};

export default AppRoutes;