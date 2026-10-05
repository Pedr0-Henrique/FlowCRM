import { ApiError, apiUrl } from "./api";
import { getAccessToken } from "./auth";

export interface Lead {
  id: string;
  name: string;
  email: string | null;
  phone: string | null;
  status: "NEW" | "CONTACTED" | "QUALIFIED" | "PROPOSAL" | "NEGOTIATION" | "WON" | "LOST";
  priority: "LOW" | "MEDIUM" | "HIGH" | "URGENT";
  source?: string | null;
  estimatedValue?: number | null;
  notes?: string | null;
  createdAt: string;
}

export interface CreateLeadRequest {
  name: string;
  email: string | null;
  phone: string | null;
  source: string | null;
  status: Lead["status"];
  priority: Lead["priority"];
  estimatedValue: number | null;
  notes: string | null;
}

export type UpdateLeadRequest = Partial<CreateLeadRequest>;

export async function getLeads(page = 0, size = 10, search?: string): Promise<{ content: Lead[]; totalElements: number }> {
  const token = getAccessToken();
  const params = new URLSearchParams({ page: page.toString(), size: size.toString() });
  if (search) params.append("search", search);

  const response = await fetch(apiUrl(`/api/v1/leads?${params}`), {
    headers: { Authorization: `Bearer ${token}` },
  });

  if (!response.ok) throw new ApiError(response.status, "Erro ao carregar leads");
  return response.json();
}

export async function createLead(data: CreateLeadRequest): Promise<Lead> {
  const token = getAccessToken();
  const response = await fetch(apiUrl("/api/v1/leads"), {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    throw new ApiError(response.status, "Não foi possível criar o lead");
  }

  return response.json();
}

export async function updateLead(id: string, data: UpdateLeadRequest): Promise<Lead> {
  const response = await fetch(apiUrl(`/api/v1/leads/${id}`), {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      Authorization: "Bearer " + (getAccessToken() ?? ""),
    },
    body: JSON.stringify(data),
  });
  if (!response.ok) throw new ApiError(response.status, "Não foi possível atualizar o lead");
  return response.json();
}

export async function deleteLead(id: string): Promise<void> {
  const response = await fetch(apiUrl(`/api/v1/leads/${id}`), {
    method: "DELETE",
    headers: { Authorization: "Bearer " + (getAccessToken() ?? "") },
  });
  if (!response.ok) throw new ApiError(response.status, "Não foi possível excluir o lead");
}
