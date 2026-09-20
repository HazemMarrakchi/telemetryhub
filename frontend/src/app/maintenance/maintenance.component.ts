import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { MaintenanceApi } from '../core/services/maintenance-api';
import { FleetApi } from '../core/services/fleet-api';
import { UsersApi } from '../core/services/users-api';
import { Equipment, GlobalOee } from '../core/models';
import { MaintenanceKpi, MaintenanceSchedule, MonthlyCost, UserSummary, WorkOrder, WorkOrderPriority, WorkOrderStatus, WorkOrderType } from '../core/models';

interface CostBar {
  x: number;
  y: number;
  w: number;
  h: number;
  value: number;
  count: number;
  month: string;
  label: string;
}

type MaintenanceView = 'orders' | 'history' | 'preventive';

@Component({
  selector: 'th-maintenance',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  template: `
    <h1 class="page-title">Maintenance</h1>
    <p class="muted">Ordres de travail CMMS : les alarmes critiques créent automatiquement une intervention et ouvrent un temps d'arrêt.</p>

    <div class="tabs">
      <button type="button" [class.active]="view() === 'orders'" (click)="view.set('orders')">Ordres de travail</button>
      <button type="button" [class.active]="view() === 'history'" (click)="view.set('history')">Historique & tends.</button>
      <button type="button" [class.active]="view() === 'preventive'" (click)="view.set('preventive')">Maintenance préventive</button>
    </div>

    <div *ngIf="view() === 'orders'">
      <div class="kpi-grid">
        <div class="card kpi">
          <strong>{{ kpi().open }}</strong>
          <span>Ordres ouverts</span>
        </div>
        <div class="card kpi">
          <strong>{{ kpi().inProgress }}</strong>
          <span>En cours</span>
        </div>
        <div class="card kpi">
          <strong class="danger-text">{{ kpi().overdue }}</strong>
          <span>En retard</span>
        </div>
        <div class="card kpi">
          <strong>{{ kpi().completedToday }}</strong>
          <span>Terminés aujourd'hui</span>
        </div>
        <div class="card kpi">
          <strong>{{ kpi().downtimeTodayMinutes }}'</strong>
          <span>Arrêt aujourd'hui</span>
        </div>
      </div>

      <div class="kpi-grid" *ngIf="oee()">
        <div class="card kpi">
          <strong>{{ oee()!.availabilityPercent }}%</strong>
          <span>Disponibilité (24 h)</span>
        </div>
        <div class="card kpi">
          <strong>{{ oee()!.downtimeMinutes }}'</strong>
          <span>Arrêts (24 h, {{ oee()!.downtimeCount }})</span>
        </div>
        <div class="card kpi">
          <strong>{{ oee()!.completionRatePercent }}%</strong>
          <span>Taux de clôture ({{ oee()!.completedOrders }}/{{ oee()!.createdOrders }})</span>
        </div>
      </div>

      <div class="panel">
        <h3>Créer un ordre de travail</h3>
        <form [formGroup]="form" (ngSubmit)="create()">
          <div class="form-grid">
            <input formControlName="title" placeholder="Titre (ex: remplacer le roulement)" />
            <select formControlName="equipmentId">
              <option value="">Sélectionner une machine…</option>
              <option *ngFor="let eq of equipments()" [value]="eq.id">{{ eq.name }}</option>
            </select>
            <select formControlName="priority">
              <option *ngFor="let p of priorities" [value]="p">{{ p }}</option>
            </select>
            <select formControlName="workType">
              <option *ngFor="let t of workTypes" [value]="t">{{ t }}</option>
            </select>
            <input formControlName="dueAt" type="datetime-local" />
          </div>
          <div class="form-grid mt">
            <input formControlName="description" placeholder="Description (optionnel)" />
            <button type="submit" [disabled]="form.invalid">Créer</button>
          </div>
        </form>
      </div>

      <div class="toolbar">
        <select [value]="filterStatus()" (change)="setFilter($event)" class="wide-auto">
          <option value="">Tous les statuts</option>
          <option *ngFor="let s of statuses" [value]="s">{{ s }}</option>
        </select>
        <button class="secondary" (click)="load()">Actualiser</button>
      </div>

      <div class="card">
        <table>
          <thead>
            <tr><th>Ordre</th><th>Machine</th><th>Type</th><th>Priorité</th><th>Statut</th><th>Technicien</th><th>Échéance</th><th>Actions</th></tr>
          </thead>
          <tbody>
            <tr *ngFor="let wo of orders()">
              <td>
                <strong>{{ wo.title }}</strong>
                <span class="badge info" *ngIf="wo.source === 'ALERT'">auto</span>
                <span class="badge warn" *ngIf="wo.source === 'SCHEDULE'">planifié</span>
                <div class="muted small">{{ wo.description || '—' }}</div>
              </td>
              <td>{{ equipmentName(wo.equipmentId) }}</td>
              <td><span class="badge" [ngClass]="typeClass(wo.workType)">{{ wo.workType }}</span></td>
              <td><span class="badge" [ngClass]="priorityClass(wo.priority)">{{ wo.priority }}</span></td>
              <td>
                <span class="badge" [ngClass]="statusClass(wo.status)">{{ wo.status }}</span>
                <span class="badge danger" *ngIf="wo.overdue">retard</span>
              </td>
              <td>
                <select *ngIf="!wo.assignedToUserId && !isClosed(wo.status)"
                        [ngModel]="''" (ngModelChange)="assign(wo, $event)" class="small">
                  <option value="">—</option>
                  <option *ngFor="let u of users()" [value]="u.id">{{ u.fullName }}</option>
                </select>
                <span *ngIf="wo.assignedToUserId" class="badge success">✔ {{ userName(wo.assignedToUserId) }}</span>
                <span *ngIf="isClosed(wo.status) && !wo.assignedToUserId" class="muted">—</span>
              </td>
              <td class="muted">{{ wo.dueAt ? (wo.dueAt | date: 'dd/MM HH:mm') : '—' }}</td>
              <td>
                <div *ngIf="wo.status === 'IN_PROGRESS'" class="inline">
                  <input [ngModel]="notes()[wo.id] || ''" (ngModelChange)="setNotes(wo.id, $event)"
                         placeholder="Notes de clôture" class="small notes-input" />
                </div>
                <button class="secondary" *ngIf="wo.status === 'CREATED' || wo.status === 'ASSIGNED'" (click)="startWo(wo.id)">Démarrer</button>
                <button class="secondary" *ngIf="wo.status === 'IN_PROGRESS'" (click)="completeWo(wo.id)">Terminer</button>
                <button class="secondary" *ngIf="!isClosed(wo.status)" (click)="cancelWo(wo.id)">Annuler</button>
                <div class="muted small" *ngIf="wo.completionNotes">✓ {{ wo.completionNotes }}</div>
              </td>
            </tr>
          </tbody>
        </table>
        <div class="empty-state" *ngIf="!orders().length">Aucun ordre de travail.</div>
      </div>
    </div>

    <div *ngIf="view() === 'history'">
      <div class="card mt">
        <h3 class="card-title">Tendances de coûts — 12 derniers mois (ordres clôturés, €)</h3>
        <svg [attr.viewBox]="'0 0 ' + costWidth + ' ' + costHeight" preserveAspectRatio="none" class="cost-chart">
          <g *ngFor="let bar of costBars()">
            <rect [attr.x]="bar.x" [attr.y]="bar.y" [attr.width]="bar.w" [attr.height]="bar.h" rx="3" class="cost-bar" />
            <text class="cost-val" [attr.x]="bar.x + bar.w / 2" [attr.y]="bar.y - 6" text-anchor="middle" *ngIf="bar.label">{{ bar.label }}€</text>
            <text class="cost-axis" [attr.x]="bar.x + bar.w / 2" [attr.y]="costHeight - 8" text-anchor="middle">{{ bar.month }}</text>
          </g>
        </svg>
        <div class="empty-state" *ngIf="costTotal() === 0">Aucune donnée de coûts sur la période.</div>
      </div>

      <div class="card mt">
        <h3 class="card-title">Historique des interventions clôturées</h3>
        <table>
          <thead>
            <tr><th>Clôturé le</th><th>Intervention</th><th>Machine</th><th>Type</th><th>Technicien</th><th>Coût estimé</th><th>Notes</th></tr>
          </thead>
          <tbody>
            <tr *ngFor="let wo of history()">
              <td class="muted">{{ (wo.completedAt || wo.updatedAt) | date: 'dd/MM/yyyy HH:mm' }}</td>
              <td>
                <strong>{{ wo.title }}</strong>
                <span class="badge info" *ngIf="wo.source === 'ALERT'">auto</span>
                <span class="badge warn" *ngIf="wo.source === 'SCHEDULE'">planifié</span>
                <div class="muted small">{{ wo.description || '—' }}</div>
              </td>
              <td>{{ equipmentName(wo.equipmentId) }}</td>
              <td><span class="badge" [ngClass]="typeClass(wo.workType)">{{ wo.workType }}</span></td>
              <td>{{ userName(wo.assignedToUserId ?? null) }}</td>
              <td>{{ wo.costEstimate != null ? wo.costEstimate.toLocaleString('fr-FR') + ' €' : '—' }}</td>
              <td>
                <div class="muted small" *ngIf="wo.completionNotes">✓ {{ wo.completionNotes }}</div>
                <span class="muted" *ngIf="!wo.completionNotes">—</span>
              </td>
            </tr>
          </tbody>
        </table>
        <div class="empty-state" *ngIf="!history().length">Aucune intervention clôturée.</div>
      </div>
    </div>

    <div *ngIf="view() === 'preventive'">
      <div class="panel">
        <h3>Planifier une maintenance préventive récurrente</h3>
        <p class="muted small">Le système génère automatiquement un ordre de travail à chaque échéance (source « planifié »).</p>
        <form [formGroup]="scheduleForm" (ngSubmit)="createSchedule()">
          <div class="form-grid">
            <select formControlName="equipmentId">
              <option value="">Sélectionner une machine…</option>
              <option *ngFor="let eq of equipments()" [value]="eq.id">{{ eq.name }}</option>
            </select>
            <input formControlName="title" placeholder="Titre (ex: vidange mensuelle)" />
            <input formControlName="intervalDays" type="number" min="1" placeholder="Fréquence (jours)" />
            <select formControlName="workType">
              <option *ngFor="let t of workTypes" [value]="t">{{ t }}</option>
            </select>
            <select formControlName="priority">
              <option *ngFor="let p of priorities" [value]="p">{{ p }}</option>
            </select>
          </div>
          <div class="form-grid mt">
            <input formControlName="description" placeholder="Description (optionnel)" />
            <button type="submit" [disabled]="scheduleForm.invalid">Créer le planning</button>
          </div>
        </form>
      </div>

      <div class="card mt">
        <h3 class="card-title">Plannings actifs</h3>
        <table>
          <thead>
            <tr><th>Planning</th><th>Machine</th><th>Fréquence</th><th>Type</th><th>Prochaine génération</th><th>Dernière génération</th><th></th></tr>
          </thead>
          <tbody>
            <tr *ngFor="let s of schedules()">
              <td>
                <strong>{{ s.title }}</strong>
                <div class="muted small">{{ s.description || '—' }}</div>
              </td>
              <td>{{ equipmentName(s.equipmentId) }}</td>
              <td><span class="badge info">tous les {{ s.intervalDays }} j</span></td>
              <td><span class="badge" [ngClass]="typeClass(s.workType)">{{ s.workType }}</span></td>
              <td>{{ s.nextRunAt | date: 'dd/MM/yyyy HH:mm' }}</td>
              <td class="muted">{{ s.lastRunAt ? (s.lastRunAt | date: 'dd/MM HH:mm') : '—' }}</td>
              <td><button class="secondary" (click)="deleteSchedule(s.id)">Supprimer</button></td>
            </tr>
          </tbody>
        </table>
        <div class="empty-state" *ngIf="!schedules().length">Aucun planning préventif. Créez-en un ci-dessus.</div>
      </div>
    </div>
  `,
  styles: `
    .tabs { display: flex; gap: 8px; margin-bottom: 16px; border-bottom: 1px solid var(--th-border); }
    .tabs button { background: transparent; border: 1px solid transparent; border-radius: var(--th-radius) var(--th-radius) 0 0; padding: 10px 16px; color: var(--th-muted); cursor: pointer; font-size: 0.95rem; }
    .tabs button:hover { color: var(--th-text); }
    .tabs button.active { color: var(--th-primary); border-color: var(--th-border); border-bottom-color: transparent; background: var(--th-surface); font-weight: 600; }
    .kpi-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 12px; margin-bottom: 16px; }
    .kpi { display: flex; flex-direction: column; gap: 4px; }
    .kpi strong { font-size: 1.6rem; }
    .danger-text { color: var(--th-danger, #dc2626); }
    .toolbar { display: flex; gap: 8px; align-items: center; margin: 12px 0; }
    .wide-auto { min-width: 220px; }
    select.small { max-width: 160px; }
    .small { font-size: 0.85rem; }
    .inline { display: flex; gap: 6px; align-items: center; }
    .notes-input { min-width: 180px; }
    .mt { margin-top: 12px; }
    .card-title { margin: 0 0 12px; font-size: 1rem; }
    .cost-chart { width: 100%; max-width: 720px; height: 200px; overflow: visible; }
    .cost-bar { fill: var(--th-primary); }
    .cost-val { fill: var(--th-text); font-size: 10px; }
    .cost-axis { fill: var(--th-muted); font-size: 10px; }
  `,
})
export class MaintenanceComponent implements OnInit {
  private api = inject(MaintenanceApi);
  private fleetApi = inject(FleetApi);
  private usersApi = inject(UsersApi);
  private fb = inject(FormBuilder);
  private destroyRef = inject(DestroyRef);

