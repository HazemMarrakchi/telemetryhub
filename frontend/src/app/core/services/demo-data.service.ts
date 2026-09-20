import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

import {
  AlertEvent, AlertRule, Availability, CostTrend, Equipment, GlobalOee, MaintenanceKpi,
  MaintenanceSchedule, MetricPoint, MonthlyCost, ReportSummary, Site, UserSummary, WorkOrder,
  WorkOrderPriority, WorkOrderStatus, WorkOrderType,
} from '../models';

/**
 * Central demo-mode data store.
 *
 * Holds realistic in-memory seed data and generators so the whole frontend can
 * run standalone (`environment.demo === true`) on static hosts (GitHub Pages /
 * Vercel) without any backend. State is mutable in-memory so create/update
 * actions performed in the UI are reflected on next load.
 */
@Injectable({ providedIn: 'root' })
export class DemoDataService {
  sites: Site[] = [
    { id: 'site-tunis', name: 'Usine Tunis', city: 'Tunis', country: 'TN' },
    { id: 'site-sfax', name: 'Atelier Sfax', city: 'Sfax', country: 'TN' },
    { id: 'site-bizerte', name: 'Site Bizerte', city: 'Bizerte', country: 'TN' },
  ];

  models: { id: string; name: string }[] = [
    { id: 'm-lathe', name: 'Tour CNC AX-200' },
    { id: 'm-press', name: 'Presse hydraulique HP-80' },
    { id: 'm-compressor', name: 'Compresseur SCREW-75' },
    { id: 'm-robot', name: 'Robot KUKA KR6' },
  ];

  equipment: Equipment[] = [
    { id: 'eq-01', name: 'Tour CNC 01', serialNumber: 'AX200-0001', status: 'ACTIVE', modelId: 'm-lathe', modelName: 'Tour CNC AX-200', siteId: 'site-tunis', siteName: 'Usine Tunis', lastSeenAt: new Date(Date.now() - 2 * 60000).toISOString() },
    { id: 'eq-02', name: 'Presse hydraulique 02', serialNumber: 'HP80-0002', status: 'ACTIVE', modelId: 'm-press', modelName: 'Presse hydraulique HP-80', siteId: 'site-tunis', siteName: 'Usine Tunis', lastSeenAt: new Date(Date.now() - 45 * 1000).toISOString() },
    { id: 'eq-03', name: 'Compresseur 03', serialNumber: 'SC75-0003', status: 'MAINTENANCE', modelId: 'm-compressor', modelName: 'Compresseur SCREW-75', siteId: 'site-sfax', siteName: 'Atelier Sfax', lastSeenAt: new Date(Date.now() - 35 * 60000).toISOString() },
    { id: 'eq-04', name: 'Robot KUKA 04', serialNumber: 'KR6-0004', status: 'ALERT', modelId: 'm-robot', modelName: 'Robot KUKA KR6', siteId: 'site-bizerte', siteName: 'Site Bizerte', lastSeenAt: new Date(Date.now() - 5 * 60000).toISOString() },
    { id: 'eq-05', name: 'Tour CNC 05', serialNumber: 'AX200-0005', status: 'ACTIVE', modelId: 'm-lathe', modelName: 'Tour CNC AX-200', siteId: 'site-tunis', siteName: 'Usine Tunis', lastSeenAt: new Date(Date.now() - 90 * 1000).toISOString() },
    { id: 'eq-06', name: 'Presse hydraulique 06', serialNumber: 'HP80-0006', status: 'STOPPED', modelId: 'm-press', modelName: 'Presse hydraulique HP-80', siteId: 'site-sfax', siteName: 'Atelier Sfax', lastSeenAt: new Date(Date.now() - 120 * 60000).toISOString() },
  ];

  users: UserSummary[] = [
    { id: 'u-1', email: 'sami.benali@acme.com', fullName: 'Sami Benali', role: 'MECHANIC' },
    { id: 'u-2', email: 'ines.gharbi@acme.com', fullName: 'Ines Gharbi', role: 'TECHNICIAN' },
    { id: 'u-3', email: 'karim.trabelsi@acme.com', fullName: 'Karim Trabelsi', role: 'SUPERVISOR' },
    { id: 'u-4', email: 'admin@acme.com', fullName: 'Admin Acme', role: 'ADMIN' },
  ];

