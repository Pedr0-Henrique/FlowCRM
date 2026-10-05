import { ApiError, apiUrl } from "./api";
import { getAccessToken } from "./auth";

export interface Client {
  id: string;
  name: string;
  email: string | null;
  phone: string | null;
  status: "ACTIVE" | "INACTIVE" | "BLOCKED";
  city: string | null;
  address?: string | null;
  state?: string | null;
  zipCode?: string | null;
  country?: string | null;
  notes?: string | null;
  createdAt: string;
}

export interface CreateClientRequest {
  name: string;
  email: string | null;
  phone: string | null;
  address: string | null;
  city: string | null;
  state: string | null;
  zipCode: string | null;
  country: string | null;
  status: Client["status"];
  notes: string | null;
}

export type UpdateClientRequest = Partial<CreateClientRequest>;

export async function getClients(page = 0, size = 10, search?: string): Promise<{ content: Client[]; totalElements: number }> {
  const token = getAccessToken();
  const params = new URLSearchParams({ page: page.toString(), size: size.toString() });
  if (search) params.append("search", search);

  const response = await fetch(apiUrl(`/api/v1/clients?${params}`), {
    headers: { Authorization: `Bearer ${token}` },
  });

  if (!response.ok) throw new ApiError(response.status, "Erro ao carregar clientes");
  return response.json();
}

export async function createClient(data: CreateClientRequest): Promise<Client> {
  const token = getAccessToken();
  const response = await fetch(apiUrl("/api/v1/clients"), {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    throw new ApiError(response.status, "Não foi possível criar o cliente");
  }

  return response.json();
}

export async function updateClient(id: string, data: UpdateClientRequest): Promise<Client> {
  const response = await fetch(apiUrl(`/api/v1/clients/${id}`), {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      Authorization: "Bearer " + (getAccessToken() ?? ""),
    },
    body: JSON.stringify(data),
  });
  if (!response.ok) throw new ApiError(response.status, "Não foi possível atualizar o cliente");
  return response.json();
}

export async function deleteClient(id: string): Promise<void> {
  const response = await fetch(apiUrl(`/api/v1/clients/${id}`), {
    method: "DELETE",
    headers: { Authorization: "Bearer " + (getAccessToken() ?? "") },
  });
  if (!response.ok) throw new ApiError(response.status, "Não foi possível excluir o cliente");
}
