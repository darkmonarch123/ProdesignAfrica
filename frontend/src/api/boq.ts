import { apiClient } from './client';

export type Currency = 'NGN' | 'GHS' | 'KES' | 'ZAR';

export interface BoqLineItem {
  category: string;
  description: string;
  quantity: number;
  unit: string;
  rate: number;
  amount: number;
}

export interface BoqResponse {
  projectId: string;
  currency: string;
  lineItems: BoqLineItem[];
  totalAmount: number;
  generatedAt: string;
  notes: string[];
}

export async function getBoqPreview(projectId: string, currency: Currency): Promise<BoqResponse> {
  const { data } = await apiClient.get<BoqResponse>(`/projects/${projectId}/boq`, { params: { currency } });
  return data;
}

async function downloadFile(url: string, filename: string) {
  const response = await apiClient.get(url, { responseType: 'blob' });
  const blobUrl = window.URL.createObjectURL(new Blob([response.data]));
  const link = document.createElement('a');
  link.href = blobUrl;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(blobUrl);
}

export async function downloadBoqExcel(projectId: string, currency: Currency, projectName: string) {
  await downloadFile(`/projects/${projectId}/boq/export.xlsx?currency=${currency}`, `BOQ_${projectName}.xlsx`);
}

export async function downloadBoqPdf(projectId: string, currency: Currency, projectName: string) {
  await downloadFile(`/projects/${projectId}/boq/export.pdf?currency=${currency}`, `BOQ_${projectName}.pdf`);
}
