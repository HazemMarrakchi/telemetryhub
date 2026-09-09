export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: { id: string; email: string; name: string; role: string };
}

export interface Equipment {
  id: string;
  name: string;
  serialNumber: string;
  status: 'ACTIVE' | 'MAINTENANCE' | 'STOPPED' | 'ALERT';
  modelId: string;
  modelName?: string;
  siteId: string;
  siteName?: string;
  lastSeenAt?: string;
}

export interface Site {
  id: string;
  name: string;
  city: string;
  country: string;
}

export interface MetricPoint {
  timestamp: string;
  value: number;
  metric: string;
}

export interface AlertEvent {
  id: string;
  ruleName: string;
  equipmentId: string;
  metric: string;
  value: number;
  threshold: number;
  severity: 'INFO' | 'WARNING' | 'CRITICAL' | 'FATAL';
  status: 'OPEN' | 'ACKNOWLEDGED' | 'RESOLVED';
  message: string;
  triggeredAt: string;
}

export interface AlertRule {
  id: string;
  name: string;
  metric: string;
  operator: string;
  threshold: number;
  severity: string;
  enabled: boolean;
}

export interface ReportSummary {
  id: string;
  kind: 'CSV' | 'PDF';
  status: string;
  title: string;
  rangeFrom: string;
  rangeTo: string;
  fileName: string;
  sizeBytes: number;
  createdAt: string;
}

export interface ChatResponse {
  answer: string;
  sources: { title: string; source: string; content: string; score: number }[];
}