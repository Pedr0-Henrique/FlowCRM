import { ApiError, apiUrl } from "./api";
import { getAccessToken } from "./auth";

export interface UserProfile {
  id: string;
  name: string;
  email: string;
  role: UserRole;
  active: boolean;
  companyId: string;
  companyName: string;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateUserProfile {
  name: string;
  email: string;
  password?: string;
}

export type UserRole = "ADMIN" | "MANAGER" | "SALES" | "USER";

export interface CreateUserRequest {
  name: string;
  email: string;
  password: string;
  role: Exclude<UserRole, "ADMIN">;
}

async function usersRequest<T>(path: string, init?: RequestInit): Promise<T> {
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
    let message = "Não foi possível concluir a operação de usuários";
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
  return response.json();
}

export function getUsers(): Promise<UserProfile[]> {
  return usersRequest<UserProfile[]>("/api/v1/users");
}

export function createUser(data: CreateUserRequest): Promise<UserProfile> {
  return usersRequest<UserProfile>("/api/v1/users", {
    method: "POST",
    body: JSON.stringify(data),
  });
}

export async function updateUserProfile(id: string, data: UpdateUserProfile): Promise<UserProfile> {
  const response = await fetch(apiUrl(`/api/v1/users/${id}`), {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      Authorization: "Bearer " + (getAccessToken() ?? ""),
    },
    body: JSON.stringify(data),
  });
  if (!response.ok) throw new ApiError(response.status, "Não foi possível atualizar o perfil");
  return response.json();
}
