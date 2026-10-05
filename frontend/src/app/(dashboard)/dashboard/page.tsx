"use client";

import { useQuery } from "@tanstack/react-query";
import { getDashboard } from "@/lib/dashboard";
import { ApiError } from "@/lib/api";
import {
  Target,
  Users,
  Wallet,
  CheckSquare,
  TrendingUp,
  AlertCircle,
  type LucideIcon,
} from "lucide-react";
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from "recharts";

const LEAD_STATUS_LABELS: Record<string, string> = {
  NEW: "Novo",
  CONTACTED: "Contatado",
  QUALIFIED: "Qualificado",
  PROPOSAL: "Proposta",
  NEGOTIATION: "Negociação",
  WON: "Ganho",
  LOST: "Perdido",
};
const LEAD_STATUS_COLORS: Record<string, string> = {
  NEW: "#3b82f6",
  CONTACTED: "#06b6d4",
  QUALIFIED: "#8b5cf6",
  PROPOSAL: "#f59e0b",
  NEGOTIATION: "#f97316",
  WON: "#10b981",
  LOST: "#ef4444",
};

export default function DashboardPage() {
  const { data: dashboard, isLoading, error } = useQuery({
    queryKey: ["dashboard"],
    queryFn: getDashboard,
  });

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <p className="text-muted-foreground">Carregando...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center h-64">
        <p className="text-destructive">
          {error instanceof ApiError && error.status === 403
            ? "Acesso negado. Verifique se sua sessão ainda é válida e se seu perfil tem permissão para acessar o dashboard."
            : "Erro ao carregar dashboard"}
        </p>
      </div>
    );
  }

  if (!dashboard) return null;

  const { kpis, leadsByStatus, opportunitiesByStage, tasksByStatus } = dashboard;
  const leadStatusData = (leadsByStatus[0]?.data || []).filter((item) => item.value > 0);
  const totalLeadsByStatus = leadStatusData.reduce((total, item) => total + item.value, 0);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-foreground">Dashboard</h1>
        <p className="text-muted-foreground">Visão geral do seu CRM</p>
      </div>

      {/* KPIs */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <KPICard
          title="Total de Leads"
          value={kpis.totalLeads}
          icon={Target}
          color="text-blue-500"
          subtitle={`${kpis.leadsWon} ganhos, ${kpis.leadsLost} perdidos`}
        />
        <KPICard
          title="Total de Clientes"
          value={kpis.totalClients}
          icon={Users}
          color="text-green-500"
        />
        <KPICard
          title="Oportunidades"
          value={kpis.totalOpportunities}
          icon={Wallet}
          color="text-purple-500"
          subtitle={`R$ ${kpis.totalOpportunityValue.toLocaleString("pt-BR")}`}
        />
        <KPICard
          title="Tarefas"
          value={kpis.totalTasks}
          icon={CheckSquare}
          color="text-orange-500"
          subtitle={`${kpis.openTasks} abertas, ${kpis.overdueTasks} atrasadas`}
        />
      </div>

      {/* Additional KPIs */}
      <div className="grid gap-4 md:grid-cols-3">
        <KPICard
          title="Taxa de Conversão"
          value={`${kpis.conversionRate.toFixed(1)}%`}
          icon={TrendingUp}
          color="text-emerald-500"
        />
        <KPICard
          title="Valor Ponderado"
          value={`R$ ${kpis.weightedOpportunityValue.toLocaleString("pt-BR")}`}
          icon={Wallet}
          color="text-cyan-500"
        />
        <KPICard
          title="Tarefas Atrasadas"
          value={kpis.overdueTasks}
          icon={AlertCircle}
          color="text-red-500"
        />
      </div>

      {/* Charts */}
      <div className="grid gap-6 md:grid-cols-2">
        <ChartCard title="Leads por Status">
          {totalLeadsByStatus === 0 ? (
            <div className="flex h-[280px] items-center justify-center text-sm text-muted-foreground">
              Ainda não há leads para exibir.
            </div>
          ) : (
            <div className="grid items-center gap-2 sm:grid-cols-[minmax(0,1fr)_160px]">
              <div className="relative h-[280px] min-w-0">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={leadStatusData}
                      cx="50%"
                      cy="50%"
                      innerRadius={68}
                      outerRadius={102}
                      paddingAngle={leadStatusData.length > 1 ? 3 : 0}
                      dataKey="value"
                      nameKey="label"
                      stroke="var(--card)"
                      strokeWidth={3}
                    >
                      {leadStatusData.map((entry) => (
                        <Cell key={entry.label} fill={LEAD_STATUS_COLORS[entry.label] ?? "#64748b"} />
                      ))}
                    </Pie>
                    <Tooltip
                      formatter={(value) => [value, "Leads"]}
                      labelFormatter={(label) => LEAD_STATUS_LABELS[String(label)] ?? String(label)}
                      contentStyle={{
                        backgroundColor: "var(--card)",
                        borderColor: "var(--border)",
                        borderRadius: "0.75rem",
                        color: "var(--foreground)",
                      }}
                    />
                  </PieChart>
                </ResponsiveContainer>
                <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center">
                  <span className="text-3xl font-bold text-foreground">{totalLeadsByStatus}</span>
                  <span className="text-xs text-muted-foreground">leads</span>
                </div>
              </div>
              <ul className="grid grid-cols-2 gap-x-3 gap-y-2 sm:grid-cols-1">
                {leadStatusData.map((item) => (
                  <li key={item.label} className="flex min-w-0 items-center justify-between gap-2 text-sm">
                    <span className="flex min-w-0 items-center gap-2">
                      <span
                        aria-hidden="true"
                        className="h-2.5 w-2.5 shrink-0 rounded-full"
                        style={{ backgroundColor: LEAD_STATUS_COLORS[item.label] ?? "#64748b" }}
                      />
                      <span className="truncate text-muted-foreground">
                        {LEAD_STATUS_LABELS[item.label] ?? item.label}
                      </span>
                    </span>
                    <span className="font-medium tabular-nums text-foreground">{item.value}</span>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </ChartCard>

        <ChartCard title="Oportunidades por Estágio">
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={opportunitiesByStage[0]?.data || []}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="label" />
              <YAxis />
              <Tooltip />
              <Bar dataKey="value" fill="#3b82f6" />
            </BarChart>
          </ResponsiveContainer>
        </ChartCard>

        <ChartCard title="Tarefas por Status" className="md:col-span-2">
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={tasksByStatus[0]?.data || []}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="label" />
              <YAxis />
              <Tooltip />
              <Bar dataKey="value" fill="#10b981" />
            </BarChart>
          </ResponsiveContainer>
        </ChartCard>
      </div>
    </div>
  );
}

function KPICard({
  title,
  value,
  icon: Icon,
  color,
  subtitle,
}: {
  title: string;
  value: number | string;
  icon: LucideIcon;
  color: string;
  subtitle?: string;
}) {
  return (
    <div className="rounded-xl border border-border bg-card p-6 shadow-sm">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm font-medium text-muted-foreground">{title}</p>
          <p className="mt-2 text-2xl font-bold text-foreground">{value}</p>
          {subtitle && <p className="mt-1 text-xs text-muted-foreground">{subtitle}</p>}
        </div>
        <Icon className={`h-8 w-8 ${color}`} />
      </div>
    </div>
  );
}

function ChartCard({ title, children, className }: { title: string; children: React.ReactNode; className?: string }) {
  return (
    <div className={`rounded-xl border border-border bg-card p-6 shadow-sm ${className}`}>
      <h3 className="text-lg font-semibold text-foreground mb-4">{title}</h3>
      {children}
    </div>
  );
}
