import { Users } from "lucide-react";
import { UserAvatar } from "@/components/common/UserAvatar";
import { cn } from "@/lib/utils";
import type { ConversationRow } from "../types";

type Props = {
  conversation: Pick<ConversationRow, "id" | "type" | "title" | "other">;
  size?: "md" | "lg" | "xl";
  className?: string;
};

export function ConversationAvatar({ conversation: c, size = "lg", className }: Props) {
  if (c.type === "GROUP") {
    return (
      <span
        className={cn(
          "flex shrink-0 items-center justify-center rounded-full bg-accent text-accent-foreground",
          size === "xl" ? "size-20" : size === "lg" ? "size-10" : "size-8",
          className,
        )}
        aria-hidden
      >
        <Users className={size === "xl" ? "size-9" : "size-5"} />
      </span>
    );
  }
  const userId = c.type === "DIRECT" && c.other ? c.other.id : c.id;
  const name = c.type === "DIRECT" && c.other ? c.other.fullName : c.title;
  return (
    <UserAvatar
      userId={userId}
      name={name}
      size={size === "md" ? "md" : "lg"}
      className={cn(size === "xl" && "size-20 text-xl", className)}
    />
  );
}
