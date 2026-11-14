"use client";

import { useEffect, useRef, useSyncExternalStore } from "react";
import { Client } from "@stomp/stompjs";
import type { WsEnvelope } from "@/features/core/types";

type Handler = (payload: unknown) => void;

const BANNER_DELAY_MS = 5000;

let client: Client | null = null;
let connected = false;
let showBanner = false;
let everConnected = false;
let bannerTimer: ReturnType<typeof setTimeout> | null = null;

const handlers = new Map<string, Set<Handler>>();
const reconnectHandlers = new Set<() => void>();
const stateListeners = new Set<() => void>();

function emitState() {
  stateListeners.forEach((l) => l());
}

function wsUrl(): string {
  const env = process.env.NEXT_PUBLIC_WS_URL;
  if (env) return env;
  const proto = window.location.protocol === "https:" ? "wss:" : "ws:";
  return `${proto}//${window.location.host}/ws`;
}

function dispatch(env: WsEnvelope) {
  handlers.get(env.type)?.forEach((h) => h(env.payload));
}

function setDisconnected() {
  connected = false;
  if (bannerTimer === null) {
    bannerTimer = setTimeout(() => {
      bannerTimer = null;
      if (!connected) {
        showBanner = true;
        emitState();
      }
    }, BANNER_DELAY_MS);
  }
}

export function connectWs() {
  if (client || typeof window === "undefined") return;
  const c = new Client({
    brokerURL: wsUrl(),
    reconnectDelay: 3000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect: () => {
      const wasDown = everConnected;
      everConnected = true;
      connected = true;
      if (bannerTimer !== null) {
        clearTimeout(bannerTimer);
        bannerTimer = null;
      }
      const hadBanner = showBanner;
      showBanner = false;
      c.subscribe("/user/queue/events", (msg) => {
        try {
          dispatch(JSON.parse(msg.body) as WsEnvelope);
        } catch {}
      });
      if (hadBanner) emitState();
      if (wasDown) reconnectHandlers.forEach((h) => h());
    },
    onWebSocketClose: setDisconnected,
    onWebSocketError: setDisconnected,
  });
  client = c;
  setDisconnected();
  c.activate();
}

export function disconnectWs() {
  if (!client) return;
  void client.deactivate();
  client = null;
  connected = false;
  everConnected = false;
  showBanner = false;
  if (bannerTimer !== null) {
    clearTimeout(bannerTimer);
    bannerTimer = null;
  }
  emitState();
}

export function subscribeWsEvent(type: string, handler: Handler): () => void {
  let set = handlers.get(type);
  if (!set) {
    set = new Set();
    handlers.set(type, set);
  }
  set.add(handler);
  return () => {
    set.delete(handler);
  };
}

export function onWsReconnect(handler: () => void): () => void {
  reconnectHandlers.add(handler);
  return () => {
    reconnectHandlers.delete(handler);
  };
}

export function useWsEvent<T = unknown>(type: string, handler: (payload: T) => void) {
  const ref = useRef(handler);
  useEffect(() => {
    ref.current = handler;
  });
  useEffect(() => subscribeWsEvent(type, (p) => ref.current(p as T)), [type]);
}

export function useWsReconnect(handler: () => void) {
  const ref = useRef(handler);
  useEffect(() => {
    ref.current = handler;
  });
  useEffect(() => onWsReconnect(() => ref.current()), []);
}

export function useWsConnectionLost(): boolean {
  return useSyncExternalStore(
    (cb) => {
      stateListeners.add(cb);
      return () => {
        stateListeners.delete(cb);
      };
    },
    () => showBanner,
    () => false,
  );
}