  readonly statuses: WorkOrderStatus[] = ['CREATED', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'];
  readonly priorities: WorkOrderPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
  readonly workTypes: WorkOrderType[] = ['PREVENTIVE', 'CORRECTIVE', 'INSPECTION'];

  readonly costWidth = 720;
  readonly costHeight = 200;

  view = signal<MaintenanceView>('orders');

  form = this.fb.nonNullable.group({
    title: ['', Validators.required],
    equipmentId: ['', Validators.required],
    priority: ['MEDIUM' as WorkOrderPriority, Validators.required],
    workType: ['PREVENTIVE' as WorkOrderType, Validators.required],
    description: [''],
    dueAt: [''],
  });

  scheduleForm = this.fb.nonNullable.group({
    equipmentId: ['', Validators.required],
    title: ['', Validators.required],
    intervalDays: [30, [Validators.required, Validators.min(1)]],
    workType: ['PREVENTIVE' as WorkOrderType, Validators.required],
    priority: ['MEDIUM' as WorkOrderPriority, Validators.required],
    description: [''],
  });

  orders = signal<WorkOrder[]>([]);
  history = signal<WorkOrder[]>([]);
  schedules = signal<MaintenanceSchedule[]>([]);
  costs = signal<MonthlyCost[]>([]);
  costBars = signal<CostBar[]>([]);
  kpi = signal<MaintenanceKpi>({
    created: 0, assigned: 0, inProgress: 0, open: 0, overdue: 0, completedToday: 0, completed: 0, downtimeTodayMinutes: 0,
  });
  oee = signal<GlobalOee | null>(null);
  notes = signal<Record<string, string>>({});
  users = signal<UserSummary[]>([]);
  equipments = signal<Equipment[]>([]);
  filterStatus = signal<WorkOrderStatus | ''>('');
  private equipmentNames = new Map<string, string>();

  ngOnInit(): void {
    this.load();
    const timer = setInterval(() => this.load(), 20000);
    this.destroyRef.onDestroy(() => clearInterval(timer));
  }

  load(): void {
    this.api.workOrders(this.filterStatus() || undefined).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (page) => this.orders.set(page.content),
    });
    this.api.history().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (page) => this.history.set(page.content),
    });
    this.api.schedules().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (list) => this.schedules.set(list),
    });
    this.api.costs().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (trend) => {
        this.costs.set(trend.months);
        this.costBars.set(this.buildCostBars(trend.months));
      },
    });
    this.api.kpi().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (k) => this.kpi.set(k),
    });
    this.api.oee().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (o) => this.oee.set(o),
    });
    this.usersApi.users().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (users) => this.users.set(users),
    });
    if (!this.equipmentNames.size) {
      this.fleetApi.equipment(0, 100).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: (page) => {
          const map = new Map<string, string>();
          for (const eq of page.content) {
            map.set(eq.id, eq.name);
          }
          this.equipmentNames = map;
          this.equipments.set(page.content);
        },
      });
    }
  }

  setFilter(event: Event): void {
    const value = (event.target as HTMLSelectElement).value as WorkOrderStatus | '';
    this.filterStatus.set(value);
    this.load();
  }

  equipmentName(id: string | undefined): string {
    return id ? this.equipmentNames.get(id) ?? id.slice(0, 8) : '—';
  }

  isClosed(status: WorkOrderStatus): boolean {
    return status === 'COMPLETED' || status === 'CANCELLED';
  }

  costTotal(): number {
    return this.costs().reduce((sum, c) => sum + c.totalCost, 0);
  }

  private buildCostBars(months: MonthlyCost[]): CostBar[] {
    const byMonth = new Map(months.map((m) => [m.month, m]));
    const buckets: { month: string; value: number; count: number }[] = [];
    const now = new Date();
    for (let i = 11; i >= 0; i--) {
      const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
      const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
      const data = byMonth.get(key);
      buckets.push({
        month: String(d.getMonth() + 1).padStart(2, '0'),
        value: data?.totalCost ?? 0,
        count: data?.orders ?? 0,
      });
    }
    const paddingLeft = 44;
    const paddingBottom = 24;
    const paddingTop = 18;
    const innerWidth = this.costWidth - paddingLeft - 12;
    const innerHeight = this.costHeight - paddingTop - paddingBottom;
    const max = Math.max(1, ...buckets.map((b) => b.value));
    const step = innerWidth / buckets.length;
    const barWidth = step * 0.6;
    return buckets.map((b, i) => {
      const height = Math.max(2, (b.value / max) * innerHeight);
      return {
        x: paddingLeft + i * step + (step - barWidth) / 2,
        y: paddingTop + innerHeight - height,
        w: barWidth,
        h: height,
        value: b.value,
        count: b.count,
        month: b.month,
        label: b.value > 0 ? Math.round(b.value).toLocaleString('fr-FR') : '',
      };
    });
  }

  create(): void {
    if (this.form.invalid) {
      return;
    }
    const value = this.form.getRawValue();
    const payload: Record<string, unknown> = {
      title: value.title,
      equipmentId: value.equipmentId,
      priority: value.priority,
      workType: value.workType,
      description: value.description || undefined,
    };
    if (value.dueAt) {
      payload['dueAt'] = new Date(value.dueAt as string).toISOString();
    }
    this.api.createWorkOrder(payload).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.form.reset({ priority: 'MEDIUM', workType: 'PREVENTIVE' });
      this.load();
    });
  }

  createSchedule(): void {
    if (this.scheduleForm.invalid) {
      return;
    }
    const value = this.scheduleForm.getRawValue();
    const payload: Record<string, unknown> = {
      title: value.title,
      equipmentId: value.equipmentId,
      intervalDays: Number(value.intervalDays),
      workType: value.workType,
      priority: value.priority,
      description: value.description || undefined,
    };
    this.api.createSchedule(payload).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.scheduleForm.reset({
        equipmentId: '', title: '', intervalDays: 30, workType: 'PREVENTIVE', priority: 'MEDIUM', description: '',
      });
      this.load();
    });
  }

  deleteSchedule(id: string): void {
    this.api.deleteSchedule(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.load());
  }

  assign(wo: WorkOrder, technicianId: string): void {
    if (!technicianId) {
      return;
    }
    this.api.assignWorkOrder(wo.id, technicianId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.load());
  }

  userName(id: string | null): string {
    if (!id) {
      return '—';
    }
    const user = this.users().find((u) => u.id === id);
    return user ? user.fullName : id.slice(0, 8);
  }

  startWo(id: string): void {
    this.api.startWorkOrder(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.load());
  }

  completeWo(id: string): void {
    this.api.completeWorkOrder(id, this.notes()[id]).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.notes.update((n) => {
        const { [id]: _removed, ...rest } = n;
        return rest;
      });
      this.load();
    });
  }

  setNotes(id: string, value: string): void {
    this.notes.update((n) => ({ ...n, [id]: value }));
  }

  typeClass(workType: WorkOrderType): string {
    return workType === 'CORRECTIVE' ? 'danger' : workType === 'PREVENTIVE' ? 'info' : 'warn';
  }

  cancelWo(id: string): void {
    this.api.cancelWorkOrder(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.load());
  }

  priorityClass(priority: WorkOrderPriority): string {
    return priority === 'CRITICAL' ? 'danger' : priority === 'HIGH' ? 'warn' : 'info';
  }

  statusClass(status: WorkOrderStatus): string {
    return status === 'COMPLETED' ? 'info' : status === 'CANCELLED' ? 'muted' : 'warn';
  }
}