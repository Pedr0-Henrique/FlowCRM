"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Building2, Pencil, Plus, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Dialog } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { createCompany, deleteCompany, getCompanies, updateCompany, type Company, type CompanyRequest } from "@/lib/companies";

const emptyForm: CompanyRequest = { name: "", slug: "" };

export default function CompaniesPage() {
  const [form, setForm] = useState(emptyForm);
  const [selected, setSelected] = useState<Company | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<Company | null>(null);
  const [open, setOpen] = useState(false);
  const queryClient = useQueryClient();
  const companies = useQuery({ queryKey: ["companies"], queryFn: getCompanies });
  const save = useMutation({
    mutationFn: () => selected ? updateCompany(selected.id, form) : createCompany(form),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["companies"] });
      setOpen(false);
      setSelected(null);
      setForm(emptyForm);
    },
  });
  const remove = useMutation({
    mutationFn: deleteCompany,
    onSuccess: async () => {
      setDeleteTarget(null);
      await queryClient.invalidateQueries({ queryKey: ["companies"] });
    },
  });

  const edit = (company: Company) => {
    setSelected(company);
    setForm({ name: company.name, slug: company.slug });
    setOpen(true);
  };

  return (
    <section className="space-y-6">
      <header className="flex items-center justify-between">
        <div><h1 className="text-2xl font-bold">Empresas</h1><p className="text-muted-foreground">Empresas cadastradas na plataforma (acesso ADMIN).</p></div>
        <Button onClick={() => { setSelected(null); setForm(emptyForm); setOpen(true); }}><Plus className="h-4 w-4" /> Nova empresa</Button>
      </header>
      {companies.isError && <p role="alert" className="text-destructive">Não foi possível carregar empresas. Verifique se sua conta tem perfil ADMIN.</p>}
      {remove.isError && <p role="alert" className="text-destructive">{remove.error.message}</p>}
      <div className="overflow-hidden rounded-xl border border-border bg-card">
        {companies.isLoading ? <p className="p-6">Carregando empresas...</p> : (
          <table className="w-full text-sm">
            <thead className="bg-muted/50 text-left"><tr><th className="p-4">Empresa</th><th className="p-4">Slug</th><th className="p-4">Criada em</th><th className="p-4 text-right">Ações</th></tr></thead>
            <tbody className="divide-y divide-border">
              {companies.data?.map((company) => <tr key={company.id}>
                <td className="p-4 font-medium"><span className="inline-flex items-center gap-2"><Building2 className="h-4 w-4" />{company.name}</span></td>
                <td className="p-4">{company.slug}</td><td className="p-4">{new Date(company.createdAt).toLocaleDateString("pt-BR")}</td>
                <td className="p-4"><div className="flex justify-end gap-1">
                  <Button variant="ghost" size="icon" aria-label={`Editar ${company.name}`} onClick={() => edit(company)}><Pencil className="h-4 w-4" /></Button>
                  <Button variant="ghost" size="icon" aria-label={`Excluir ${company.name}`} disabled={remove.isPending} onClick={() => { remove.reset(); setDeleteTarget(company); }}><Trash2 className="h-4 w-4" /></Button>
                </div></td>
              </tr>)}
              {!companies.isLoading && !companies.data?.length && <tr><td className="p-6 text-center text-muted-foreground" colSpan={4}>Nenhuma empresa cadastrada.</td></tr>}
            </tbody>
          </table>
        )}
      </div>
      {open && <Dialog title={selected ? "Editar empresa" : "Nova empresa"} onClose={() => setOpen(false)}>
        <form className="space-y-4" onSubmit={(event) => { event.preventDefault(); save.mutate(); }}>
          <label className="block space-y-1 text-sm">Nome<Input required maxLength={180} value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /></label>
          <label className="block space-y-1 text-sm">Slug<Input required maxLength={80} pattern="[a-z0-9]+(?:-[a-z0-9]+)*" value={form.slug} onChange={(event) => setForm({ ...form, slug: event.target.value.toLowerCase().replace(/\s+/g, "-") })} /></label>
          {save.isError && <p role="alert" className="text-sm text-destructive">{save.error.message}</p>}
          <div className="flex justify-end gap-2"><Button type="button" variant="outline" onClick={() => setOpen(false)}>Cancelar</Button><Button disabled={save.isPending}>{save.isPending ? "Salvando..." : "Salvar"}</Button></div>
        </form>
      </Dialog>}
      {deleteTarget && <ConfirmDialog
        title="Excluir empresa?"
        description="A empresa e todos os dados associados serão excluídos permanentemente. Esta ação não pode ser desfeita."
        itemName={deleteTarget.name}
        isPending={remove.isPending}
        onCancel={() => setDeleteTarget(null)}
        onConfirm={() => remove.mutate(deleteTarget.id)}
      />}
    </section>
  );
}
