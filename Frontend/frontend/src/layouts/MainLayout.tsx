import { Suspense } from 'react';
import { Outlet } from 'react-router';
import { FullPageSpinner } from '../components/FullPageSpinner';
import { AppHeader } from './AppHeader';

export function MainLayout() {
  return (
    <>
      <AppHeader />
      <main className="mx-auto max-w-5xl px-4 py-6">
        <Suspense fallback={<FullPageSpinner />}>
          <Outlet />
        </Suspense>
      </main>
    </>
  );
}