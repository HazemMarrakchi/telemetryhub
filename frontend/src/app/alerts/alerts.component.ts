import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { AlertingApi } from '../core/services/alerting-api';
import { AlertEvent } from '../core/models';

@Component({
  selector: 'th-alerts',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <h1 class="page-title">Alertes</h1>
    <p class="muted">Règles de seuil évaluées en temps réel sur le flux de mesures.</p>

    <div class="panel">
      <h3>Créer une règle</h3>
      <form [formGroup]="ruleForm" (ngSubmit)="createRule()">
        <div class="form-grid">
          <input formControlName="name" placeholder="Nom de la règle" />
          <input formControlName="metric" placeholder="Métrique (temperature…)" />
          <select formControlName="operator">
            <option value="GT">&gt;</option>
            <option value="GTE">&gt;=</option>
            <option value="LT">&lt;</option>
            <option value="LTE">&lt;=</option>
          </select>
          <input formControlName="threshold" type="number" step="0.1" placeholder="Seuil" />
          <select formControlName="severity">
            <option value="INFO">INFO</option>
            <option value="WARNING">WARNING</option>
            <option value="CRITICAL">CRITICAL</option>
          </select>
        </div>
        <button type="submit" [disabled]="ruleForm.invalid" class="mt">Ajouter la règle</button>
      </form>
      <div class="mt">
        <span class="badge info" *ngFor="let rule of rules()" style="margin-right:6px">
          {{ rule.name }} ({{ rule.metric }} {{ rule.operator }} {{ rule.threshold }})
          <button class="linklike" (click)="toggleRule(rule)">
            {{ rule.enabled ? 'off' : 'on' }}
          </button>
        </span>
      </div>
    </div>

    <div class="card">
      <h3>Événements</h3>
      <table>
        <thead>
          <tr><th>Règle</th><th>Métrique</th><th>Valeur / seuil</th><th>Sévérité</th><th>Statut</th><th>Horodatage</th><th></th></tr>
        </thead>
        <tbody>
          <tr *ngFor="let alert of alerts()">
            <td>{{ alert.ruleName }}</td>
            <td class="muted">{{ alert.metric }}</td>
            <td>{{ alert.value | number: '1.1-1' }} / {{ alert.threshold }}</td>
            <td><span class="badge" [ngClass]="severityClass(alert.severity)">{{ alert.severity }}</span></td>
            <td><span class="badge" [ngClass]="statusClass(alert.status)">{{ alert.status }}</span></td>
            <td class="muted">{{ alert.triggeredAt | date: 'dd/MM HH:mm:ss' }}</td>
            <td>
              <button class="secondary" *ngIf="alert.status === 'OPEN'" (click)="ack(alert.id)">Acquitter</button>
              <button class="secondary" *ngIf="alert.status === 'ACKNOWLEDGED'" (click)="resolve(alert.id)">Résoudre</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div class="empty-state" *ngIf="!alerts().length">Aucune alerte pour l'instant.</div>
    </div>
  `,
  styles: `
    .linklike { background: none; color: var(--th-text); padding: 0 4px; font-weight: 600; }
    .linklike:hover { background: none; text-decoration: underline; }
  `,
})
export class AlertsComponent implements OnInit {
  private api = inject(AlertingApi);
  private fb = inject(FormBuilder);
  private destroyRef = inject(DestroyRef);

  ruleForm = this.fb.nonNullable.group({
    name: ['', Validators.required],
    metric: ['temperature', Validators.required],
    operator: ['GT', Validators.required],
    threshold: [0, Validators.required],
    severity: ['WARNING', Validators.required],
  });

  alerts = signal<AlertEvent[]>([]);
  rules = signal<{ id: string; name: string; metric: string; operator: string; threshold: number; enabled: boolean }[]>([]);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.api.alerts().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (page) => this.alerts.set(page.content),
    });
    this.api.rules().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (rules) => this.rules.set(rules),
    });
  }

  createRule(): void {
    if (this.ruleForm.invalid) {
      return;
    }
    const value = this.ruleForm.getRawValue();
    this.api.createRule({ ...value, threshold: Number(value.threshold) })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.ruleForm.controls.name.reset();
          this.load();
        },
      });
  }

  toggleRule(rule: { id: string; enabled: boolean }): void {
    this.api.setRuleEnabled(rule.id, !rule.enabled).pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.load());
  }

  ack(id: string): void {
    this.api.acknowledge(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.load());
  }

  resolve(id: string): void {
    this.api.resolve(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.load());
  }

  severityClass(severity: string): string {
    return severity === 'CRITICAL' || severity === 'FATAL' ? 'danger' : 'warn';
  }

  statusClass(status: string): string {
    return status === 'OPEN' ? 'danger' : status === 'ACKNOWLEDGED' ? 'warn' : 'info';
  }
}