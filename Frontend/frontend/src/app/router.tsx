import { lazy } from 'react';
import { createBrowserRouter } from 'react-router';
import { GuestOnly, RequireRole, RoleRedirect } from '../features/auth/guards';
import { MainLayout } from '../layouts/MainLayout';
import { AdminLayout } from '../layouts/AdminLayout';

const LoginPage = lazy(() => import('../features/auth/pages/LoginPage'));
const RegisterPage = lazy(() => import('../features/auth/pages/RegisterPage'));
const AccountPage = lazy(() => import('../features/account/pages/AccountPage'));
const AddressesPage = lazy(() => import('../features/addresses/pages/AddressesPage'));
const MyOrdersPage = lazy(() => import('../features/orders/pages/MyOrdersPage'));
const CourierHomePage = lazy(() => import('../features/courier/pages/CourierHomePage'));
const AdminDashboardPage = lazy(() => import('../features/admin/pages/AdminDashboardPage'));
const NotFoundPage = lazy(() => import('./NotFoundPage'));

export const router = createBrowserRouter([
  { path: '/', element: <RoleRedirect /> },
  {
    element: <GuestOnly />,
    children: [
      { path: '/login', element: <LoginPage /> },
      { path: '/register', element: <RegisterPage mode="customer" /> },
      { path: '/register/courier', element: <RegisterPage mode="courier" /> },
    ],
  },
  {
    element: <RequireRole roles={['CUSTOMER', 'COURIER']} />,
    children: [{ element: <MainLayout />, children: [{ path: '/account', element: <AccountPage /> }] }],
  },
  {
    element: <RequireRole roles={['CUSTOMER']} />,
    children: [{
      element: <MainLayout />,
      children: [
        { path: '/orders', element: <MyOrdersPage /> },
        { path: '/addresses', element: <AddressesPage /> },
      ],
    }],
  },
  {
    element: <RequireRole roles={['COURIER']} />,
    children: [{ element: <MainLayout />, children: [{ path: '/courier', element: <CourierHomePage /> }] }],
  },
  {
    element: <RequireRole roles={['ADMIN']} />,
    children: [{ element: <AdminLayout />, children: [{ path: '/admin', element: <AdminDashboardPage /> }] }],
  },
  { path: '*', element: <NotFoundPage /> },
]);