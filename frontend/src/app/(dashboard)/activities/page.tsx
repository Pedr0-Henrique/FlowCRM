"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Plus, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Dialog } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { getUser } from "@/lib/auth";
import { createActivity, deleteActivity, getActivities, type ActivityRequest, type ActivityType } from "@/lib/activities";

const emptyForm: ActivityRequest = { title: "", description: null, type: "NOTE", occurredAt: null, clientId: null, leadId: null };
const types: ActivityType[] = ["NOTE", "CALL", "EMAIL", "MEETING", "OTHER"];

export default function ActivitiesPage() {
  const [open, setOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<{ id: string; title: string } | null>(null);
  const [form, setForm] = useState(emptyForm);
  const queryClient = useQueryClient();
  const query = useQuery({ queryKey: ["activities"], queryFn: () => getActivities() });
  const role = getUser()?.role;
  const canDelete = role === "ADMIN" || role === "MANAGER";
  const save = useMutation({
    mutationFn: createActivity,
    onSuccess: async () => { await queryClient.invalidateQueries({ queryKey: ["activities"] }); setOpen(false); setForm(emptyForm); },
  });
  const remove = useMutation({
    mutationFn: deleteActivity,
    onSuccess: async () => {
      setDeleteTarget(null);
      await queryClient.invalidateQueries({ queryKey: ["activities"] });
    },
  });

  return (
    <section className="space-y-6">
      <header className="flex items-center justify-between"><div><h1 className="text-2xl font-bold">Atividades</h1><p className="text-muted-foreground">Histórico de contatos, reuniões e anotações.</p></div><Button onClick={() => setOpen(true)}><Plus className="h-4 w-4" /> Registrar atividade</Button></header>
      {query.isError && <p role="alert" className="text-destructive">{query.error.message}</p>}
      {remove.isError && <p role="alert" className="text-destructive">{remove.error.message}</p>}
      <div className="space-y-3">
        {query.data?.content.map((activity) => <article key={activity.id} className="flex items-start justify-between gap-4 rounded-xl border border-border bg-card p-5">
          <div><div className="flex flex-wrap items-center gap-2"><h2 className="font-semibold">{activity.title}</h2><span className="rounded-full bg-muted px-2 py-1 text-xs">{activity.type}</span></div><p className="mt-1 text-sm text-muted-foreground">{activity.description || "Sem detalhes adicionais"}</p><p className="mt-2 text-xs text-muted-foreground">{new Date(activity.occurredAt).toLocaleString("pt-BR")} · {activity.userName}{activity.clientName ? ` · Cliente: ${activity.clientName}` : ""}{activity.leadName ? ` · Lead: ${activity.leadName}` : ""}</p></div>
          {canDelete && <Button variant="ghost" size="icon" aria-label={`Excluir ${activity.title}`} disabled={remove.isPending} onClick={() => { remove.reset(); setDeleteTarget({ id: activity.id, title: activity.title }); }}><Trash2 className="h-4 w-4" /></Button>}
        </article>)}
        {query.isLoading && <p className="p-6 text-center">Carregando atividades...</p>}
        {!query.isLoading && !query.data?.content.length && <div className="rounded-xl border border-dashed border-border p-10 text-center text-muted-foreground">Nenhuma atividade registrada.</div>}
      </div>
      {open && <Dialog title="Registrar atividade" onClose={() => setOpen(false)}><form className="space-y-4" onSubmit={(event) => { event.preventDefault(); save.mutate(form); }}>
        <label className="block space-y-1 text-sm">Título *<Input required maxLength={180} value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} /></label>
        <div className="grid gap-4 sm:grid-cols-2"><label className="block space-y-1 text-sm">Tipo<select className="h-10 w-full rounded-lg border border-border bg-background px-3" value={form.type} onChange={(event) => setForm({ ...form, type: event.target.value as ActivityType })}>{types.map((type) => <option key={type}>{type}</option>)}</select></label><label className="block space-y-1 text-sm">Data/hora<Input type="datetime-local" value={form.occurredAt ? new Date(form.occurredAt).toISOString().slice(0, 16) : ""} onChange={(event) => setForm({ ...form, occurredAt: event.target.value ? new Date(event.target.value).toISOString() : null })} /></label></div>
        <label className="block space-y-1 text-sm">Descrição<textarea className="min-h-24 w-full rounded-lg border border-border bg-background px-3 py-2" value={form.description ?? ""} onChange={(event) => setForm({ ...form, description: event.target.value || null })} /></label>
        {save.isError && <p role="alert" className="text-sm text-destructive">{save.error.message}</p>}
        <div className="flex justify-end gap-2"><Button type="button" variant="outline" onClick={() => setOpen(false)}>Cancelar</Button><Button disabled={save.isPending}>{save.isPending ? "Salvando..." : "Registrar"}</Button></div>
      </form></Dialog>}
      {deleteTarget && <ConfirmDialog
        title="Excluir atividade?"
        description="Esta atividade será removida permanentemente. Esta ação não pode ser desfeita."
        itemName={deleteTarget.title}
        isPending={remove.isPending}
        onCancel={() => setDeleteTarget(null)}
        onConfirm={() => remove.mutate(deleteTarget.id)}
      />}
    </section>
  );
}