  rules: AlertRule[] = [
    { id: 'rule-1', name: 'Température moteur', metric: 'temperature', operator: 'GT', threshold: 70, severity: 'WARNING', enabled: true },
    { id: 'rule-2', name: 'Vibration élevée', metric: 'vibration', operator: 'GT', threshold: 6.5, severity: 'CRITICAL', enabled: true },
    { id: 'rule-3', name: 'Pression compresseur', metric: 'pressure', operator: 'LT', threshold: 4, severity: 'WARNING', enabled: true },
  ];

  alerts: AlertEvent[] = [
    { id: 'al-1', ruleName: 'Vibration élevée', equipmentId: 'eq-04', metric: 'vibration', value: 7.8, threshold: 6.5, severity: 'CRITICAL', status: 'OPEN', message: 'Vibration au-dessus du seuil', triggeredAt: new Date(Date.now() - 6 * 60000).toISOString() },
    { id: 'al-2', ruleName: 'Température moteur', equipmentId: 'eq-03', metric: 'temperature', value: 74.2, threshold: 70, severity: 'WARNING', status: 'ACKNOWLEDGED', message: 'Température moteur élevée', triggeredAt: new Date(Date.now() - 90 * 60000).toISOString() },
    { id: 'al-3', ruleName: 'Pression compresseur', equipmentId: 'eq-06', metric: 'pressure', value: 3.2, threshold: 4, severity: 'WARNING', status: 'OPEN', message: 'Pression trop basse', triggeredAt: new Date(Date.now() - 3 * 3600000).toISOString() },
    { id: 'al-4', ruleName: 'Vibration élevée', equipmentId: 'eq-02', metric: 'vibration', value: 6.9, threshold: 6.5, severity: 'CRITICAL', status: 'RESOLVED', message: 'Vibration au-dessus du seuil', triggeredAt: new Date(Date.now() - 26 * 3600000).toISOString() },
  ];

  workOrders: WorkOrder[] = [
    { id: 'wo-1', equipmentId: 'eq-03', title: 'Remplacer roulement compresseur', description: 'Usure avancée détectée par capteur', priority: 'HIGH', status: 'IN_PROGRESS', source: 'ALERT', workType: 'CORRECTIVE', assignedToUserId: 'u-1', dueAt: new Date(Date.now() + 4 * 3600000).toISOString(), startedAt: new Date(Date.now() - 30 * 60000).toISOString(), spareParts: 'Roulement 6205-2RS', costEstimate: 420, createdAt: new Date(Date.now() - 5 * 3600000).toISOString(), updatedAt: new Date(Date.now() - 30 * 60000).toISOString(), overdue: false },
    { id: 'wo-2', equipmentId: 'eq-04', title: 'Aligner axes robot', description: 'Dérive de position détectée', priority: 'CRITICAL', status: 'CREATED', source: 'ALERT', workType: 'CORRECTIVE', assignedToUserId: 'u-2', dueAt: new Date(Date.now() + 2 * 3600000).toISOString(), costEstimate: 260, createdAt: new Date(Date.now() - 1 * 3600000).toISOString(), updatedAt: new Date(Date.now() - 1 * 3600000).toISOString(), overdue: false },
    { id: 'wo-3', equipmentId: 'eq-01', title: 'Maintenance préventive tour CNC', description: 'Contrôle trimestriel', priority: 'MEDIUM', status: 'ASSIGNED', source: 'SCHEDULE', workType: 'PREVENTIVE', assignedToUserId: 'u-2', dueAt: new Date(Date.now() + 24 * 3600000).toISOString(), costEstimate: 150, createdAt: new Date(Date.now() - 2 * 24 * 3600000).toISOString(), updatedAt: new Date(Date.now() - 20 * 3600000).toISOString(), overdue: false },
    { id: 'wo-4', equipmentId: 'eq-05', title: 'Inspection frein presse', priority: 'LOW', status: 'COMPLETED', source: 'MANUAL', workType: 'INSPECTION', assignedToUserId: 'u-1', completedAt: new Date(Date.now() - 18 * 3600000).toISOString(), completionNotes: 'RAS, conforme', createdAt: new Date(Date.now() - 40 * 3600000).toISOString(), updatedAt: new Date(Date.now() - 18 * 3600000).toISOString(), overdue: false },
    { id: 'wo-5', equipmentId: 'eq-06', title: 'Réparer fuite hydraulique', priority: 'HIGH', status: 'CREATED', source: 'ALERT', workType: 'CORRECTIVE', dueAt: new Date(Date.now() + 6 * 3600000).toISOString(), costEstimate: 640, createdAt: new Date(Date.now() - 8 * 3600000).toISOString(), updatedAt: new Date(Date.now() - 8 * 3600000).toISOString(), overdue: true },
  ];

