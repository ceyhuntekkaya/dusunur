"use client";

import { QueryClientProvider } from "@tanstack/react-query";
import { TooltipProvider } from "@/components/ui/tooltip";
import { Toaster } from "@/components/ui/sonner";
import { queryClient } from "@/lib/query-client";
import { useIsDesktop } from "@/lib/media";

function AppToaster() {
  const desktop = useIsDesktop();
  return (
    <Toaster
      theme="light"
      richColors
      closeButton
      duration={3000}
      position={desktop ? "bottom-right" : "top-center"}
    />
  );
}

export function Providers({ children }: { children: React.ReactNode }) {
  return (
    <QueryClientProvider client={queryClient}>
      <TooltipProvider>
        {children}
        <AppToaster />
      </TooltipProvider>
    </QueryClientProvider>
  );
}
