"use client";

import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Check, Pencil, Plus, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Dialog } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { getUser } from "@/lib/auth";
import { completeTask, createTask, deleteTask, getTasks, isTaskOverdue, updateTask, type Task, type TaskPriority, type TaskRequest, type TaskStatus } from "@/lib/tasks";
import { getUsers } from "@/lib/users";

const emptyForm: TaskRequest = { title: "", description: null, status: "TODO", priority: "MEDIUM", dueDate: null, assignedToId: null, clientId: null, leadId: null };
const statuses: TaskStatus[] = ["TODO", "IN_PROGRESS", "COMPLETED", "CANCELLED"];
const priorities: TaskPriority[] = ["LOW", "MEDIUM", "HIGH", "URGENT"];
const statusLabels: Record<TaskStatus, string> = {
  TODO: "A fazer",
  IN_PROGRESS: "Em andamento",
  COMPLETED: "Concluída",
  CANCELLED: "Cancelada",
};
const priorityLabels: Record<TaskPriority, string> = {
  LOW: "Baixa",
  MEDIUM: "Média",
  HIGH: "Alta",
  URGENT: "Urgente",
};

function toLocalDateTimeValue(value: string | null): string {
  if (!value) return "";
  const date = new Date(value);
  const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
  return localDate.toISOString().slice(0, 16);
}

