import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",
  poweredByHeader: false,
  turbopack: {
    rules: {
      "*.css": {
        loaders: ["@tailwindcss/turbopack"],
        as: "*.css",
      },
    },
  },
  async rewrites() {
    return process.env.NODE_ENV === "development"
      ? [
          { source: "/api/:path*", destination: "http://localhost:8080/api/:path*" },
          { source: "/ws", destination: "http://localhost:8080/ws" },
        ]
      : [];
  },
  async headers() {
    return [{ source: "/sw.js", headers: [{ key: "Cache-Control", value: "no-cache" }] }];
  },
};

export default nextConfig;
