import { apiClient } from './client';
import type { Project } from './projects';

export interface TemplateSummary {
  id: string;
  name: string;
  category: string;
  bedrooms: number;
  style: string;
  suggestedPlotWidthMeters: number;
  suggestedPlotDepthMeters: number;
  thumbnailSvg: string;
}

export interface TemplatePage {
  content: TemplateSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export async function listTemplates(bedrooms: number | null, page: number, size = 24): Promise<TemplatePage> {
  const { data } = await apiClient.get<TemplatePage>('/templates', {
    params: { bedrooms: bedrooms ?? undefined, page, size },
  });
  return data;
}

export async function useTemplate(
  templateId: string,
  projectName: string,
  plotWidthMeters?: number,
  plotDepthMeters?: number
): Promise<Project> {
  const { data } = await apiClient.post<Project>(`/templates/${templateId}/use`, {
    projectName,
    plotWidthMeters,
    plotDepthMeters,
  });
  return data;
}