  schedules: MaintenanceSchedule[] = [
    { id: 'sch-1', equipmentId: 'eq-01', title: 'Vidange et graissage trimestriel', workType: 'PREVENTIVE', priority: 'MEDIUM', intervalDays: 90, nextRunAt: new Date(Date.now() + 6 * 24 * 3600000).toISOString(), lastRunAt: new Date(Date.now() - 84 * 24 * 3600000).toISOString(), active: true, createdAt: new Date(Date.now() - 200 * 24 * 3600000).toISOString() },
    { id: 'sch-2', equipmentId: 'eq-02', title: 'Contrôle sécurité semestriel', workType: 'INSPECTION', priority: 'HIGH', intervalDays: 180, nextRunAt: new Date(Date.now() + 12 * 24 * 3600000).toISOString(), lastRunAt: new Date(Date.now() - 168 * 24 * 3600000).toISOString(), active: true, createdAt: new Date(Date.now() - 300 * 24 * 3600000).toISOString() },
  ];

  reports: ReportSummary[] = [
    { id: 'rp-1', kind: 'PDF', status: 'READY', title: 'Rapport température - 24h', rangeFrom: new Date(Date.now() - 24 * 3600000).toISOString(), rangeTo: new Date().toISOString(), fileName: 'temperature-24h.pdf', sizeBytes: 84_321, createdAt: new Date(Date.now() - 2 * 3600000).toISOString() },
    { id: 'rp-2', kind: 'CSV', status: 'READY', title: 'Séries vibration - 7j', rangeFrom: new Date(Date.now() - 7 * 24 * 3600000).toISOString(), rangeTo: new Date().toISOString(), fileName: 'vibration-7d.csv', sizeBytes: 12_045, createdAt: new Date(Date.now() - 26 * 3600000).toISOString() },
  ];

  // ---- Telemetry ----
  telemetryRaw(from: string, to: string): Observable<MetricPoint[]> {
    return of(this.walk('raw', from, to));
  }

  telemetryAggregate(bucket: string, from: string, to: string): Observable<MetricPoint[]> {
    return of(this.walk('aggregate', from, to));
  }

  telemetryLatest(): Observable<MetricPoint[]> {
    const now = Date.now();
    const points = ['temperature', 'vibration', 'pressure', 'rpm', 'current'].map((metric) => ({
      timestamp: new Date(now - Math.floor(Math.random() * 4000)).toISOString(),
      value: this.baseValue(metric) + nextNoise(metric),
      metric,
    }));
    return of(points);
  }

  private walk(kind: string, from: string, to: string): MetricPoint[] {
    const fromMs = new Date(from).getTime();
    const toMs = new Date(to).getTime();
    if (!Number.isFinite(fromMs) || !Number.isFinite(toMs) || toMs <= fromMs) {
      return [];
    }
    const metrics = ['temperature', 'vibration', 'pressure', 'rpm', 'current'];
    const stepMs = kind === 'aggregate' ? 15 * 60000 : 60000;
    const points: MetricPoint[] = [];
    for (const metric of metrics) {
      let value = this.baseValue(metric);
      for (let t = fromMs; t <= toMs; t += stepMs) {
        value += nextNoise(metric);
        value = Math.max(this.minValue(metric), Math.min(this.maxValue(metric), value));
        points.push({ timestamp: new Date(t).toISOString(), value: round2(value), metric });
      }
    }
    return points;
  }

  // ---- Fleet ----
  fleetEquipment(page: number, size: number): Observable<{ content: Equipment[]; totalElements: number; totalPages: number }> {
    const start = page * size;
    const content = this.equipment.slice(start, start + size);
    return of({ content, totalElements: this.equipment.length, totalPages: Math.ceil(this.equipment.length / size) });
  }

  fleetEquipmentById(id: string): Observable<Equipment> {
    const eq = this.equipment.find((e) => e.id === id) ?? this.equipment[0];
    return of(eq);
  }

