import { create } from 'zustand';

interface UIState {
  assistantOpen: boolean;
  toggleAssistant: () => void;
}

export const useUIStore = create<UIState>((set) => ({
  assistantOpen: true,
  toggleAssistant: () =>
    set((state) => ({
      assistantOpen: !state.assistantOpen,
    })),
}));
