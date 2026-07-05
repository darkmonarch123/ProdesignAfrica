import { apiClient } from './client';

export interface Project {
  id: string;
  name: string;
  location: string | null;
  description: string | null;
  ownerId: string;
  complianceRuleCode: string | null;
  plotWidthMeters: number | null;
  plotDepthMeters: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface ProjectRequest {
  name: string;
  location?: string;
  description?: string;
  complianceRuleCode?: string | null;
  plotWidthMeters?: number | null;
  plotDepthMeters?: number | null;
}

export async function listProjects(): Promise<Project[]> {
  const { data } = await apiClient.get<Project[]>('/projects');
  return data;
}

export async function getProject(id: string): Promise<Project> {
  const { data } = await apiClient.get<Project>(`/projects/${id}`);
  return data;
}

export async function createProject(request: ProjectRequest): Promise<Project> {
  const { data } = await apiClient.post<Project>('/projects', request);
  return data;
}

export async function updateProject(id: string, request: ProjectRequest): Promise<Project> {
  const { data } = await apiClient.put<Project>(`/projects/${id}`, request);
  return data;
}

export async function deleteProject(id: string): Promise<void> {
  await apiClient.delete(`/projects/${id}`);
}
