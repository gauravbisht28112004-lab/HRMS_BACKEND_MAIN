import { HierarchyDashboard } from '@/features/dashboard/components';

/**
 * Menu entry for the target-flow view. Renders the same unified
 * HierarchyDashboard used as the home page for each supervisor level, so a
 * Manager / Team Leader / ATL (and Admin, viewing the Managers) has a
 * dedicated "My Team & Targets" destination in the sidebar.
 */
export const HierarchyTargetsPage = () => <HierarchyDashboard />;
