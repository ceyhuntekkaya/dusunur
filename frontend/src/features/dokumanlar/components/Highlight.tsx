export function Highlight({ text, query }: { text: string; query: string }) {
  const lower = text.toLocaleLowerCase("tr");
  const q = query.toLocaleLowerCase("tr");
  const i = q ? lower.indexOf(q) : -1;
  if (i < 0 || lower.length !== text.length) return <>{text}</>;
  return (
    <>
      {text.slice(0, i)}
      <mark className="rounded-sm bg-yellow-200 px-0.5 text-foreground">{text.slice(i, i + q.length)}</mark>
      {text.slice(i + q.length)}
    </>
  );
}
