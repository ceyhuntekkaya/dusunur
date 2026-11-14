export function DetailList({ items }: { items: string[] }) {
  if (items.length === 0) return null;
  return (
    <ul
      className="max-h-48 list-disc space-y-1 overflow-y-auto rounded-lg bg-destructive/10 py-3 pr-3 pl-7 text-[13px] text-destructive"
      role="alert"
    >
      {items.map((m, i) => (
        <li key={i}>{m}</li>
      ))}
    </ul>
  );
}
