import { Avatar, AvatarFallback } from "@/components/ui/avatar";

export const PALETTE = [
  ["#DBEAFE", "#1E40AF"],
  ["#DCFCE7", "#166534"],
  ["#FCE7F3", "#9D174D"],
  ["#FEF3C7", "#92400E"],
  ["#E0E7FF", "#3730A3"],
  ["#CCFBF1", "#115E59"],
  ["#FFE4E6", "#9F1239"],
  ["#EDE9FE", "#5B21B6"],
] as const;

const SIZES = { sm: "sm", md: "default", lg: "lg" } as const;

function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return "?";
  const first = parts[0][0];
  const last = parts.length > 1 ? parts[parts.length - 1][0] : "";
  return (first + last).toLocaleUpperCase("tr");
}

type Props = {
  userId: number;
  name: string;
  size?: keyof typeof SIZES;
  className?: string;
};

export function UserAvatar({ userId, name, size = "md", className }: Props) {
  const [bg, fg] = PALETTE[Math.abs(userId) % PALETTE.length];
  return (
    <Avatar size={SIZES[size]} className={className} aria-label={name}>
      <AvatarFallback
        style={{ backgroundColor: bg, color: fg }}
        className="text-xs font-semibold"
      >
        {initials(name)}
      </AvatarFallback>
    </Avatar>
  );
}