  fleetCreateEquipment(payload: Record<string, unknown>): Observable<Equipment> {
    const id = `eq-${String(this.equipment.length + 1).padStart(2, '0')}`;
    const model = this.models.find((m) => m.id === payload['modelId']) ?? { id: String(payload['modelId']), name: String(payload['modelId']) };
    const site = this.sites.find((s) => s.id === payload['siteId']) ?? { id: String(payload['siteId']), name: String(payload['siteId']), city: '', country: '' };
    const eq: Equipment = {
      id,
      name: String(payload['name'] ?? 'Équipement'),
      serialNumber: String(payload['serialNumber'] ?? ''),
      status: 'ACTIVE',
      modelId: model.id,
      modelName: model.name,
      siteId: site.id,
      siteName: site.name,
      lastSeenAt: new Date().toISOString(),
    };
    this.equipment.unshift(eq);
    return of(eq);
  }

  fleetSites(): Observable<{ content: Site[]; totalElements: number; totalPages: number }> {
    return of({ content: this.sites, totalElements: this.sites.length, totalPages: 1 });
  }

  fleetModels(): Observable<{ content: { id: string; name: string }[]; totalElements: number; totalPages: number }> {
    return of({ content: this.models, totalElements: this.models.length, totalPages: 1 });
  }
  fleetCreateSite(payload: Record<string, unknown>): Observable<Site> {
    const site: Site = {
      id: `site-${this.sites.length + 1}`,
      name: String(payload?.['name'] ?? 'Locale'),
      city: String(payload?.['city'] ?? ''),
      country: String(payload?.['country'] ?? ''),
    };
    this.sites.push(site);
    return of(site);
  }


  // ---- Users ----
  userList(): Observable<UserSummary[]> {
    return of([...this.users]);
  }

  // ---- Maintenance ----
  maintenanceOrders(status?: WorkOrderStatus): Observable<{ content: WorkOrder[]; totalElements: number; totalPages: number }> {
    const list = status ? this.workOrders.filter((w) => w.status === status) : this.workOrders;
    return of({ content: list, totalElements: list.length, totalPages: 1 });
  }

  maintenanceOrdersHistory(): Observable<{ content: WorkOrder[]; totalElements: number; totalPages: number }> {
    const done = this.workOrders.filter((w) => w.status === 'COMPLETED' || w.status === 'CANCELLED');
    return of({ content: done, totalElements: done.length, totalPages: 1 });
  }

  maintenanceSchedules(): Observable<MaintenanceSchedule[]> {
    return of([...this.schedules]);
  }

  costs(): Observable<CostTrend> {
    const months = ['Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Juin', 'Juil', 'Août', 'Sep'].map((m, i) => ({
      month: m,
      orders: 3 + i,
      totalCost: Math.round((3200 + i * 900 + Math.random() * 400) * 10) / 10,
    }));
    const last = months[months.length - 1];
    last.totalCost = 5400 + Math.random() * 300;
    return of({ months });
  }

  kpi(): Observable<MaintenanceKpi> {
    const open = this.workOrders.filter((w) => w.status === 'CREATED' || w.status === 'ASSIGNED').length;
    const inProgress = this.workOrders.filter((w) => w.status === 'IN_PROGRESS').length;
    const overdue = this.workOrders.filter((w) => w.overdue && w.status !== 'COMPLETED' && w.status !== 'CANCELLED').length;
    const completedToday = this.workOrders.filter((w) => {
      if (w.status !== 'COMPLETED' || !w.completedAt) return false;
      const d = new Date(w.completedAt);
      const today = new Date();
      return d.toDateString() === today.toDateString();
    }).length;
    return of({
      created: this.workOrders.length,
      assigned: this.workOrders.filter((w) => w.status === 'ASSIGNED').length,
      inProgress,
      open,
      overdue,
      completedToday,
      completed: this.workOrders.filter((w) => w.status === 'COMPLETED').length,
      downtimeTodayMinutes: 47 + Math.round(Math.random() * 30),
    });
  }

  oee(): Observable<GlobalOee> {
    const from = new Date(Date.now() - 24 * 3600000).toISOString();
    const to = new Date().toISOString();
    return of({
      from,
      to,
      totalMinutes: 1440,
      uptimeMinutes: 1350 + Math.round(Math.random() * 40),
      downtimeMinutes: 90 - Math.round(Math.random() * 40),
      availabilityPercent: 92.4 + Math.round(Math.random() * 40) / 10,
      downtimeCount: 6,
      completedOrders: this.workOrders.filter((w) => w.status === 'COMPLETED').length,
      createdOrders: this.workOrders.length,
      completionRatePercent: 78 + Math.round(Math.random() * 10),
    });
  }

