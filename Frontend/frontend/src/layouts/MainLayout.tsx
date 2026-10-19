import { Suspense } from "react";
import { Outlet } from "react-router";
import { FullPageSpinner } from "../components/FullPageSpinner";
import { useAuth } from "../features/auth/AuthContext";
import { CourierRuntime } from "../features/courier/CourierRuntime";
import { AppHeader } from "./AppHeader";

export function MainLayout() {
  const { user } = useAuth();

  const layout = (
    <div className="flex min-h-dvh flex-col bg-cream text-ink">
      <AppHeader />
      <main className="mx-auto w-full max-w-5xl flex-1 px-4 py-6">
        <Suspense fallback={<FullPageSpinner />}>
          <Outlet />
        </Suspense>
      </main>
    </div>
  );

  return user?.role === "COURIER" ? (
    <CourierRuntime>{layout}</CourierRuntime>
  ) : (
    layout
  );
}

export default MainLayout;
