self.addEventListener("install", () => {
  self.skipWaiting();
});

self.addEventListener("activate", (event) => {
  event.waitUntil(self.clients.claim());
});

self.addEventListener("push", (event) => {
  let data = {};
  try {
    data = event.data ? event.data.json() : {};
  } catch {
    data = {};
  }
  const title = data.title || "İletişim";
  event.waitUntil(
    self.registration.showNotification(title, {
      body: data.body || "",
      tag: data.tag || undefined,
      data: { link: data.link || "/akis" },
      icon: "/icons/icon-192.png",
      badge: "/icons/badge-72.png",
    }),
  );
});

self.addEventListener("notificationclick", (event) => {
  event.notification.close();
  const raw = (event.notification.data && event.notification.data.link) || "";
  let link = "/akis";
  if (raw.startsWith("/") && !raw.includes("\\") && !/[\x00-\x1f]/.test(raw)) {
    try {
      if (new URL(raw, self.location.origin).origin === self.location.origin) link = raw;
    } catch {}
  }
  event.waitUntil(
    self.clients.matchAll({ type: "window", includeUncontrolled: true }).then((windows) => {
      const open = windows[0];
      if (open) {
        return open.focus().then(() => open.navigate(link));
      }
      return self.clients.openWindow(link);
    }),
  );
});
