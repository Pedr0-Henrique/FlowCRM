"use client";

import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useTheme } from "@/components/theme-provider";
import { getUser, updateStoredUser } from "@/lib/auth";
import { updateUserProfile } from "@/lib/users";

export default function SettingsPage() {
  const user = getUser();
  const [name, setName] = useState(user?.name ?? "");
  const [email, setEmail] = useState(user?.email ?? "");
  const [password, setPassword] = useState("");
  const { resolvedTheme, setTheme } = useTheme();
  const save = useMutation({
    mutationFn: () => {
      if (!user) throw new Error("Sessão não encontrada. Entre novamente.");
      return updateUserProfile(user.userId, { name, email, ...(password ? { password } : {}) });
    },
    onSuccess: (profile) => {
      updateStoredUser({ name: profile.name, email: profile.email });
      setPassword("");
    },
  });

  return <section className="mx-auto max-w-3xl space-y-6">
    <header><h1 className="text-2xl font-bold">Configurações</h1><p className="text-muted-foreground">Gerencie seu perfil e preferências.</p></header>
    <article className="space-y-5 rounded-xl border border-border bg-card p-6">
      <div><h2 className="text-lg font-semibold">Perfil</h2><p className="text-sm text-muted-foreground">Atualize seus dados de acesso.</p></div>
      <form className="space-y-4" onSubmit={(event) => { event.preventDefault(); save.mutate(); }}>
        <label className="block space-y-1 text-sm">Nome<Input required maxLength={160} value={name} onChange={(event) => setName(event.target.value)} /></label>
        <label className="block space-y-1 text-sm">E-mail<Input required type="email" maxLength={180} value={email} onChange={(event) => setEmail(event.target.value)} /></label>
        <label className="block space-y-1 text-sm">Nova senha<Input type="password" minLength={6} maxLength={100} autoComplete="new-password" value={password} onChange={(event) => setPassword(event.target.value)} placeholder="Deixe em branco para manter a senha atual" /></label>
        {save.isError && <p role="alert" className="text-sm text-destructive">{save.error.message}</p>}
        {save.isSuccess && <p role="status" className="text-sm text-green-600">Perfil atualizado.</p>}
        <Button disabled={save.isPending}>{save.isPending ? "Salvando..." : "Salvar perfil"}</Button>
      </form>
    </article>
    <article className="flex items-center justify-between gap-4 rounded-xl border border-border bg-card p-6">
      <div><h2 className="font-semibold">Aparência</h2><p className="text-sm text-muted-foreground">Tema atual: {resolvedTheme === "dark" ? "escuro" : "claro"}</p></div>
      <Button variant="outline" onClick={() => setTheme(resolvedTheme === "dark" ? "light" : "dark")}>Alternar tema</Button>
    </article>
    <article className="rounded-xl border border-border bg-card p-6"><h2 className="font-semibold">Empresa e acesso</h2><dl className="mt-3 grid gap-3 text-sm sm:grid-cols-2"><div><dt className="text-muted-foreground">Empresa</dt><dd>{user?.companyName ?? "-"}</dd></div><div><dt className="text-muted-foreground">Perfil</dt><dd>{user?.role ?? "-"}</dd></div></dl></article>
  </section>;
}
