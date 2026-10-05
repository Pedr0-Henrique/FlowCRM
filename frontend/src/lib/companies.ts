import { ApiError, apiUrl } from "./api";
import { getAccessToken } from "./auth";

export interface Company {
  id: string;
  name: string;
  slug: string;
  createdAt: string;
  updatedAt: string;
}

export interface CompanyRequest {
  name: string;
  slug: string;
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
  if (!response.ok) throw new ApiError(response.status, "Não foi possível concluir a operação com empresas");
  if (response.status === 204) return undefined as T;
  return response.json();
}

export function getCompanies() {
  return request<Company[]>("/api/v1/companies");
}

export function createCompany(data: CompanyRequest) {
  return request<Company>("/api/v1/companies", { method: "POST", body: JSON.stringify(data) });
}

export function updateCompany(id: string, data: CompanyRequest) {
  return request<Company>(`/api/v1/companies/${id}`, { method: "PUT", body: JSON.stringify(data) });
}

export function deleteCompany(id: string) {
  return request<void>(`/api/v1/companies/${id}`, { method: "DELETE" });
}
