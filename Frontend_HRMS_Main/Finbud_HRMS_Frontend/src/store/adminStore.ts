import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface AdminState {
  monthlyTargetAmount: number;
  updateMonthlyTargetAmount: (amount: number) => void;
}

export const useAdminStore = create<AdminState>()(
  persist(
    (set) => ({
      monthlyTargetAmount: 2500000,
      updateMonthlyTargetAmount: (amount) =>
        set({
          monthlyTargetAmount: amount,
        }),
    }),
    {
      name: 'finbud-admin-settings',
    },
  ),
);
