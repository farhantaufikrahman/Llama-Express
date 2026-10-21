import { render } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  createMemoryRouter,
  RouterProvider,
  type RouteObject,
} from "react-router";
import {
  AuthContext,
  type AuthContextValue,
} from "../features/auth/AuthContext";

/** Alamat awal: string biasa, atau objek dengan state (mis. { from: '/admin' } seperti yang dikirim guard) */
type StartUrl = string | { pathname: string; state?: unknown };

/**
 * Render dengan router + React Query + (opsional) auth.
 * QueryClient BARU per test → cache tidak bocor antar test.
 */
export function renderApp({
  routes,
  url,
  auth,
}: {
  routes: RouteObject[];
  url: StartUrl;
  auth?: AuthContextValue;
}) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: Infinity },
      mutations: { retry: false },
    },
  });
  const router = createMemoryRouter(routes, { initialEntries: [url] });
  const tree = (
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  );
  const user = userEvent.setup();
  render(
    auth ? (
      <AuthContext.Provider value={auth}>{tree}</AuthContext.Provider>
    ) : (
      tree
    ),
  );
  return { user, router, queryClient };
}
