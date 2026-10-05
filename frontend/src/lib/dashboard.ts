import { ApiError, apiUrl } from "./api";
import { getAccessToken } from "./auth";

export interface DashboardKPIs {
  totalLeads: number;
  totalClients: number;
  totalOpportunities: number;
  totalTasks: number;
  leadsWon: number;
  leadsLost: number;
  totalOpportunityValue: number;
  weightedOpportunityValue: number;
  openTasks: number;
  overdueTasks: number;
  conversionRate: number;
}

export interface ChartDataPoint {
  label: string;
  value: number;
}

export interface ChartSeries {
  name: string;
  data: ChartDataPoint[];
}

export interface DashboardResponse {
  kpis: DashboardKPIs;
  leadsByStatus: ChartSeries[];
  leadsBySource: ChartSeries[];
  opportunitiesByStage: ChartSeries[];
  tasksByStatus: ChartSeries[];
  monthlyRevenue: ChartSeries[];
}

export async function getDashboard(): Promise<DashboardResponse> {
  const token = getAccessToken();
  const response = await fetch(apiUrl("/api/v1/dashboard"), {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });

  if (!response.ok) {
    throw new ApiError(response.status, "Erro ao carregar dashboard");
  }

  return response.json();
}
