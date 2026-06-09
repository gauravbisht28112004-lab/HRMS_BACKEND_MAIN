import { useEffect } from 'react';
import { AppRouter } from '@/routes/AppRouter';
import { useAuthStore } from '@/store/authStore';
import { AUTH_EXPIRED_EVENT } from '@/services/tokenStorage';

function App() {
  const initializeSession = useAuthStore((state) => state.initializeSession);
  const clearSession = useAuthStore((state) => state.clearSession);

  useEffect(() => {
    void initializeSession();
  }, [initializeSession]);

  useEffect(() => {
    const handleAuthExpired = () => {
      clearSession();
    };

    window.addEventListener(AUTH_EXPIRED_EVENT, handleAuthExpired);
    return () => window.removeEventListener(AUTH_EXPIRED_EVENT, handleAuthExpired);
  }, [clearSession]);

  return <AppRouter />;
}

export default App;