  availability(equipmentId: string): Observable<Availability> {
    return of({
      equipmentId,
      from: new Date(Date.now() - 24 * 3600000).toISOString(),
      to: new Date().toISOString(),
      totalMinutes: 1440,
      uptimeMinutes: 1360,
      downtimeMinutes: 80,
      availabilityPercent: 94.4,
      downtimeCount: 4,
    });
  }

  createWorkOrder(payload: Record<string, unknown>): Observable<WorkOrder> {
    const id = `wo-${this.workOrders.length + 1}`;
    const wo: WorkOrder = {
      id,
      equipmentId: String(payload['equipmentId'] ?? ''),
      title: String(payload['title'] ?? 'Ordre de travail'),
      description: payload['description'] ? String(payload['description']) : undefined,
      priority: (payload['priority'] as WorkOrderPriority) ?? 'MEDIUM',
      status: 'CREATED',
      source: 'MANUAL',
      workType: (payload['workType'] as WorkOrderType) ?? 'PREVENTIVE',
      dueAt: payload['dueAt'] ? String(payload['dueAt']) : undefined,
      costEstimate: payload['costEstimate'] ? Number(payload['costEstimate']) : undefined,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      overdue: false,
    };
    this.workOrders.unshift(wo);
    return of(wo);
  }

  assignWorkOrder(id: string, assignedToUserId: string): Observable<WorkOrder> {
    const wo = this.workOrders.find((w) => w.id === id);
    if (wo) {
      wo.assignedToUserId = assignedToUserId;
      if (wo.status === 'CREATED') wo.status = 'ASSIGNED';
      wo.updatedAt = new Date().toISOString();
    }
    return of(wo ?? this.workOrders[0]);
  }

  startWorkOrder(id: string): Observable<WorkOrder> {
    const wo = this.workOrders.find((w) => w.id === id);
    if (wo) {
      wo.status = 'IN_PROGRESS';
      wo.startedAt = new Date().toISOString();
      wo.updatedAt = new Date().toISOString();
    }
    return of(wo ?? this.workOrders[0]);
  }

  completeWorkOrder(id: string, completionNotes?: string): Observable<WorkOrder> {
    const wo = this.workOrders.find((w) => w.id === id);
    if (wo) {
      wo.status = 'COMPLETED';
      wo.completedAt = new Date().toISOString();
      wo.completionNotes = completionNotes ?? wo.completionNotes;
      wo.updatedAt = new Date().toISOString();
    }
    return of(wo ?? this.workOrders[0]);
  }

  cancelWorkOrder(id: string): Observable<WorkOrder> {
    const wo = this.workOrders.find((w) => w.id === id);
    if (wo) {
      wo.status = 'CANCELLED';
      wo.updatedAt = new Date().toISOString();
    }
    return of(wo ?? this.workOrders[0]);
  }

  createSchedule(payload: Record<string, unknown>): Observable<MaintenanceSchedule> {
    const sch: MaintenanceSchedule = {
      id: `sch-${this.schedules.length + 1}`,
      equipmentId: String(payload['equipmentId'] ?? ''),
      title: String(payload['title'] ?? 'Planification'),
      workType: (payload['workType'] as WorkOrderType) ?? 'PREVENTIVE',
      priority: (payload['priority'] as WorkOrderPriority) ?? 'MEDIUM',
      intervalDays: Number(payload['intervalDays'] ?? 30),
      nextRunAt: new Date(Date.now() + 30 * 24 * 3600000).toISOString(),
      active: true,
      createdAt: new Date().toISOString(),
    };
    this.schedules.push(sch);
    return of(sch);
  }

  deleteSchedule(id: string): Observable<void> {
    this.schedules = this.schedules.filter((s) => s.id !== id);
    return of(undefined);
  }

  // ---- Alerts ----
  alertEvents(status?: string, severity?: string): Observable<{ content: AlertEvent[]; totalElements: number; totalPages: number }> {
    let list = this.alerts;
    if (status) list = list.filter((a) => a.status === status);
    if (severity) list = list.filter((a) => a.severity === severity);
    return of({ content: list, totalElements: list.length, totalPages: 1 });
  }

  acknowledge(id: string): Observable<AlertEvent> {
    const alert = this.alerts.find((a) => a.id === id);
    if (alert) alert.status = 'ACKNOWLEDGED';
    return of(alert ?? this.alerts[0]);
  }