export default function TasksPage() {
  const [form, setForm] = useState(emptyForm);
  const [selected, setSelected] = useState<Task | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<{ id: string; title: string } | null>(null);
  const [open, setOpen] = useState(false);
  const [now, setNow] = useState(() => Date.now());
  const queryClient = useQueryClient();
  const role = getUser()?.role;
  const currentUserId = getUser()?.userId ?? null;
  const canDelete = role === "ADMIN" || role === "MANAGER";
  const canAssign = canDelete;
  useEffect(() => {
    const interval = window.setInterval(() => setNow(Date.now()), 60_000);
    return () => window.clearInterval(interval);
  }, []);
  const query = useQuery({ queryKey: ["tasks"], queryFn: () => getTasks(0, 100) });
  const users = useQuery({ queryKey: ["users"], queryFn: getUsers, enabled: canAssign });
  const refresh = () => queryClient.invalidateQueries({ queryKey: ["tasks"] });
  const save = useMutation({
    mutationFn: () => {
      const request = {
        ...form,
        assignedToId: canAssign ? form.assignedToId : currentUserId,
      };
      return selected ? updateTask(selected.id, request) : createTask(request);
    },
    onSuccess: async () => { await refresh(); setOpen(false); setSelected(null); setForm(emptyForm); },
  });
  const complete = useMutation({ mutationFn: completeTask, onSuccess: refresh });
  const remove = useMutation({
    mutationFn: deleteTask,
    onSuccess: async () => {
      setDeleteTarget(null);
      await refresh();
    },
  });
  const edit = (task: Task) => {
    setSelected(task);
    setForm({ title: task.title, description: task.description, status: task.status, priority: task.priority, dueDate: task.dueDate, assignedToId: task.assignedToId, clientId: task.clientId, leadId: task.leadId });
    save.reset();
    setOpen(true);
  };

  return (
    <section className="space-y-6">
      <header className="flex items-center justify-between"><div><h1 className="text-2xl font-bold">Tarefas</h1><p className="text-muted-foreground">Organize pendências, prazos e prioridades.</p></div><Button onClick={() => { save.reset(); setSelected(null); setForm({ ...emptyForm, assignedToId: currentUserId }); setOpen(true); }}><Plus className="h-4 w-4" /> Nova tarefa</Button></header>
      {query.isError && <p role="alert" className="text-destructive">{query.error.message}</p>}
      {save.isError && <p role="alert" className="text-destructive">{save.error.message}</p>}
      {complete.isError && <p role="alert" className="text-destructive">{complete.error.message}</p>}
      {remove.isError && <p role="alert" className="text-destructive">{remove.error.message}</p>}
      <div className="overflow-x-auto rounded-xl border border-border bg-card"><table className="w-full text-sm"><thead className="bg-muted/50 text-left"><tr><th className="p-4">Tarefa</th><th className="p-4">Responsável</th><th className="p-4">Prazo</th><th className="p-4">Prioridade</th><th className="p-4">Status</th><th className="p-4 text-right">Ações</th></tr></thead><tbody className="divide-y divide-border">
        {query.data?.content.map((task) => {
          const overdue = isTaskOverdue(task, now);
          return <tr key={task.id}><td className="p-4"><div className="font-medium">{task.title}</div>{task.description && <div className="text-xs text-muted-foreground">{task.description}</div>}</td><td className="p-4">{task.assignedToName ?? "-"}</td><td className="p-4"><div>{task.dueDate ? new Date(task.dueDate).toLocaleString("pt-BR") : "-"}</div>{overdue && <span className="mt-1 inline-flex rounded-full bg-red-100 px-2 py-0.5 text-xs font-medium text-red-700 dark:bg-red-950 dark:text-red-300">Em atraso</span>}</td><td className="p-4">{priorityLabels[task.priority]}</td><td className="p-4"><span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-medium ${task.status === "COMPLETED" ? "bg-green-100 text-green-700 dark:bg-green-950 dark:text-green-300" : task.status === "CANCELLED" ? "bg-muted text-muted-foreground" : "bg-blue-100 text-blue-700 dark:bg-blue-950 dark:text-blue-300"}`}>{statusLabels[task.status]}</span></td><td className="p-4"><div className="flex justify-end gap-1">{task.status !== "COMPLETED" && task.status !== "CANCELLED" && <Button variant="ghost" size="icon" aria-label={`Concluir ${task.title}`} onClick={() => complete.mutate(task.id)}><Check className="h-4 w-4" /></Button>}<Button variant="ghost" size="icon" aria-label={`Editar ${task.title}`} onClick={() => edit(task)}><Pencil className="h-4 w-4" /></Button>{canDelete && <Button variant="ghost" size="icon" aria-label={`Excluir ${task.title}`} disabled={remove.isPending} onClick={() => { remove.reset(); setDeleteTarget({ id: task.id, title: task.title }); }}><Trash2 className="h-4 w-4" /></Button>}</div></td></tr>;
        })}
        {query.isLoading && <tr><td colSpan={6} className="p-6 text-center">Carregando...</td></tr>}
        {!query.isLoading && !query.data?.content.length && <tr><td colSpan={6} className="p-6 text-center text-muted-foreground">Nenhuma tarefa cadastrada.</td></tr>}
      </tbody></table></div>
      {open && <Dialog title={selected ? "Editar tarefa" : "Nova tarefa"} onClose={() => setOpen(false)}><form className="space-y-4" onSubmit={(event) => { event.preventDefault(); save.mutate(); }}>
        <label className="block space-y-1 text-sm">Título *<Input required maxLength={200} value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} /></label>
        <div className="grid gap-4 sm:grid-cols-2"><label className="block space-y-1 text-sm">Status<select className="h-10 w-full rounded-lg border border-border bg-background px-3" value={form.status} onChange={(event) => setForm({ ...form, status: event.target.value as TaskStatus })}>{statuses.map((status) => <option key={status} value={status}>{statusLabels[status]}</option>)}</select></label><label className="block space-y-1 text-sm">Prioridade<select className="h-10 w-full rounded-lg border border-border bg-background px-3" value={form.priority} onChange={(event) => setForm({ ...form, priority: event.target.value as TaskPriority })}>{priorities.map((priority) => <option key={priority} value={priority}>{priorityLabels[priority]}</option>)}</select></label><label className="block space-y-1 text-sm sm:col-span-2">Prazo<Input type="datetime-local" value={toLocalDateTimeValue(form.dueDate)} onChange={(event) => setForm({ ...form, dueDate: event.target.value ? new Date(event.target.value).toISOString() : null })} /><span className="block text-xs text-muted-foreground">Tarefas não concluídas ou canceladas serão marcadas como em atraso automaticamente após o prazo.</span></label></div>
        {canAssign && <label className="block space-y-1 text-sm">Responsável<select className="h-10 w-full rounded-lg border border-border bg-background px-3" value={form.assignedToId ?? ""} onChange={(event) => setForm({ ...form, assignedToId: event.target.value || null })} disabled={users.isLoading || users.isError}><option value="">Sem responsável</option>{users.data?.filter((user) => user.active).map((user) => <option key={user.id} value={user.id}>{user.name} · {user.role}</option>)}</select>{users.isError && <span role="alert" className="block text-xs text-destructive">Não foi possível carregar os funcionários. Tente novamente.</span>}</label>}
        <label className="block space-y-1 text-sm">Descrição<textarea className="min-h-24 w-full rounded-lg border border-border bg-background px-3 py-2" value={form.description ?? ""} onChange={(event) => setForm({ ...form, description: event.target.value || null })} /></label>
        <div className="flex justify-end gap-2"><Button type="button" variant="outline" onClick={() => setOpen(false)}>Cancelar</Button><Button disabled={save.isPending}>{save.isPending ? "Salvando..." : "Salvar"}</Button></div>
      </form></Dialog>}
      {deleteTarget && <ConfirmDialog
        title="Excluir tarefa?"
        description="Esta tarefa será removida permanentemente. Esta ação não pode ser desfeita."
        itemName={deleteTarget.title}
        isPending={remove.isPending}
        onCancel={() => setDeleteTarget(null)}
        onConfirm={() => remove.mutate(deleteTarget.id)}
      />}
    </section>
  );
}
