"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { UserPlus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { getUser } from "@/lib/auth";
import { createUser, getUsers, type CreateUserRequest, type UserRole } from "@/lib/users";

const emptyForm: CreateUserRequest = {
  name: "",
  email: "",
  password: "",
  role: "USER",
};

const roleLabels: Record<UserRole, string> = {
  ADMIN: "Administrador",
  MANAGER: "Gerente",
  SALES: "Vendas",
  USER: "Usuário",
};

export default function UsersPage() {
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const queryClient = useQueryClient();
  const currentUser = getUser();
  const isAdmin = currentUser?.role === "ADMIN";
  const allowedRoles: UserRole[] = isAdmin ? ["MANAGER", "SALES", "USER"] : ["SALES", "USER"];
  const users = useQuery({ queryKey: ["users"], queryFn: getUsers });
  const create = useMutation({
    mutationFn: createUser,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["users"] });
      setOpen(false);
      setForm(emptyForm);
    },
  });

  const openCreateDialog = () => {
    create.reset();
    setForm({ ...emptyForm, role: isAdmin ? "MANAGER" : "USER" });
    setOpen(true);
  };

  return (
    <section className="space-y-6">
      <header className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold">Usuários</h1>
          <p className="text-muted-foreground">Cadastre pessoas da sua empresa e defina o nível de acesso.</p>
        </div>
        <Button onClick={openCreateDialog}>
          <UserPlus className="h-4 w-4" />
          Novo usuário
        </Button>
      </header>

      {users.isError && (
        <div role="alert" className="rounded-xl border border-destructive/30 bg-destructive/5 p-4 text-sm text-destructive">
          {users.error.message}
          {users.error instanceof Error && "status" in users.error && users.error.status === 403
            ? " Somente ADMIN e MANAGER podem acessar a gestão de usuários."
            : ""}
        </div>
      )}

      <div className="overflow-x-auto rounded-xl border border-border bg-card">
        <table className="w-full text-sm">
          <thead className="bg-muted/50 text-left">
            <tr>
              <th className="p-4">Nome</th>
              <th className="p-4">E-mail</th>
              <th className="p-4">Perfil</th>
              <th className="p-4">Acesso</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {users.data?.map((user) => (
              <tr key={user.id}>
                <td className="p-4 font-medium">{user.name}</td>
                <td className="p-4 text-muted-foreground">{user.email}</td>
                <td className="p-4">
                  <span className="rounded-full bg-muted px-2.5 py-1 text-xs font-medium">
                    {roleLabels[user.role] ?? user.role}
                  </span>
                </td>
                <td className="p-4">
                  <span className={`inline-flex items-center gap-2 text-xs ${user.active ? "text-emerald-600" : "text-muted-foreground"}`}>
                    <span className={`h-2 w-2 rounded-full ${user.active ? "bg-emerald-500" : "bg-muted-foreground/50"}`} />
                    {user.active ? "Ativo" : "Inativo"}
                  </span>
                </td>
              </tr>
            ))}
            {users.isLoading && <tr><td colSpan={4} className="p-8 text-center text-muted-foreground">Carregando usuários...</td></tr>}
            {!users.isLoading && !users.isError && !users.data?.length && (
              <tr><td colSpan={4} className="p-8 text-center text-muted-foreground">Nenhum usuário encontrado.</td></tr>
            )}
          </tbody>
        </table>
      </div>

      <aside className="rounded-xl border border-border bg-card p-5 text-sm text-muted-foreground">
        <h2 className="font-semibold text-foreground">Sobre os perfis</h2>
        <ul className="mt-3 space-y-2">
          <li><strong className="text-foreground">MANAGER:</strong> gerencia leads, clientes, oportunidades e usuários.</li>
          <li><strong className="text-foreground">SALES:</strong> trabalha com leads, clientes, oportunidades e tarefas.</li>
          <li><strong className="text-foreground">USER:</strong> acessa tarefas e atividades.</li>
        </ul>
      </aside>

      {open && (
        <Dialog title="Cadastrar usuário" onClose={() => setOpen(false)}>
          <form className="space-y-4" onSubmit={(event) => { event.preventDefault(); create.mutate(form); }}>
            <label className="block space-y-1 text-sm">
              Nome
              <Input required maxLength={160} autoComplete="name" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} />
            </label>
            <label className="block space-y-1 text-sm">
              E-mail
              <Input required type="email" maxLength={180} autoComplete="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} />
            </label>
            <label className="block space-y-1 text-sm">
              Senha inicial
              <Input required type="password" minLength={6} maxLength={100} autoComplete="new-password" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} />
              <span className="block text-xs text-muted-foreground">Compartilhe a senha inicial com a pessoa por um canal seguro.</span>
            </label>
            <label className="block space-y-1 text-sm">
              Perfil
              <select
                className="h-10 w-full rounded-lg border border-border bg-background px-3"
                value={form.role}
                onChange={(event) => setForm({ ...form, role: event.target.value as CreateUserRequest["role"] })}
              >
                {allowedRoles.map((role) => <option key={role} value={role}>{roleLabels[role]}</option>)}
              </select>
            </label>
            {create.isError && <p role="alert" className="text-sm text-destructive">{create.error.message}</p>}
            <div className="flex justify-end gap-2">
              <Button type="button" variant="outline" disabled={create.isPending} onClick={() => setOpen(false)}>Cancelar</Button>
              <Button disabled={create.isPending}>{create.isPending ? "Cadastrando..." : "Cadastrar usuário"}</Button>
            </div>
          </form>
        </Dialog>
      )}
    </section>
  );
}
