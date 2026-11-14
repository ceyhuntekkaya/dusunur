export type FieldError = { field: string; message: string };

export class ApiError extends Error {
  constructor(
    public status: number,
    public code: string,
    message: string,
    public fieldErrors: FieldError[] = [],
    public details: string[] = [],
    public traceId?: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export type PageResponse<T> = { items: T[]; page: number; size: number; total: number };
export type CursorResponse<T> = { items: T[]; nextCursor: number | null };

const MAX_UPLOAD_MB = 100;

function readCookie(name: string): string | undefined {
  if (typeof document === "undefined") return undefined;
  const prefix = `${name}=`;
  const part = document.cookie.split("; ").find((c) => c.startsWith(prefix));
  return part ? decodeURIComponent(part.slice(prefix.length)) : undefined;
}

async function rawFetch(path: string, init: RequestInit): Promise<Response> {
  try {
    return await fetch(path, { credentials: "include", ...init });
  } catch {
    throw new ApiError(
      0,
      "NETWORK_ERROR",
      "Sunucuya ulaşılamıyor. İnternet bağlantınızı kontrol edip tekrar deneyin.",
    );
  }
}

async function ensureCsrfToken(): Promise<string | undefined> {
  let token = readCookie("XSRF-TOKEN");
  if (!token) {
    await rawFetch("/api/auth/csrf", { method: "GET" });
    token = readCookie("XSRF-TOKEN");
  }
  return token;
}

function fallbackMessage(status: number): string {
  if (status === 413) return `Dosya çok büyük. En fazla ${MAX_UPLOAD_MB} MB yükleyebilirsiniz.`;
  if (status === 502 || status === 503 || status === 504)
    return "Sistem şu anda yanıt vermiyor. Birkaç dakika sonra tekrar deneyin.";
  return `Beklenmeyen bir yanıt alındı (HTTP ${status}). Sayfayı yenileyip tekrar deneyin.`;
}

function isApiErrorBody(body: unknown): body is {
  code: string;
  message: string;
  fieldErrors?: FieldError[];
  details?: string[];
  traceId?: string;
} {
  return (
    typeof body === "object" &&
    body !== null &&
    typeof (body as Record<string, unknown>).code === "string" &&
    typeof (body as Record<string, unknown>).message === "string"
  );
}

export async function api<T>(
  path: string,
  init: Omit<RequestInit, "body"> & { json?: unknown; body?: BodyInit | null } = {},
): Promise<T> {
  const { json, headers: initHeaders, ...rest } = init;
  const method = (rest.method ?? "GET").toUpperCase();
  const headers = new Headers(initHeaders);
  const options: RequestInit = { ...rest, method, headers };

  if (json !== undefined) {
    headers.set("Content-Type", "application/json");
    options.body = JSON.stringify(json);
  }
  if (method !== "GET" && method !== "HEAD") {
    const token = await ensureCsrfToken();
    if (token) headers.set("X-XSRF-TOKEN", token);
  }

  const res = await rawFetch(path, options);

  if (res.status === 204) return undefined as T;

  const text = await res.text();
  let body: unknown;
  if (text) {
    try {
      body = JSON.parse(text);
    } catch {
      body = undefined;
    }
  }

  if (!res.ok) {
    if (isApiErrorBody(body)) {
      throw new ApiError(
        res.status,
        body.code,
        body.message,
        body.fieldErrors ?? [],
        body.details ?? [],
        body.traceId,
      );
    }
    throw new ApiError(res.status, "HTTP_ERROR", fallbackMessage(res.status));
  }

  return body as T;
}