  resolve(id: string): Observable<AlertEvent> {
    const alert = this.alerts.find((a) => a.id === id);
    if (alert) alert.status = 'RESOLVED';
    return of(alert ?? this.alerts[0]);
  }

  alertRules(): Observable<AlertRule[]> {
    return of([...this.rules]);
  }

  createRule(payload: Record<string, unknown>): Observable<AlertRule> {
    const rule: AlertRule = {
      id: `rule-${this.rules.length + 1}`,
      name: String(payload['name'] ?? 'Règle'),
      metric: String(payload['metric'] ?? 'temperature'),
      operator: String(payload['operator'] ?? 'GT'),
      threshold: Number(payload['threshold'] ?? 0),
      severity: String(payload['severity'] ?? 'WARNING'),
      enabled: true,
    };
    this.rules.push(rule);
    return of(rule);
  }

  setRuleEnabled(id: string, enabled: boolean): Observable<AlertRule> {
    const rule = this.rules.find((r) => r.id === id);
    if (rule) rule.enabled = enabled;
    return of(rule ?? this.rules[0]);
  }

  // ---- Reports ----
  reportsList(): Observable<{ content: ReportSummary[]; totalElements: number; totalPages: number }> {
    const sorted = [...this.reports].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
    return of({ content: sorted, totalElements: sorted.length, totalPages: 1 });
  }

  generateReport(payload: Record<string, unknown>): Observable<ReportSummary> {
    const kind = payload['kind'] === 'PDF' ? 'PDF' : 'CSV';
    const title = payload['title'] ? String(payload['title']) : `Rapport ${payload['metric'] ?? 'données'}`;
    const report: ReportSummary = {
      id: `rp-${this.reports.length + 1}`,
      kind,
      status: 'READY',
      title,
      rangeFrom: payload['from'] ? String(payload['from']) : new Date(Date.now() - 24 * 3600000).toISOString(),
      rangeTo: payload['to'] ? String(payload['to']) : new Date().toISOString(),
      fileName: `${(payload['metric'] ?? 'rapport').toString().toLowerCase()}-${Date.now()}.${kind.toLowerCase()}`,
      sizeBytes: Math.round(8000 + Math.random() * 40000),
      createdAt: new Date().toISOString(),
    };
    this.reports.unshift(report);
    return of(report);
  }

  downloadReport(id: string): Observable<Blob> {
    const report = this.reports.find((r) => r.id === id);
    const lines = [
      'timestamp,metric,value',
      ...Array.from({ length: 30 }, (_, i) => ({
        t: new Date(Date.now() - (30 - i) * 60000).toISOString(),
        m: report?.title ?? 'telemetry',
        v: Math.round((40 + Math.random() * 30) * 10) / 10,
      })).map((r) => `${r.t},${r.m},${r.v}`),
    ].join('\n');
    const blob = new Blob([lines], { type: report?.kind === 'PDF' ? 'application/pdf' : 'text/csv' });
    return of(blob);
  }

  // ---- helpers ----
  private baseValue(metric: string): number {
    switch (metric) {
      case 'temperature': return 52 + Math.random() * 6;
      case 'vibration': return 4.2 + Math.random() * 1.2;
      case 'pressure': return 5 + Math.random();
      case 'rpm': return 1450 + Math.random() * 80;
      case 'current': return 32 + Math.random() * 6;
      default: return 50;
    }
  }

  private minValue(metric: string): number {
    switch (metric) { case 'vibration': return 0; case 'pressure': return 2; case 'rpm': return 200; case 'current': return 0; default: return 0; }
  }

  private maxValue(metric: string): number {
    switch (metric) { case 'temperature': return 95; case 'vibration': return 10; case 'pressure': return 8; case 'rpm': return 1800; case 'current': return 60; default: return 100; }
  }
}

function nextNoise(metric: string): number {
  switch (metric) {
    case 'temperature': return (Math.random() - 0.5) * 3;
    case 'vibration': return (Math.random() - 0.5) * 0.7;
    case 'pressure': return (Math.random() - 0.5) * 0.4;
    case 'rpm': return (Math.random() - 0.5) * 20;
    case 'current': return (Math.random() - 0.5) * 2.4;
    default: return (Math.random() - 0.5);
  }
}

function round2(value: number): number {
  return Math.round(value * 100) / 100;
}


