import type { ReactNode } from "react";

const URL_RE = /(https?:\/\/[^\s<]+|www\.[^\s<]+)/g;

export function linkify(text: string): ReactNode[] {
  return text.split(URL_RE).map((part, i) => {
    if (i % 2 === 0) return part;
    const href = part.startsWith("www.") ? `https://${part}` : part;
    return (
      <a
        key={i}
        href={href}
        target="_blank"
        rel="noopener noreferrer"
        className="text-primary underline-offset-2 hover:underline"
      >
        {part}
      </a>
    );
  });
}
