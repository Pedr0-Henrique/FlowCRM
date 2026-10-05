"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createClient, deleteClient, getClients, updateClient, type Client, type CreateClientRequest } from "@/lib/clients";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Dialog } from "@/components/ui/dialog";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Search, Plus, Pencil, Trash2 } from "lucide-react";

const emptyClientForm: CreateClientRequest = {
  name: "",
  email: null,
  phone: null,
  address: null,
  city: null,
  state: null,
  zipCode: null,
  country: null,
  status: "ACTIVE",
  notes: null,
};

export default function ClientsPage() {
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [form, setForm] = useState<CreateClientRequest>(emptyClientForm);
  const [editingClient, setEditingClient] = useState<Client | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<{ id: string; name: string } | null>(null);
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ["clients", page, search],
    queryFn: () => getClients(page, 10, search || undefined),
  });

  const createMutation = useMutation({
    mutationFn: createClient,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["clients"] });
      setForm(emptyClientForm);
      setIsCreateOpen(false);
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: CreateClientRequest }) => updateClient(id, data),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["clients"] });
      setEditingClient(null);
      setIsCreateOpen(false);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteClient,
    onSuccess: async () => {
      setDeleteTarget(null);
      await queryClient.invalidateQueries({ queryKey: ["clients"] });
    },
  });

  const handleCreate = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const data = {
      ...form,
      email: form.email?.trim() || null,
      phone: form.phone?.trim() || null,
      address: form.address?.trim() || null,
      city: form.city?.trim() || null,
      state: form.state?.trim() || null,
      zipCode: form.zipCode?.trim() || null,
      country: form.country?.trim() || null,
      notes: form.notes?.trim() || null,
    };
    if (editingClient) updateMutation.mutate({ id: editingClient.id, data });
    else createMutation.mutate(data);
  };

  const startEditing = (client: Client) => {
    setEditingClient(client);
    setForm({
      name: client.name,
      email: client.email,
      phone: client.phone,
      address: client.address ?? null,
      city: client.city,
      state: client.state ?? null,
      zipCode: client.zipCode ?? null,
      country: client.country ?? null,
      status: client.status,
      notes: client.notes ?? null,
    });
    setIsCreateOpen(true);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Clientes</h1>
          <p className="text-muted-foreground">Gerencie seus clientes</p>
        </div>
        <Button type="button" onClick={() => { setEditingClient(null); setForm(emptyClientForm); setIsCreateOpen(true); }}>
          <Plus className="h-4 w-4 mr-2" />
          Novo Cliente
        </Button>
      </div>

      {isCreateOpen && (
        <Dialog title={editingClient ? "Editar cliente" : "Novo cliente"} onClose={() => setIsCreateOpen(false)}>
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
                Cidade
                <Input
                  maxLength={100}
                  value={form.city ?? ""}
                  onChange={(event) => setForm({ ...form, city: event.target.value || null })}
                />
              </label>
              <label className="block space-y-1 text-sm font-medium text-foreground">
                Estado
                <Input
                  maxLength={50}
                  value={form.state ?? ""}
                  onChange={(event) => setForm({ ...form, state: event.target.value || null })}
                />
              </label>
              <label className="block space-y-1 text-sm font-medium text-foreground sm:col-span-2">
                Endereço
                <Input
                  value={form.address ?? ""}
                  onChange={(event) => setForm({ ...form, address: event.target.value || null })}
                />
              </label>
              <label className="block space-y-1 text-sm font-medium text-foreground">
                CEP
                <Input
                  maxLength={20}
                  value={form.zipCode ?? ""}
                  onChange={(event) => setForm({ ...form, zipCode: event.target.value || null })}
                />
              </label>
              <label className="block space-y-1 text-sm font-medium text-foreground">
                País
                <Input
                  maxLength={100}
                  value={form.country ?? ""}
                  onChange={(event) => setForm({ ...form, country: event.target.value || null })}
                />
              </label>
            </div>
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
                  : editingClient ? "Salvar alterações" : "Salvar cliente"}
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
            placeholder="Buscar clientes..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="pl-10"
          />
        </div>
        {deleteTarget && <ConfirmDialog
          title="Excluir cliente?"
          description="Este cliente será removido permanentemente. Esta ação não pode ser desfeita."
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
              <th className="px-6 py-3 text-left text-xs font-medium text-muted-foreground uppercase">Telefone</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-muted-foreground uppercase">Status</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-muted-foreground uppercase">Cidade</th>
              <th className="px-6 py-3 text-right text-xs font-medium text-muted-foreground uppercase">Ações</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {isLoading ? (
              <tr>
                <td colSpan={6} className="px-6 py-8 text-center text-muted-foreground">
                  Carregando...
                </td>
              </tr>
            ) : data?.content.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-6 py-8 text-center text-muted-foreground">
                  Nenhum cliente encontrado
                </td>
              </tr>
            ) : (
              data?.content.map((client) => (
                <tr key={client.id} className="hover:bg-muted/50">
                  <td className="px-6 py-4 font-medium text-foreground">{client.name}</td>
                  <td className="px-6 py-4 text-muted-foreground">{client.email || "-"}</td>
                  <td className="px-6 py-4 text-muted-foreground">{client.phone || "-"}</td>
                  <td className="px-6 py-4">
                    <span className={`inline-flex px-2 py-1 text-xs rounded-full ${
                      client.status === "ACTIVE" ? "bg-green-100 text-green-800" :
                      client.status === "INACTIVE" ? "bg-gray-100 text-gray-800" :
                      "bg-red-100 text-red-800"
                    }`}>
                      {client.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-muted-foreground">{client.city || "-"}</td>
                  <td className="px-6 py-4 text-right">
                    <div className="flex justify-end gap-1">
                      <Button type="button" variant="ghost" size="icon" aria-label={`Editar ${client.name}`} title="Editar cliente" onClick={() => startEditing(client)}>
                        <Pencil className="h-4 w-4" />
                      </Button>
                      <Button type="button" variant="ghost" size="icon" aria-label={`Excluir ${client.name}`} title="Excluir cliente" disabled={deleteMutation.isPending} onClick={() => {
                        deleteMutation.reset();
                        setDeleteTarget({ id: client.id, name: client.name });
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
