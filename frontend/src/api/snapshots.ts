import { apiClient } from './client';
import type { CanvasState } from '../engine/types';

export interface SnapshotResponse {
  id: string;
  projectId: string;
  schemaVersion: string;
  canvasStateJson: string;
  createdByUserId: string;
  isAutoSave: boolean;
  createdAt: string;
}

export async function getLatestSnapshot(projectId: string): Promise<SnapshotResponse | null> {
  const { data, status } = await apiClient.get<SnapshotResponse>(`/projects/${projectId}/snapshot/latest`, {
    validateStatus: (s) => s === 200 || s === 204,
  });
  return status === 204 ? null : data;
}

export async function saveSnapshot(
  projectId: string,
  state: CanvasState,
  isAutoSave = true
): Promise<SnapshotResponse> {
  const { data } = await apiClient.post<SnapshotResponse>(`/projects/${projectId}/snapshot`, {
    schemaVersion: state.schemaVersion,
    canvasStateJson: JSON.stringify(state),
    isAutoSave,
  });
  return data;
}

export async function getHistory(projectId: string): Promise<SnapshotResponse[]> {
  const { data } = await apiClient.get<SnapshotResponse[]>(`/projects/${projectId}/history`);
  return data;
}

export async function restoreVersion(projectId: string, versionId: string): Promise<SnapshotResponse> {
  const { data } = await apiClient.post<SnapshotResponse>(`/projects/${projectId}/restore/${versionId}`);
  return data;
}
