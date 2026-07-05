import { apiClient } from './client';

export interface DrawingSheetOptions {
  paperSize: 'A4' | 'A3' | 'A2';
  scale: number;
  sheetTitle: string;
}

export async function downloadDrawingSheet(projectId: string, options: DrawingSheetOptions, projectName: string) {
  const response = await apiClient.get(`/projects/${projectId}/drawing-sheet/export.pdf`, {
    params: options,
    responseType: 'blob',
  });
  const blobUrl = window.URL.createObjectURL(new Blob([response.data]));
  const link = document.createElement('a');
  link.href = blobUrl;
  link.download = `Drawing_${projectName}.pdf`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(blobUrl);
}
