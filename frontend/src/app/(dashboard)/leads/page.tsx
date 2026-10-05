"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createLead, deleteLead, getLeads, updateLead, type CreateLeadRequest, type Lead } from "@/lib/leads";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Dialog } from "@/components/ui/dialog";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Search, Plus, Pencil, Trash2 } from "lucide-react";

const emptyLeadForm: CreateLeadRequest = {
  name: "",
  email: null,
  phone: null,
  source: null,
  status: "NEW",
  priority: "MEDIUM",
  estimatedValue: null,
  notes: null,
};

export default function LeadsPage() {
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [form, setForm] = useState<CreateLeadRequest>(emptyLeadForm);
  const [editingLead, setEditingLead] = useState<Lead | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<{ id: string; name: string } | null>(null);
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ["leads", page, search],
    queryFn: () => getLeads(page, 10, search || undefined),
  });

  const createMutation = useMutation({
    mutationFn: createLead,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["leads"] });
      setForm(emptyLeadForm);
      setIsCreateOpen(false);
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: CreateLeadRequest }) => updateLead(id, data),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["leads"] });
      setEditingLead(null);
      setIsCreateOpen(false);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteLead,
    onSuccess: async () => {
      setDeleteTarget(null);
      await queryClient.invalidateQueries({ queryKey: ["leads"] });
    },
  });

  const handleCreate = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const data = {
      ...form,
      email: form.email?.trim() || null,
      phone: form.phone?.trim() || null,
      source: form.source?.trim() || null,
      notes: form.notes?.trim() || null,
    };
    if (editingLead) updateMutation.mutate({ id: editingLead.id, data });
    else createMutation.mutate(data);
  };

  const startEditing = (lead: Lead) => {
    setEditingLead(lead);
    setForm({
      name: lead.name,
      email: lead.email,
      phone: lead.phone,
      source: lead.source ?? null,
      status: lead.status,
      priority: lead.priority,
      estimatedValue: lead.estimatedValue ?? null,
      notes: lead.notes ?? null,
    });
    setIsCreateOpen(true);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Leads</h1>
          <p className="text-muted-foreground">Gerencie seus leads e pipeline</p>
        </div>
        <Button type="button" onClick={() => { setEditingLead(null); setForm(emptyLeadForm); setIsCreateOpen(true); }}>
          <Plus className="h-4 w-4 mr-2" />
          Novo Lead
        </Button>
      </div>

      {isCreateOpen && (
        <Dialog title={editingLead ? "Editar lead" : "Novo lead"} onClose={() => setIsCreateOpen(false)}>
          <form className="space-y-4" onSubmit={handleCreate}>
            <label className="block space-y-1 text-sm font-medium text-foreground">
              Nome *
              <Input
                required
                maxLength={180}
                autoFocus
                value={form.name}
                onChange={(event) => setForm({ ...form, name: event.target.value })}
              />
            </label>
            <div className="grid gap-4 sm:grid-cols-2">
              <label className="block space-y-1 text-sm font-medium text-foreground">
                E-mail
                <Input
                  type="email"
                  maxLength={180}
                  value={form.email ?? ""}
                  onChange={(event) => setForm({ ...form, email: event.target.value || null })}
                />
              </label>
              <label className="block space-y-1 text-sm font-medium text-foreground">
                Telefone
                <Input
                  type="tel"
                  maxLength={32}
                  value={form.phone ?? ""}
                  onChange={(event) => setForm({ ...form, phone: event.target.value || null })}
                />
              </label>
              <label className="block space-y-1 text-sm font-medium text-foreground">
                Origem
                <select
                  className="flex h-10 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm"
                  value={form.source ?? ""}
                  onChange={(event) => setForm({ ...form, source: event.target.value || null })}
                >
                  <option value="">Não informada</option>
                  <option value="WEBSITE">Site</option>
                  <option value="REFERRAL">Indicação</option>
                  <option value="SOCIAL_MEDIA">Redes sociais</option>
                  <option value="EMAIL">E-mail</option>
                  <option value="PHONE">Telefone</option>
                  <option value="EVENT">Evento</option>
                  <option value="ADVERTISEMENT">Anúncio</option>
                  <option value="OTHER">Outra</option>
                </select>
              </label>
              <label className="block space-y-1 text-sm font-medium text-foreground">
                Prioridade
                <select
                  className="flex h-10 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm"
                  value={form.priority}
                  onChange={(event) =>
                    setForm({ ...form, priority: event.target.value as Lead["priority"] })
                  }
                >
                  <option value="LOW">Baixa</option>
                  <option value="MEDIUM">Média</option>
                  <option value="HIGH">Alta</option>
                  <option value="URGENT">Urgente</option>
                </select>
              </label>
            </div>
            <label className="block space-y-1 text-sm font-medium text-foreground">
              Valor estimado
              <Input
                type="number"
                min="0"
                step="0.01"
                value={form.estimatedValue ?? ""}
                onChange={(event) =>
                  setForm({
                    ...form,
                    estimatedValue: event.target.value ? Number(event.target.value) : null,
                  })
                }
              />
            </label>
            <label className="block space-y-1 text-sm font-medium text-foreground">
              Observações
              <textarea
                className="min-h-24 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm"
                value={form.notes ?? ""}
                onChange={(event) => setForm({ ...form, notes: event.target.value || null })}
              />
            </label>
            {(createMutation.isError || updateMutation.isError) && (
              <p role="alert" className="text-sm text-destructive">
                {(createMutation.error ?? updateMutation.error)?.message}
              </p>
            )}
            <div className="flex justify-end gap-2">
              <Button type="button" variant="outline" onClick={() => setIsCreateOpen(false)}>
                Cancelar
              </Button>
              <Button type="submit" disabled={createMutation.isPending || updateMutation.isPending}>
                {createMutation.isPending || updateMutation.isPending
                  ? "Salvando..."
                  : editingLead ? "Salvar alterações" : "Salvar lead"}
              </Button>
            </div>
          </form>
        </Dialog>
      )}

      {deleteMutation.isError && <p role="alert" className="text-sm text-destructive">{deleteMutation.error.message}</p>}
      <div className="flex items-center gap-4">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Buscar leads..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="pl-10"
          />
        </div>
        {deleteTarget && <ConfirmDialog
          title="Excluir lead?"
          description="Este lead será removido permanentemente. Esta ação não pode ser desfeita."
          itemName={deleteTarget.name}
          isPending={deleteMutation.isPending}
          onCancel={() => setDeleteTarget(null)}
          onConfirm={() => deleteMutation.mutate(deleteTarget.id)}
        />}
      </div>

      <div className="rounded-xl border border-border bg-card overflow-hidden">
        <table className="w-full">
          <thead className="bg-muted/50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-muted-foreground uppercase">Nome</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-muted-foreground uppercase">E-mail</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-muted-foreground uppercase">Status</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-muted-foreground uppercase">Prioridade</th>
              <th className="px-6 py-3 text-right text-xs font-medium text-muted-foreground uppercase">Ações</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {isLoading ? (
              <tr>
                <td colSpan={5} className="px-6 py-8 text-center text-muted-foreground">
                  Carregando...
                </td>
              </tr>
            ) : data?.content.length === 0 ? (
              <tr>
                <td colSpan={5} className="px-6 py-8 text-center text-muted-foreground">
                  Nenhum lead encontrado
                </td>
              </tr>
            ) : (
              data?.content.map((lead) => (
                <tr key={lead.id} className="hover:bg-muted/50">
                  <td className="px-6 py-4 font-medium text-foreground">{lead.name}</td>
                  <td className="px-6 py-4 text-muted-foreground">{lead.email || "-"}</td>
                  <td className="px-6 py-4">
                    <span className={`inline-flex px-2 py-1 text-xs rounded-full ${
                      lead.status === "WON" ? "bg-green-100 text-green-800" :
                      lead.status === "LOST" ? "bg-red-100 text-red-800" :
                      lead.status === "NEW" ? "bg-blue-100 text-blue-800" :
                      "bg-gray-100 text-gray-800"
                    }`}>
                      {lead.status}
                    </span>
                  </td>
                  <td className="px-6 py-4">
                    <span className={`inline-flex px-2 py-1 text-xs rounded-full ${
                      lead.priority === "URGENT" ? "bg-red-100 text-red-800" :
                      lead.priority === "HIGH" ? "bg-orange-100 text-orange-800" :
                      lead.priority === "MEDIUM" ? "bg-yellow-100 text-yellow-800" :
                      "bg-gray-100 text-gray-800"
                    }`}>
                      {lead.priority}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-right">
                    <div className="flex justify-end gap-1">
                      <Button type="button" variant="ghost" size="icon" aria-label={`Editar ${lead.name}`} title="Editar lead" onClick={() => startEditing(lead)}>
                        <Pencil className="h-4 w-4" />
                      </Button>
                      <Button type="button" variant="ghost" size="icon" aria-label={`Excluir ${lead.name}`} title="Excluir lead" disabled={deleteMutation.isPending} onClick={() => {
                        deleteMutation.reset();
                        setDeleteTarget({ id: lead.id, name: lead.name });
                      }}>
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>

        {data && data.totalElements > 10 && (
          <div className="px-6 py-4 border-t border-border flex items-center justify-between">
            <p className="text-sm text-muted-foreground">
              Mostrando {page * 10 + 1} a {Math.min((page + 1) * 10, data.totalElements)} de {data.totalElements}
            </p>
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage(Math.max(0, page - 1))}
                disabled={page === 0}
              >
                Anterior
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage(page + 1)}
                disabled={(page + 1) * 10 >= data.totalElements}
              >
                Próxima
              </Button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
