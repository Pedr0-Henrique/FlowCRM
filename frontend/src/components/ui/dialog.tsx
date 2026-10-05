"use client";

import type { ReactNode } from "react";
import { X } from "lucide-react";
import { Button } from "@/components/ui/button";

export function Dialog({
  title,
  onClose,
  children,
}: {
  title: string;
  onClose: () => void;
  children: ReactNode;
}) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto bg-black/50 p-4">
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby="dialog-title"
        className="my-auto w-full max-w-xl rounded-xl border border-border bg-card p-6 shadow-xl"
      >
        <div className="mb-6 flex items-center justify-between">
          <h2 id="dialog-title" className="text-xl font-semibold text-foreground">
            {title}
          </h2>
          <Button type="button" variant="ghost" size="icon" onClick={onClose} aria-label="Fechar">
            <X className="h-4 w-4" />
          </Button>
        </div>
        {children}
      </section>
    </div>
  );
}
