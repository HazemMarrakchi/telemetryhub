import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { FleetApi } from '../core/services/fleet-api';
import { Equipment, Site } from '../core/models';

@Component({
  selector: 'th-fleet',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="row">
      <h1 class="page-title">Parc & équipements</h1>
      <span class="spacer"></span>
      <button class="secondary" (click)="load()">Actualiser</button>
    </div>

    <div class="panel">
      <h3>Ajouter un équipement</h3>
      <form [formGroup]="form" (ngSubmit)="createEquipment()">
        <div class="form-grid">
          <input formControlName="name" placeholder="Nom (ex: Compresseur SAS-01)" />
          <input formControlName="serialNumber" placeholder="N° série" />
          <select formControlName="modelId">
            <option value="">Modèle…</option>
            <option *ngFor="let model of models()" [value]="model.id">{{ model.name }}</option>
          </select>
          <select formControlName="siteId">
            <option value="">Site…</option>
            <option *ngFor="let site of sites()" [value]="site.id">{{ site.name }} — {{ site.city }}</option>
          </select>
        </div>
        <button type="submit" [disabled]="form.invalid" class="mt">Créer</button>
        <span class="error-text" *ngIf="formError()">{{ formError() }}</span>
      </form>
    </div>

    <div class="card">
      <h3>Équipements</h3>
      <table>
        <thead>
          <tr><th>Nom</th><th>N° série</th><th>Statut</th><th>Dernier contact</th></tr>
        </thead>
        <tbody>
          <tr *ngFor="let equipment of equipment()">
            <td>{{ equipment.name }}</td>
            <td class="muted">{{ equipment.serialNumber }}</td>
            <td><span class="badge" [ngClass]="statusClass(equipment.status)">{{ equipment.status }}</span></td>
            <td class="muted">{{ (equipment.lastSeenAt ?? '—') | date:'dd/MM HH:mm' }}</td>
          </tr>
        </tbody>
      </table>
      <div class="empty-state" *ngIf="!equipment().length">Aucun équipement enregistré.</div>
    </div>
  `,
})
export class FleetComponent implements OnInit {
  private api = inject(FleetApi);
  private fb = inject(FormBuilder);
  private destroyRef = inject(DestroyRef);

  form = this.fb.nonNullable.group({
    name: ['', Validators.required],
    serialNumber: ['', Validators.required],
    modelId: ['', Validators.required],
    siteId: ['', Validators.required],
  });

  equipment = signal<Equipment[]>([]);
  sites = signal<Site[]>([]);
  models = signal<{ id: string; name: string }[]>([]);
  formError = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.api.equipment(0, 50).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (page) => {
        this.equipment.set(page.content);
        this.refreshLookups();
      },
    });
  }

  private refreshLookups(): void {
    this.api.sites().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (page) => this.sites.set(page.content),
    });
    this.api.models().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (page) => this.models.set(page.content),
    });
  }

  createEquipment(): void {
    if (this.form.invalid) {
      return;
    }
    const value = this.form.getRawValue();
    this.api.createEquipment(value).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.form.reset();
        this.load();
      },
      error: (error) => this.formError.set(message(error)),
    });
  }

  statusClass(status: string): string {
    switch (status) {
      case 'ACTIVE':
        return 'ok';
      case 'MAINTENANCE':
        return 'warn';
      case 'ALERT':
        return 'danger';
      default:
        return 'info';
    }
  }
}

function message(error: { error?: { message?: string } }): string {
  return error.error?.message ?? "Une erreur est survenue";
}