import type { MetadataRoute } from "next";

export default function manifest(): MetadataRoute.Manifest {
  const institution = process.env.NEXT_PUBLIC_INSTITUTION_NAME ?? "Okul";
  return {
    name: `${institution} İletişim`,
    short_name: "İletişim",
    start_url: "/akis",
    display: "standalone",
    background_color: "#F4F2EE",
    theme_color: "#FFFFFF",
    lang: "tr",
    icons: [
      { src: "/icons/icon-192.png", sizes: "192x192", type: "image/png" },
      { src: "/icons/icon-512.png", sizes: "512x512", type: "image/png" },
      { src: "/icons/icon-512-maskable.png", sizes: "512x512", type: "image/png", purpose: "maskable" },
    ],
  };
}
