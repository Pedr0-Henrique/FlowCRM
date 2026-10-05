import { ApiError, apiUrl } from "./api";
import { getAccessToken } from "./auth";

export type ActivityType = "NOTE" | "CALL" | "EMAIL" | "MEETING" | "OTHER";

export interface Activity {
  id: string;
  title: string;
  description: string | null;
  type: ActivityType;
  occurredAt: string;
  userId: string;
  userName: string;
  clientId: string | null;
  clientName: string | null;
  leadId: string | null;
  leadName: string | null;
}

export interface ActivityRequest {
  title: string;
  description: string | null;
  type: ActivityType;
  occurredAt: string | null;
  clientId: string | null;
  leadId: string | null;
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
  if (!response.ok) throw new ApiError(response.status, "Não foi possível concluir a operação com atividades");
  if (response.status === 204) return undefined as T;
  return response.json();
}

export function getActivities(page = 0, size = 100) {
  return request<{ content: Activity[]; totalElements: number }>(
    `/api/v1/activities?${new URLSearchParams({ page: String(page), size: String(size) })}`,
  );
}

export function createActivity(data: ActivityRequest) {
  return request<Activity>("/api/v1/activities", { method: "POST", body: JSON.stringify(data) });
}

export function deleteActivity(id: string) {
  return request<void>(`/api/v1/activities/${id}`, { method: "DELETE" });
}
