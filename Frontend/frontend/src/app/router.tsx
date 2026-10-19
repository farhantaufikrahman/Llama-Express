import { lazy } from "react";
import { createBrowserRouter } from "react-router";
import { GuestOnly, RequireRole, RoleRedirect } from "../features/auth/guards";
import { MainLayout } from "../layouts/MainLayout";
import { AdminLayout } from "../layouts/AdminLayout";
import { RouteErrorPage } from "./RouteErrorPage";

// Code splitting per halaman: kode (dan library seperti Leaflet) baru diunduh saat halamannya dibuka
const LoginPage = lazy(() => import("../features/auth/pages/LoginPage"));
const RegisterPage = lazy(() => import("../features/auth/pages/RegisterPage"));
const AccountPage = lazy(() => import("../features/account/pages/AccountPage"));
const AddressesPage = lazy(
  () => import("../features/addresses/pages/AddressesPage"),
);
const MyOrdersPage = lazy(
  () => import("../features/orders/pages/MyOrdersPage"),
);
const NewOrderPage = lazy(
  () => import("../features/orders/pages/NewOrderPage"),
);
const OrderDetailPage = lazy(
  () => import("../features/orders/pages/OrderDetailPage"),
);
const CourierHomePage = lazy(
  () => import("../features/courier/pages/CourierHomePage"),
);
const AdminDashboardPage = lazy(
  () => import("../features/admin/pages/AdminDashboardPage"),
);
const NotFoundPage = lazy(() => import("./NotFoundPage"));
const PublicTrackingPage = lazy(
  () => import("../features/tracking/pages/PublicTrackingPage"),
);
const CourierJobPage = lazy(
  () => import("../features/courier/pages/CourierJobPage"),
);
const CourierEarningsPage = lazy(
  () => import("../features/courier/pages/CourierEarningsPage"),
);
const AdminOrdersPage = lazy(
  () => import("../features/admin/pages/AdminOrdersPage"),
);
const AdminOrderDetailPage = lazy(
  () => import("../features/admin/pages/AdminOrderDetailPage"),
);
const AdminCouriersPage = lazy(
  () => import("../features/admin/pages/AdminCouriersPage"),
);
const AdminTariffsPage = lazy(
  () => import("../features/admin/pages/AdminTariffsPage"),
);
const AdminUsersPage = lazy(
  () => import("../features/admin/pages/AdminUsersPage"),
);

export const router = createBrowserRouter([
  {
    // Satu induk untuk semua route → error apa pun di halaman mana pun ditangkap RouteErrorPage
    errorElement: <RouteErrorPage />,
    children: [
      { path: "/", element: <RoleRedirect /> },
      { path: "/track/:token", element: <PublicTrackingPage /> },

      // ---------- Publik (hanya untuk yang BELUM login) ----------
      {
        element: <GuestOnly />,
        children: [
          { path: "/login", element: <LoginPage /> },
          { path: "/register", element: <RegisterPage mode="customer" /> },
          {
            path: "/register/courier",
            element: <RegisterPage mode="courier" />,
          },
        ],
      },

      // ---------- Customer & kurir ----------
      {
        element: <RequireRole roles={["CUSTOMER", "COURIER"]} />,
        children: [
          {
            element: <MainLayout />,
            children: [{ path: "/account", element: <AccountPage /> }],
          },
        ],
      },

      // ---------- Customer ----------
      {
        element: <RequireRole roles={["CUSTOMER"]} />,
        children: [
          {
            element: <MainLayout />,
            children: [
              { path: "/orders", element: <MyOrdersPage /> },
              { path: "/orders/new", element: <NewOrderPage /> }, // path statis menang atas :code
              { path: "/orders/:code", element: <OrderDetailPage /> },
              { path: "/addresses", element: <AddressesPage /> },
            ],
          },
        ],
      },

      // ---------- Kurir ----------
      {
        element: <RequireRole roles={["COURIER"]} />,
        children: [
          { path: "/courier", element: <CourierHomePage /> },
          { path: "/courier/job", element: <CourierJobPage /> },
          { path: "/courier/earnings", element: <CourierEarningsPage /> },
        ],
      },

      // ---------- Admin ----------
      {
        element: <RequireRole roles={["ADMIN"]} />,
        children: [
          {
            element: <AdminLayout />,
            children: [
              { path: "/admin", element: <AdminDashboardPage /> },
              { path: "/admin/orders", element: <AdminOrdersPage /> },
              {
                path: "/admin/orders/:code",
                element: <AdminOrderDetailPage />,
              },
              { path: "/admin/couriers", element: <AdminCouriersPage /> },
              { path: "/admin/tariffs", element: <AdminTariffsPage /> },
              { path: "/admin/users", element: <AdminUsersPage /> },
            ],
          },
        ],
      },

      { path: "*", element: <NotFoundPage /> },
    ],
  },
]);
