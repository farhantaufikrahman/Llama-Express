import { Suspense } from "react";
import { Outlet } from "react-router";
import { FullPageSpinner } from "../components/FullPageSpinner";
import { useAuth } from "../features/auth/AuthContext";
import { CourierRuntime } from "../features/courier/CourierRuntime";
import { AppHeader } from "./AppHeader";

export function MainLayout() {
  const { user } = useAuth();
  const content = (
    <Suspense fallback={<FullPageSpinner />}>
      <Outlet />
    </Suspense>
  );
  return (
    <>
      <AppHeader />
      <main className="mx-auto max-w-5xl px-4 py-6">
        {user?.role === "COURIER" ? (
          <CourierRuntime>{content}</CourierRuntime>
        ) : (
          content
        )}
      </main>
    </>
  );
}
