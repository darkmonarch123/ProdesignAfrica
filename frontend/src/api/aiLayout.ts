import { apiClient } from './client';

export interface AiLayoutRequest {
  buildingType: string;
  bedrooms?: number;
  brief: string;
}

export interface AiLayoutResponse {
  canvasStateJson: string;
  modelUsed: string;
  usedFreeFallback: boolean;
  note: string | null;
}

export async function generateAiLayout(projectId: string, request: AiLayoutRequest): Promise<AiLayoutResponse> {
  const { data } = await apiClient.post<AiLayoutResponse>(`/projects/${projectId}/ai-layout`, request);
  return data;
}
