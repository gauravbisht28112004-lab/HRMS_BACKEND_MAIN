import { useState } from 'react';
import clsx from 'clsx';
import { getInitials } from '@/utils/format';

export type AvatarSize = 'xs' | 'sm' | 'md' | 'lg' | 'xl';

interface AvatarProps {
  /** Display name — used for the initials fallback and the alt text. */
  name: string;
  /**
   * URL to the profile picture. Usually a presigned S3 URL coming back from
   * the backend; may be a legacy public URL for older employees. Undefined /
   * empty string means "no picture — show initials."
   */
  src?: string | null;
  size?: AvatarSize;
  /** Extra classes to apply to the outer element (for borders, rings, etc.). */
  className?: string;
}

/**
 * Circular employee avatar. Renders the image if `src` is set and loads
 * successfully, otherwise falls back to the employee's initials inside a
 * brand-coloured circle (same style the Topbar has always used).
 *
 * The image is given `onError={hide}` so a broken/expired presigned URL
 * gracefully degrades back to initials instead of showing the browser's
 * default broken-image icon.
 */
export const Avatar = ({ name, src, size = 'md', className }: AvatarProps) => {
  const [errored, setErrored] = useState(false);

  const sizeClasses: Record<AvatarSize, string> = {
    xs: 'h-6 w-6 text-[10px]',
    sm: 'h-8 w-8 text-xs',
    md: 'h-10 w-10 text-sm',
    lg: 'h-16 w-16 text-lg',
    xl: 'h-24 w-24 text-2xl',
  };

  const showImage = Boolean(src) && !errored;

  return (
    <div
      className={clsx(
        'inline-flex shrink-0 items-center justify-center overflow-hidden rounded-full bg-brand-700 font-semibold text-white',
        sizeClasses[size],
        className,
      )}
      aria-label={name}
    >
      {showImage ? (
        <img
          src={src ?? ''}
          alt={name}
          className="h-full w-full object-cover"
          onError={() => setErrored(true)}
          referrerPolicy="no-referrer"
          draggable={false}
        />
      ) : (
        <span>{getInitials(name || '??')}</span>
      )}
    </div>
  );
};
