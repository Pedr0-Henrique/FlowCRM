"use client";

import { useEffect, useSyncExternalStore } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import {
  Activity,
  Building2,
  BarChart3,
  LayoutDashboard,
  Settings,
  Target,
  Users,
  UserPlus,
  Wallet,
  CheckSquare,
  LogOut,
} from "lucide-react";
import { ThemeToggle } from "@/components/theme-toggle";
import { Button } from "@/components/ui/button";
import { clearAuth, getAccessToken, getUser, type AuthResponse } from "@/lib/auth";
import type { ReactNode } from "react";

const nav = [
  { href: "/dashboard", label: "Dashboard", icon: LayoutDashboard, roles: ["ADMIN", "MANAGER", "SALES"] },
  { href: "/leads", label: "Leads", icon: Target, roles: ["ADMIN", "MANAGER", "SALES"] },
  { href: "/clients", label: "Clientes", icon: Users, roles: ["ADMIN", "MANAGER", "SALES"] },
  { href: "/companies", label: "Empresas", icon: Building2, roles: ["ADMIN"] },
  { href: "/opportunities", label: "Oportunidades", icon: Wallet, roles: ["ADMIN", "MANAGER", "SALES"] },
  { href: "/tasks", label: "Tarefas", icon: CheckSquare, roles: ["ADMIN", "MANAGER", "SALES", "USER"] },
  { href: "/activities", label: "Atividades", icon: Activity, roles: ["ADMIN", "MANAGER", "SALES", "USER"] },
  { href: "/reports", label: "Relatórios", icon: BarChart3, roles: ["ADMIN", "MANAGER", "SALES"] },
  { href: "/users", label: "Usuários", icon: UserPlus, roles: ["ADMIN", "MANAGER"] },
  { href: "/settings", label: "Configurações", icon: Settings },
];

function subscribeToAuth(callback: () => void) {
  window.addEventListener("storage", callback);
  return () => window.removeEventListener("storage", callback);
}

function getAuthSnapshot() {
  return Boolean(getAccessToken());
}

function getServerAuthSnapshot(): boolean | null {
  return null;
}

export default function DashboardLayout({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const authenticated = useSyncExternalStore(
    subscribeToAuth,
    getAuthSnapshot,
    getServerAuthSnapshot,
  );
  const user: AuthResponse | null = authenticated ? getUser() : null;

  useEffect(() => {
    if (authenticated === false) {
      clearAuth();
      router.replace("/login");
    }
  }, [authenticated, router]);

  const handleLogout = () => {
    clearAuth();
    router.replace("/login");
  };

  if (authenticated === null || !authenticated) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <p className="text-muted-foreground">Verificando sessão...</p>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen">
      <aside className="hidden w-60 shrink-0 border-r border-border bg-sidebar md:flex md:flex-col">
        <div className="flex h-16 items-center px-6 text-lg font-semibold tracking-tight">FlowCRM</div>
        <nav className="flex flex-1 flex-col gap-1 px-3 py-2">
          {nav.filter((item) => !item.roles || item.roles.includes(user?.role ?? "")).map((item) => {
            const isActive = pathname === item.href || pathname.startsWith(item.href + "/");
            return (
              <Link
                key={item.href}
                href={item.href}
                className={`flex items-center gap-2 rounded-lg px-3 py-2 text-sm transition-colors ${
                  isActive
                    ? "bg-accent text-accent-foreground"
                    : "text-foreground/80 hover:bg-muted hover:text-foreground"
                }`}
              >
                <item.icon className="h-4 w-4" />
                {item.label}
              </Link>
            );
          })}
        </nav>
      </aside>
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex h-16 items-center justify-between border-b border-border bg-card px-4 md:px-8">
          <p className="text-sm text-muted-foreground">Clean Modern SaaS Dashboard</p>
          <div className="flex items-center gap-4">
            {user && (
              <div className="hidden md:flex items-center gap-2 text-sm">
                <span className="text-muted-foreground">{user.name}</span>
                <span className="text-muted-foreground/50">•</span>
                <span className="text-xs bg-muted px-2 py-1 rounded">{user.role}</span>
              </div>
            )}
            <ThemeToggle />
            <Button variant="ghost" size="icon" onClick={handleLogout} title="Sair">
              <LogOut className="h-4 w-4" />
            </Button>
          </div>
        </header>
        <main className="flex-1 p-4 md:p-8">{children}</main>
      </div>
    </div>
  );
}
