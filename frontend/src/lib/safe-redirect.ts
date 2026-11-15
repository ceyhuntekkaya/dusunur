export function safeInternalPath(next: string | null | undefined): string | null {
  if (!next || !next.startsWith("/") || next.includes("\\") || /[\x00-\x1f]/.test(next)) return null;
  try {
    return new URL(next, window.location.origin).origin === window.location.origin ? next : null;
  } catch {
    return null;
  }
}
