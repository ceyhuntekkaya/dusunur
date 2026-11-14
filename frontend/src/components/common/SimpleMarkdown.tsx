import { Fragment } from "react";

type Block =
  | { kind: "heading"; level: 1 | 2 | 3; text: string }
  | { kind: "paragraph"; text: string }
  | { kind: "list"; ordered: boolean; items: string[] };

function parse(source: string): Block[] {
  const blocks: Block[] = [];
  let paragraph: string[] = [];
  const flush = () => {
    if (paragraph.length) blocks.push({ kind: "paragraph", text: paragraph.join(" ") });
    paragraph = [];
  };

  for (const raw of source.replace(/\r\n/g, "\n").split("\n")) {
    const line = raw.trim();
    const heading = /^(#{1,3})\s+(.*)$/.exec(line);
    const bullet = /^[-*]\s+(.*)$/.exec(line);
    const numbered = /^\d+[.)]\s+(.*)$/.exec(line);
    const last = blocks[blocks.length - 1];

    if (!line) {
      flush();
    } else if (heading) {
      flush();
      blocks.push({ kind: "heading", level: heading[1].length as 1 | 2 | 3, text: heading[2] });
    } else if (bullet || numbered) {
      flush();
      const ordered = !!numbered;
      const text = (bullet ?? numbered)![1];
      if (last?.kind === "list" && last.ordered === ordered) last.items.push(text);
      else blocks.push({ kind: "list", ordered, items: [text] });
    } else {
      paragraph.push(line);
    }
  }
  flush();
  return blocks;
}

export function SimpleMarkdown({ source }: { source: string }) {
  return (
    <div className="space-y-3 text-[15px] leading-relaxed">
      {parse(source).map((b, i) => (
        <Fragment key={i}>
          {b.kind === "heading" && (
            <p className={b.level === 1 ? "text-lg font-semibold" : "text-base font-semibold"}>{b.text}</p>
          )}
          {b.kind === "paragraph" && <p>{b.text}</p>}
          {b.kind === "list" &&
            (b.ordered ? (
              <ol className="list-decimal space-y-1 pl-5">
                {b.items.map((t, j) => (
                  <li key={j}>{t}</li>
                ))}
              </ol>
            ) : (
              <ul className="list-disc space-y-1 pl-5">
                {b.items.map((t, j) => (
                  <li key={j}>{t}</li>
                ))}
              </ul>
            ))}
        </Fragment>
      ))}
    </div>
  );
}
