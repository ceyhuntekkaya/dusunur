const FOLD_MAP: Record<string, string> = {
  ı: "i",
  ğ: "g",
  ü: "u",
  ş: "s",
  ö: "o",
  ç: "c",
  â: "a",
  î: "i",
  û: "u",
};

export function fold(s: string): string {
  return s
    .toLocaleLowerCase("tr")
    .replace(/[ığüşöçâîû]/g, (c) => FOLD_MAP[c])
    .normalize("NFD")
    .replace(/\p{M}/gu, "")
    .replace(/\s+/g, " ")
    .trim();
}
