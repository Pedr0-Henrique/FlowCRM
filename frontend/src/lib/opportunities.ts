import { ApiError, apiUrl } from "./api";
import { getAccessToken } from "./auth";

export type OpportunityStage = "NEW" | "CONTACT" | "PROPOSAL" | "NEGOTIATION" | "CLOSED";

export interface Opportunity {
  id: string;
  title: string;
  description: string | null;
  value: number;
  stage: OpportunityStage;
  probability: number;
  expectedCloseDate: string | null;
  actualCloseDate: string | null;
  notes: string | null;
  clientId: string | null;
  clientName: string | null;
  leadId: string | null;
  leadName: string | null;
  assignedToId: string | null;
  assignedToName: string | null;
  createdAt: string;
}

export interface OpportunityRequest {
  title: string;
  description: string | null;
  value: number;
  stage: OpportunityStage;
  probability: number;
  expectedCloseDate: string | null;
  notes: string | null;
  clientId: string | null;
  leadId: string | null;
  assignedToId: string | null;
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
    let message = "Não foi possível concluir a operação com oportunidades";
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

export async function getOpportunities(page = 0, size = 10): Promise<{ content: Opportunity[]; totalElements: number }> {
  const params = new URLSearchParams({ page: page.toString(), size: size.toString() });
  return request(`/api/v1/opportunities?${params}`);
}

export function createOpportunity(data: OpportunityRequest) {
  return request<Opportunity>("/api/v1/opportunities", { method: "POST", body: JSON.stringify(data) });
}

export function updateOpportunity(id: string, data: OpportunityRequest) {
  return request<Opportunity>(`/api/v1/opportunities/${id}`, { method: "PUT", body: JSON.stringify(data) });
}

export function deleteOpportunity(id: string) {
  return request<void>(`/api/v1/opportunities/${id}`, { method: "DELETE" });
}
