import { lazy } from 'react';
import { createBrowserRouter } from 'react-router';
import { GuestOnly, RequireRole, RoleRedirect } from '../features/auth/guards';
import { MainLayout } from '../layouts/MainLayout';
import { AdminLayout } from '../layouts/AdminLayout';

// Code splitting per area: kurir tidak perlu mengunduh kode halaman admin, dan sebaliknya
const LoginPage = lazy(() => import('../features/auth/pages/LoginPage'));
const MyOrdersPage = lazy(() => import('../features/orders/pages/MyOrdersPage'));
const CourierHomePage = lazy(() => import('../features/courier/pages/CourierHomePage'));
const AdminDashboardPage = lazy(() => import('../features/admin/pages/AdminDashboardPage'));
const NotFoundPage = lazy(() => import('./NotFoundPage'));

export const router = createBrowserRouter([
  { path: '/', element: <RoleRedirect /> },
  {
    element: <GuestOnly />,
    children: [{ path: '/login', element: <LoginPage /> }],
  },
  {
    element: <RequireRole roles={['CUSTOMER']} />,
    children: [{ element: <MainLayout />, children: [{ path: '/orders', element: <MyOrdersPage /> }] }],
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