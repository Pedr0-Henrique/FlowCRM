"use client";

import { useQuery } from "@tanstack/react-query";
import { Download, RefreshCw } from "lucide-react";
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { Button } from "@/components/ui/button";
import { getDashboard } from "@/lib/dashboard";

export default function ReportsPage() {
  const query = useQuery({ queryKey: ["reports"], queryFn: getDashboard });
  const exportCsv = () => {
    if (!query.data) return;
    const { kpis, leadsByStatus, opportunitiesByStage, tasksByStatus, monthlyRevenue } = query.data;
    const rows = [
      ["Relatório", "Indicador", "Valor"],
      ["Resumo", "Total de leads", kpis.totalLeads],
      ["Resumo", "Leads ganhos", kpis.leadsWon],
      ["Resumo", "Leads perdidos", kpis.leadsLost],
      ["Resumo", "Clientes", kpis.totalClients],
      ["Resumo", "Oportunidades", kpis.totalOpportunities],
      ["Resumo", "Valor de oportunidades", kpis.totalOpportunityValue],
      ["Resumo", "Valor ponderado", kpis.weightedOpportunityValue],
      ["Resumo", "Taxa de conversão (%)", kpis.conversionRate],
      ["Resumo", "Tarefas abertas", kpis.openTasks],
      ["Resumo", "Tarefas atrasadas", kpis.overdueTasks],
      ...leadsByStatus.flatMap((series) => series.data.map((point) => ["Leads por status", point.label, point.value])),
      ...opportunitiesByStage.flatMap((series) => series.data.map((point) => ["Oportunidades por etapa", point.label, point.value])),
      ...tasksByStatus.flatMap((series) => series.data.map((point) => ["Tarefas por status", point.label, point.value])),
      ...monthlyRevenue.flatMap((series) => series.data.map((point) => ["Receita mensal", point.label, point.value])),
    ];
    const csv = rows.map((row) => row.map((cell) => `"${String(cell).replaceAll('"', '""')}"`).join(";")).join("\r\n");
    const link = document.createElement("a");
    const url = URL.createObjectURL(new Blob(["\uFEFF" + csv], { type: "text/csv;charset=utf-8" }));
    link.href = url;
    link.download = `flowcrm-relatorio-${new Date().toISOString().slice(0, 10)}.csv`;
    link.click();
    window.setTimeout(() => URL.revokeObjectURL(url), 1000);
  };

  if (query.isLoading) return <p className="p-8 text-muted-foreground">Carregando relatórios...</p>;
  if (query.isError || !query.data) return <div className="space-y-3"><h1 className="text-2xl font-bold">Relatórios</h1><p role="alert" className="text-destructive">{query.error?.message ?? "Não foi possível carregar os relatórios."}</p><Button variant="outline" onClick={() => query.refetch()}><RefreshCw className="h-4 w-4" /> Tentar novamente</Button></div>;

  const { kpis, leadsByStatus, opportunitiesByStage, tasksByStatus, monthlyRevenue } = query.data;
  const metrics = [["Leads", kpis.totalLeads], ["Clientes", kpis.totalClients], ["Oportunidades", kpis.totalOpportunities], ["Tarefas", kpis.totalTasks], ["Conversão", `${kpis.conversionRate.toFixed(1)}%`], ["Tarefas atrasadas", kpis.overdueTasks]] as const;

  return <section className="space-y-6">
    <header className="flex flex-wrap items-center justify-between gap-3"><div><h1 className="text-2xl font-bold">Relatórios</h1><p className="text-muted-foreground">Indicadores consolidados da sua empresa.</p></div><div className="flex gap-2"><Button variant="outline" onClick={() => query.refetch()}><RefreshCw className="h-4 w-4" /> Atualizar</Button><Button onClick={exportCsv}><Download className="h-4 w-4" /> Exportar CSV</Button></div></header>
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">{metrics.map(([label, value]) => <article key={label} className="rounded-xl border border-border bg-card p-5"><p className="text-sm text-muted-foreground">{label}</p><p className="mt-2 text-2xl font-bold">{value}</p></article>)}</div>
    <div className="grid gap-6 lg:grid-cols-2">
      <ReportChart title="Leads por status" data={leadsByStatus[0]?.data ?? []} />
      <ReportChart title="Oportunidades por etapa" data={opportunitiesByStage[0]?.data ?? []} />
      <ReportChart title="Tarefas por status" data={tasksByStatus[0]?.data ?? []} />
      <ReportChart title="Receita mensal" data={monthlyRevenue[0]?.data ?? []} />
    </div>
  </section>;
}

function ReportChart({ title, data }: { title: string; data: { label: string; value: number }[] }) {
  return <article className="rounded-xl border border-border bg-card p-5"><h2 className="font-semibold">{title}</h2>{data.length ? <div className="mt-4 h-72"><ResponsiveContainer width="100%" height="100%"><BarChart data={data}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="label" /><YAxis /><Tooltip /><Bar dataKey="value" fill="#3b82f6" /></BarChart></ResponsiveContainer></div> : <p className="py-12 text-center text-sm text-muted-foreground">Sem dados para exibir.</p>}</article>;
}
