import { MutationCache, QueryCache, QueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { ApiError } from "@/lib/api";

export const SESSION_ENDING_CODES = [
  "UNAUTHENTICATED",
  "AUTH_ACCOUNT_PASSIVE",
  "AUTH_NO_SEASON_MEMBERSHIP",
  "AUTH_NO_ACTIVE_SEASON",
];

const AUTH_PAGES = ["/giris", "/sifremi-unuttum", "/sifre-belirle"];

export function toastError(message: string, details: string[] = []) {
  toast.error(message, {
    duration: 6000,
    description: details.length > 0 ? details.slice(0, 5).join("\n") : undefined,
    classNames: { description: "whitespace-pre-line" },
  });
}

function handleError(error: unknown, silent: boolean) {
  if (!(error instanceof ApiError)) {
    if (!silent) toastError("Beklenmeyen bir hata oluştu. Lütfen tekrar deneyin.");
    return;
  }

  if (SESSION_ENDING_CODES.includes(error.code)) {
    if (typeof window === "undefined") return;
    const path = window.location.pathname;
    const onAuthPage = path === "/" || AUTH_PAGES.some((p) => path.startsWith(p));
    if (onAuthPage) return;
    queryClient.clear();
    const params = new URLSearchParams({ neden: error.code });
    if (error.code === "UNAUTHENTICATED") {
      params.set("next", window.location.pathname + window.location.search);
    }
    // eslint-disable-next-line @next/next/no-location-assign-relative-destination
    window.location.assign(`/giris?${params.toString()}`);
    return;
  }

  if (error.code === "KVKK_REQUIRED") {
    void queryClient.invalidateQueries({ queryKey: ["me"] });
    return;
  }

  if (!silent) toastError(error.message, error.details);
}

export const queryClient: QueryClient = new QueryClient({
  queryCache: new QueryCache({
    onError: (error, query) => handleError(error, query.meta?.silentError === true),
  }),
  mutationCache: new MutationCache({
    onError: (error, _variables, _context, mutation) =>
      handleError(error, mutation.meta?.silentError === true),
  }),
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      refetchOnWindowFocus: true,
      retry: (failureCount, error) => {
        if (error instanceof ApiError && error.status >= 400 && error.status < 500) return false;
        return failureCount < 1;
      },
    },
  },
});
