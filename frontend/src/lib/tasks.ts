import { ApiError, apiUrl } from "./api";
import { getAccessToken } from "./auth";

export type TaskStatus = "TODO" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";
export type TaskPriority = "LOW" | "MEDIUM" | "HIGH" | "URGENT";

export interface Task {
  id: string;
  title: string;
  description: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  dueDate: string | null;
  assignedToId: string | null;
  assignedToName: string | null;
  clientId: string | null;
  clientName: string | null;
  leadId: string | null;
  leadName: string | null;
  createdAt: string;
}

export interface TaskRequest {
  title: string;
  description: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  dueDate: string | null;
  assignedToId: string | null;
  clientId: string | null;
  leadId: string | null;
}

export function isTaskOverdue(
  task: Pick<Task, "dueDate" | "status">,
  now = Date.now(),
): boolean {
  return Boolean(
    task.dueDate &&
      new Date(task.dueDate).getTime() < now &&
      task.status !== "COMPLETED" &&
      task.status !== "CANCELLED",
  );
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(apiUrl(path), {
    ...init,
    headers: {
      Authorization: "Bearer " + (getAccessToken() ?? ""),
      ...(init?.body ? { "Content-Type": "application/json" } : {}),
      ...init?.headers,
    },
  });
  if (!response.ok) {
    const body = await response.text();
    let message = "Não foi possível concluir a operação com tarefas";
    if (body) {
      try {
        const parsed: unknown = JSON.parse(body);
        if (parsed && typeof parsed === "object" && "message" in parsed && typeof parsed.message === "string") {
          message = parsed.message;
        }
      } catch {
        message = "A API retornou uma resposta inválida";
      }
    }
    throw new ApiError(response.status, `${message} (HTTP ${response.status})`);
  }
  if (response.status === 204) return undefined as T;
  return response.json();
}

export function getTasks(page = 0, size = 20) {
  return request<{ content: Task[]; totalElements: number }>(
    `/api/v1/tasks?${new URLSearchParams({ page: String(page), size: String(size) })}`,
  );
}

export function createTask(data: TaskRequest) {
  return request<Task>("/api/v1/tasks", { method: "POST", body: JSON.stringify(data) });
}

export function updateTask(id: string, data: TaskRequest) {
  return request<Task>(`/api/v1/tasks/${id}`, { method: "PUT", body: JSON.stringify(data) });
}

export function completeTask(id: string) {
  return request<Task>(`/api/v1/tasks/${id}/complete`, { method: "PATCH" });
}

export function deleteTask(id: string) {
  return request<void>(`/api/v1/tasks/${id}`, { method: "DELETE" });
}
