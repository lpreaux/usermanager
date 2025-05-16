import { Outlet } from 'react-router-dom';
import Navbar from './Navbar';
import Footer from './Footer';
import { Toaster } from 'react-hot-toast';

const MainLayout = () => {
    return (
        <div className="flex flex-col min-h-screen">
            <Navbar />
            <main className="flex-grow">
                <div className="container-content">
                    <Outlet />
                </div>
            </main>
            <Footer />
            <Toaster position="top-right" />
        </div>
    );
};

export default MainLayout;
