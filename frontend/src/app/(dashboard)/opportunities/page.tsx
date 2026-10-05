"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Pencil, Plus, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Dialog } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { createOpportunity, deleteOpportunity, getOpportunities, updateOpportunity, type Opportunity, type OpportunityRequest, type OpportunityStage } from "@/lib/opportunities";
import { getClients } from "@/lib/clients";
import { getLeads } from "@/lib/leads";

const emptyForm: OpportunityRequest = {
  title: "", description: null, value: 0, stage: "NEW", probability: 10,
  expectedCloseDate: null, notes: null, clientId: null, leadId: null, assignedToId: null,
};
const stages: OpportunityStage[] = ["NEW", "CONTACT", "PROPOSAL", "NEGOTIATION", "CLOSED"];
const associationPageSize = 100;

async function getAllClients() {
  const firstPage = await getClients(0, associationPageSize);
  const clients = [...firstPage.content];
  const pages = Math.ceil(firstPage.totalElements / associationPageSize);

  for (let page = 1; page < pages; page += 1) {
    const result = await getClients(page, associationPageSize);
    clients.push(...result.content);
  }

  return clients;
}

async function getAllLeads() {
  const firstPage = await getLeads(0, associationPageSize);
  const leads = [...firstPage.content];
  const pages = Math.ceil(firstPage.totalElements / associationPageSize);

  for (let page = 1; page < pages; page += 1) {
    const result = await getLeads(page, associationPageSize);
    leads.push(...result.content);
  }

  return leads;
}

