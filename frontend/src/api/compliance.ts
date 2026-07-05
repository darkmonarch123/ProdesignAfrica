import { apiClient } from './client';

export interface ComplianceRuleSet {
  code: string;
  country: string;
  region: string;
  name: string;
  frontSetbackMeters: number;
  sideSetbackMeters: number;
  rearSetbackMeters: number;
  roadReserveMeters: number;
  maxPlotCoveragePercent: number;
  maxHeightMeters: number;
}

export type ComplianceStatus = 'OK' | 'WARN' | 'FAIL' | 'INFO';

export interface ComplianceCheckResult {
  code: string;
  label: string;
  status: ComplianceStatus;
  detail: string;
}

export interface ComplianceReport {
  projectId: string;
  ruleSetCode: string | null;
  checks: ComplianceCheckResult[];
  evaluatedAt: string;
}

export async function listRuleSets(): Promise<ComplianceRuleSet[]> {
  const { data } = await apiClient.get<ComplianceRuleSet[]>('/compliance/rulesets');
  return data;
}

export async function getComplianceReport(projectId: string): Promise<ComplianceReport> {
  const { data } = await apiClient.get<ComplianceReport>(`/projects/${projectId}/compliance`);
  return data;
}
