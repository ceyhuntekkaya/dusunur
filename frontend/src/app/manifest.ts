import type { MetadataRoute } from "next";

export default function manifest(): MetadataRoute.Manifest {
  const institution = process.env.NEXT_PUBLIC_INSTITUTION_NAME ?? "Okul";
  return {
    name: `${institution} İletişim`,
    short_name: "İletişim",
    start_url: "/akis",
    display: "standalone",
    background_color: "#F5F7FA",
    theme_color: "#2453C7",
    lang: "tr",
    icons: [
      { src: "/icons/icon-192.png", sizes: "192x192", type: "image/png" },
      { src: "/icons/icon-512.png", sizes: "512x512", type: "image/png" },
      { src: "/icons/icon-512-maskable.png", sizes: "512x512", type: "image/png", purpose: "maskable" },
    ],
  };
}
