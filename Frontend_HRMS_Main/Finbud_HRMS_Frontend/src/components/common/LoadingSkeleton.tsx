export const LoadingSkeleton = () => (
  <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
    {Array.from({ length: 4 }).map((_, index) => (
      <div key={index} className="h-36 animate-pulse rounded-3xl bg-white/70 shadow-panel" />
    ))}
  </div>
);
