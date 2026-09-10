import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { MaintenanceApi } from '../core/services/maintenance-api';
import { FleetApi } from '../core/services/fleet-api';
import { UsersApi } from '../core/services/users-api';
import { MaintenanceKpi, UserSummary, WorkOrder, WorkOrderPriority, WorkOrderStatus, WorkOrderType } from '../core/models';
import { Equipment, GlobalOee } from '../core/models';

@Component({
  selector: 'th-maintenance',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  template: `
    <h1 class="page-title">Maintenance</h1>
    <p class="muted">Ordres de travail CMMS : les alarmes critiques créent automatiquement une intervention et ouvrent un temps d'arrêt.</p>

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
  `,
  styles: `
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

  form = this.fb.nonNullable.group({
    title: ['', Validators.required],
    equipmentId: ['', Validators.required],
    priority: ['MEDIUM' as WorkOrderPriority, Validators.required],
    workType: ['PREVENTIVE' as WorkOrderType, Validators.required],
    description: [''],
    dueAt: [''],
  });

  orders = signal<WorkOrder[]>([]);
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

  equipmentName(id: string): string {
    return id ? this.equipmentNames.get(id) ?? id.slice(0, 8) : '—';
  }

  isClosed(status: WorkOrderStatus): boolean {
    return status === 'COMPLETED' || status === 'CANCELLED';
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