export default function OpportunitiesPage() {
  const [form, setForm] = useState(emptyForm);
  const [selected, setSelected] = useState<Opportunity | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<{ id: string; title: string } | null>(null);
  const [open, setOpen] = useState(false);
  const queryClient = useQueryClient();
  const query = useQuery({ queryKey: ["opportunities"], queryFn: () => getOpportunities(0, 100) });
  const clients = useQuery({ queryKey: ["clients", "opportunity-options"], queryFn: getAllClients });
  const leads = useQuery({ queryKey: ["leads", "opportunity-options"], queryFn: getAllLeads });
  const save = useMutation({
    mutationFn: () => selected ? updateOpportunity(selected.id, form) : createOpportunity(form),
    onSuccess: async () => { await queryClient.invalidateQueries({ queryKey: ["opportunities"] }); setOpen(false); setSelected(null); setForm(emptyForm); },
  });
  const remove = useMutation({
    mutationFn: deleteOpportunity,
    onSuccess: async () => {
      setDeleteTarget(null);
      await queryClient.invalidateQueries({ queryKey: ["opportunities"] });
    },
  });

  const edit = (item: Opportunity) => {
    setSelected(item);
    setForm({
      title: item.title, description: item.description, value: item.value, stage: item.stage,
      probability: item.probability, expectedCloseDate: item.expectedCloseDate, notes: item.notes,
      clientId: item.clientId, leadId: item.leadId, assignedToId: item.assignedToId,
    });
    setOpen(true);
  };

  return (
    <section className="space-y-6">
      <header className="flex items-center justify-between"><div><h1 className="text-2xl font-bold">Oportunidades</h1><p className="text-muted-foreground">Acompanhe negócios e previsão de receita.</p></div><Button onClick={() => { setSelected(null); setForm(emptyForm); setOpen(true); }}><Plus className="h-4 w-4" /> Nova oportunidade</Button></header>
      {query.isError && <p role="alert" className="text-destructive">{query.error.message}</p>}
      {clients.isError && <p role="alert" className="text-destructive">{clients.error.message}</p>}
      {leads.isError && <p role="alert" className="text-destructive">{leads.error.message}</p>}
      {remove.isError && <p role="alert" className="text-destructive">{remove.error.message}</p>}
      <div className="overflow-x-auto rounded-xl border border-border bg-card">
        <table className="w-full text-sm"><thead className="bg-muted/50 text-left"><tr><th className="p-4">Negócio</th><th className="p-4">Cliente / Lead</th><th className="p-4">Valor</th><th className="p-4">Etapa</th><th className="p-4">Probabilidade</th><th className="p-4 text-right">Ações</th></tr></thead>
          <tbody className="divide-y divide-border">
            {query.data?.content.map((item) => <tr key={item.id}><td className="p-4 font-medium">{item.title}</td><td className="p-4">{item.clientName ?? item.leadName ?? "-"}</td><td className="p-4">{Number(item.value).toLocaleString("pt-BR", { style: "currency", currency: "BRL" })}</td><td className="p-4">{item.stage}</td><td className="p-4">{item.probability}%</td><td className="p-4"><div className="flex justify-end gap-1"><Button variant="ghost" size="icon" aria-label={`Editar ${item.title}`} onClick={() => edit(item)}><Pencil className="h-4 w-4" /></Button><Button variant="ghost" size="icon" aria-label={`Excluir ${item.title}`} disabled={remove.isPending} onClick={() => { remove.reset(); setDeleteTarget({ id: item.id, title: item.title }); }}><Trash2 className="h-4 w-4" /></Button></div></td></tr>)}
            {query.isLoading && <tr><td colSpan={6} className="p-6 text-center">Carregando...</td></tr>}
            {!query.isLoading && !query.data?.content.length && <tr><td colSpan={6} className="p-6 text-center text-muted-foreground">Nenhuma oportunidade cadastrada.</td></tr>}
          </tbody></table>
      </div>
      {open && <Dialog title={selected ? "Editar oportunidade" : "Nova oportunidade"} onClose={() => setOpen(false)}>
        <form className="space-y-4" onSubmit={(event) => { event.preventDefault(); save.mutate(); }}>
          <label className="block space-y-1 text-sm">Título *<Input required maxLength={200} value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} /></label>
          <div className="grid gap-4 sm:grid-cols-2">
            <label className="block space-y-1 text-sm">Valor *<Input required type="number" min="0.01" step="0.01" value={form.value || ""} onChange={(event) => setForm({ ...form, value: Number(event.target.value) })} /></label>
            <label className="block space-y-1 text-sm">Etapa<select className="h-10 w-full rounded-lg border border-border bg-background px-3" value={form.stage} onChange={(event) => setForm({ ...form, stage: event.target.value as OpportunityStage })}>{stages.map((stage) => <option key={stage}>{stage}</option>)}</select></label>
            <label className="block space-y-1 text-sm">Probabilidade (%)<Input type="number" min="0" max="100" value={form.probability} onChange={(event) => setForm({ ...form, probability: Number(event.target.value) })} /></label>
            <label className="block space-y-1 text-sm">Fechamento previsto<Input type="date" value={form.expectedCloseDate ?? ""} onChange={(event) => setForm({ ...form, expectedCloseDate: event.target.value || null })} /></label>
            <label className="block space-y-1 text-sm sm:col-span-2">Descrição<Input value={form.description ?? ""} onChange={(event) => setForm({ ...form, description: event.target.value || null })} /></label>
            <label className="block space-y-1 text-sm">Cliente
              <select className="h-10 w-full rounded-lg border border-border bg-background px-3" value={form.clientId ?? ""} onChange={(event) => setForm({ ...form, clientId: event.target.value || null })} disabled={clients.isLoading || clients.isError}>
                <option value="">Sem cliente</option>
                {clients.data?.map((client) => <option key={client.id} value={client.id}>{client.name}</option>)}
              </select>
            </label>
            <label className="block space-y-1 text-sm">Lead
              <select className="h-10 w-full rounded-lg border border-border bg-background px-3" value={form.leadId ?? ""} onChange={(event) => setForm({ ...form, leadId: event.target.value || null })} disabled={leads.isLoading || leads.isError}>
                <option value="">Sem lead</option>
                {leads.data?.map((lead) => <option key={lead.id} value={lead.id}>{lead.name}</option>)}
              </select>
            </label>
          </div>
          {save.isError && <p role="alert" className="text-sm text-destructive">{save.error.message}</p>}
          <div className="flex justify-end gap-2"><Button type="button" variant="outline" onClick={() => setOpen(false)}>Cancelar</Button><Button disabled={save.isPending}>{save.isPending ? "Salvando..." : "Salvar"}</Button></div>
        </form>
      </Dialog>}
      {deleteTarget && <ConfirmDialog
        title="Excluir oportunidade?"
        description="Esta oportunidade será removida permanentemente. Esta ação não pode ser desfeita."
        itemName={deleteTarget.title}
        isPending={remove.isPending}
        onCancel={() => setDeleteTarget(null)}
        onConfirm={() => remove.mutate(deleteTarget.id)}
      />}
    </section>
  );
}
