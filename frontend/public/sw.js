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
    self.clients.matchAll({ type: "window", includeUncontrolled: true }).then((windows) => {
      if (windows.some((w) => w.focused)) return;
      return self.registration.showNotification(title, {
        body: data.body || "",
        tag: data.tag || undefined,
        data: { link: data.link || "/akis" },
        icon: "/icons/icon-192.png",
        badge: "/icons/badge-72.png",
      });
    }),
  );
});

self.addEventListener("notificationclick", (event) => {
  event.notification.close();
  const link = (event.notification.data && event.notification.data.link) || "/akis";
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
