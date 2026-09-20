import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ReportsApi } from '../core/services/reports-api';
import { ReportSummary } from '../core/models';
import { FileSizePipe } from '../shared/file-size.pipe';

@Component({
  selector: 'th-reports',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FileSizePipe],
  template: `
    <h1 class="page-title">Rapports</h1>
    <p class="muted">Génération de rapports PDF/CSV à partir des séries temporelles agrégées par 5 minutes.</p>

    <div class="panel">
      <h3>Nouveau rapport</h3>
      <form [formGroup]="form" (ngSubmit)="generate()">
        <div class="form-grid">
          <select formControlName="kind">
            <option value="CSV">CSV</option>
            <option value="PDF">PDF</option>
          </select>
          <input formControlName="metric" placeholder="Métrique (temperature, vibration…)" />
          <input formControlName="from" type="datetime-local" />
          <input formControlName="to" type="datetime-local" />
        </div>
        <button type="submit" [disabled]="form.invalid" class="mt">Générer</button>
        <span class="error-text" *ngIf="error()">{{ error() }}</span>
      </form>
    </div>

    <div class="card">
      <h3>Rapports générés</h3>
      <table>
        <thead>
          <tr><th>Titre</th><th>Type</th><th>Statut</th><th>Taille</th><th>Créé le</th><th></th></tr>
        </thead>
        <tbody>
          <tr *ngFor="let report of reports()">
            <td>{{ report.title }}</td>
            <td><span class="badge info">{{ report.kind }}</span></td>
            <td><span class="badge" [ngClass]="report.status === 'READY' ? 'ok' : 'warn'">{{ report.status }}</span></td>
            <td class="muted">{{ report.sizeBytes | fileSize }}</td>
            <td class="muted">{{ report.createdAt | date: 'dd/MM HH:mm' }}</td>
            <td>
              <button *ngIf="report.status === 'READY'" class="secondary" (click)="download(report)">Télécharger</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div class="empty-state" *ngIf="!reports().length">Aucun rapport généré.</div>
    </div>
  `,
})
export class ReportsComponent implements OnInit {
  private api = inject(ReportsApi);
  private fb = inject(FormBuilder);
  private destroyRef = inject(DestroyRef);

  form = this.fb.nonNullable.group({
    kind: ['CSV', Validators.required],
    metric: ['temperature', Validators.required],
    from: ['', Validators.required],
    to: ['', Validators.required],
  });

  reports = signal<ReportSummary[]>([]);
  error = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.api.list().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (page) => this.reports.set(page.content),
    });
  }

  generate(): void {
    if (this.form.invalid) {
      return;
    }
    const value = this.form.getRawValue();
    const payload = {
      kind: value.kind,
      metric: value.metric,
      from: new Date(value.from).toISOString(),
      to: new Date(value.to).toISOString(),
      equipmentId: null,
    };
    this.api.generate(payload).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.error.set(null);
        setTimeout(() => this.load(), 800);
      },
      error: (err) => this.error.set(err.error?.message ?? 'Échec de génération'),
    });
  }

  download(report: ReportSummary): void {
    this.api.download(report.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = report.fileName ?? `telemetryhub-${report.id}.${report.kind.toLowerCase()}`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
    });
  }
}