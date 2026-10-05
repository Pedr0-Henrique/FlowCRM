import { apiUrl } from "./api";

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  companyName: string;
  companySlug: string;
  name: string;
  email: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  userId: string;
  name: string;
  email: string;
  role: string;
  companyId: string;
  companyName: string;
}

export async function login(data: LoginRequest): Promise<AuthResponse> {
  const response = await fetch(apiUrl("/api/v1/auth/login"), {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    throw new Error("Credenciais inválidas");
  }

  return response.json();
}

export async function register(data: RegisterRequest): Promise<AuthResponse> {
  const response = await fetch(apiUrl("/api/v1/auth/register"), {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || "Erro ao cadastrar");
  }

  return response.json();
}

export function setAuthTokens(auth: AuthResponse) {
  localStorage.setItem("accessToken", auth.accessToken);
  localStorage.setItem("refreshToken", auth.refreshToken);
  localStorage.setItem("user", JSON.stringify(auth));
}

export function getAccessToken(): string | null {
  if (typeof window === "undefined") {
    return null;
  }

  return localStorage.getItem("accessToken");
}

export function getUser(): AuthResponse | null {
  if (typeof window === "undefined") {
    return null;
  }

  const user = localStorage.getItem("user");
  return user ? JSON.parse(user) : null;
}

export function updateStoredUser(profile: Pick<AuthResponse, "name" | "email">) {
  const user = getUser();
  if (user) {
    localStorage.setItem("user", JSON.stringify({ ...user, ...profile }));
  }
}

export function clearAuth() {
  localStorage.removeItem("accessToken");
  localStorage.removeItem("refreshToken");
  localStorage.removeItem("user");
}
