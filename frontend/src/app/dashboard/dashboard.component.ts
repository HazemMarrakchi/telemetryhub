import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs/operators';

import { TelemetryApi } from '../core/services/telemetry-api';
import { MetricPoint } from '../core/models';

interface SeriesPoint {
  name: Date;
  value: number;
}

interface Series {
  name: string;
  series: SeriesPoint[];
  color: string;
}

const PALETTE = ['#2563eb', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4'];

@Component({
  selector: 'th-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="row">
      <h1 class="page-title">Tableau de bord</h1>
      <span class="spacer"></span>
      <span class="badge info">Vue: {{ rangeLabel }}</span>
      <span class="badge ok" *ngIf="lastUpdate()">À jour: {{ lastUpdate() }}</span>
      <button class="secondary" (click)="refresh()">Actualiser</button>
    </div>
    <p class="muted">Courbe : moyenne par tranche de 15 minutes sur les dernières 24 heures. Dernières valeurs : mesures instantanées.</p>

    <div class="card mt chart-box" *ngIf="series().length">
      <svg [attr.viewBox]="'0 0 ' + width + ' ' + height" preserveAspectRatio="none" class="chart-svg">
        <g *ngFor="let grid of gridLines()">
          <line class="grid"
            [attr.x1]="0" [attr.x2]="width"
            [attr.y1]="grid.y" [attr.y2]="grid.y" />
          <text class="axis-label" [attr.x]="4" [attr.y]="grid.y - 4">{{ grid.label }}</text>
        </g>
        <path
          *ngFor="let line of lines()"
          class="line"
          [attr.d]="line.path"
          [attr.stroke]="line.color" fill="none"/>
      </svg>
      <div class="legend">
        <span class="legend-item" *ngFor="let line of series()">
          <i class="swatch" [style.background]="line.color"></i>{{ line.name }}
        </span>
      </div>
    </div>

    <div class="empty-state" *ngIf="!loading() && !series().length">
      Aucune donnée disponible pour la période. Lancez le simulateur pour alimenter le flux.
    </div>

    <div class="card mt">
      <h3>Dernières valeurs</h3>
      <table>
        <thead>
          <tr><th>Métrique</th><th>Valeur</th><th>Horodatage</th></tr>
        </thead>
        <tbody>
          <tr *ngFor="let point of latest()">
            <td>{{ point.metric }}</td>
            <td>{{ point.value | number: '1.2-2' }}</td>
            <td class="muted">{{ point.timestamp | date: 'HH:mm:ss' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  `,
  styles: `
    :host { display: block; }
    .chart-svg {
      width: 100%; height: 260px;
      overflow: visible;
    }
    .chart-box { position: relative; height: 310px; padding: 8px; overflow: hidden; }
    .grid { stroke: var(--th-border); stroke-width: 1; stroke-dasharray: 3 3; }
    .line { stroke-width: 2; }
    .axis-label { fill: var(--th-muted); font-size: 10px; }
    .legend { display: flex; gap: 14px; flex-wrap: wrap; position: absolute; bottom: 4px; left: 12px; }
    .legend-item { display: inline-flex; align-items: center; gap: 6px; font-size: 12px; color: var(--th-muted); }
    .swatch { width: 10px; height: 10px; border-radius: 2px; display: inline-block; }
  `,
})
export class DashboardComponent implements OnInit {
  private api = inject(TelemetryApi);
  private destroyRef = inject(DestroyRef);

  rangeLabel = '24 dernières heures';

  width = 900;
  height = 260;
  private padding = { top: 14, right: 14, bottom: 14, left: 44 };

  series = signal<Series[]>([]);
  latest = signal<MetricPoint[]>([]);
  loading = signal(true);
  lastUpdate = signal<string | null>(null);
  private timer?: ReturnType<typeof setInterval>;

  ngOnInit(): void {
    this.refresh();
    this.timer = setInterval(() => this.refresh(), 5_000);
    this.destroyRef.onDestroy(() => {
      if (this.timer) {
        clearInterval(this.timer);
      }
    });
  }

  refresh(): void {
    this.loading.set(true);
    const to = new Date();
    const from = new Date(to.getTime() - 24 * 3600 * 1000);
    this.api
      .aggregate('15 minutes', from.toISOString(), to.toISOString())
      .pipe(
        finalize(() => this.loading.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (points) => {
          this.scatter(points);
          this.lastUpdate.set(new Date().toLocaleTimeString('fr-FR'));
        },
        error: () => this.series.set([]),
      });
    const recentFrom = new Date(to.getTime() - 2 * 60 * 1000);
    this.api
      .raw(recentFrom.toISOString(), to.toISOString())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (points) => this.latest.set(latestPerMetric(points, 10)),
      });
  }

  gridLines(): { y: number; label: string }[] {
    const lines = [];
    for (let i = 0; i <= 4; i++) {
      const y = this.padding.top + ((this.height - this.padding.top - this.padding.bottom) * i) / 4;
      lines.push({ y, label: i * 25 + '%' });
    }
    return lines;
  }

  lines(): { path: string; color: string }[] {
    const series = this.series();
    if (!series.length) {
      return [];
    }
    const allTimes = series.flatMap((s) => s.series.map((p) => p.name.getTime()));
    const minTime = Math.min(...allTimes);
    const maxTime = Math.max(...allTimes);
    const allValues = series.flatMap((s) => s.series.map((p) => p.value));
    const minValue = Math.min(...allValues);
    const maxValue = Math.max(...allValues);

    const plotWidth = this.width - this.padding.left - this.padding.right;
    const plotHeight = this.height - this.padding.top - this.padding.bottom;
    const x = (t: number) =>
      this.padding.left + (maxTime > minTime ? ((t - minTime) / (maxTime - minTime)) * plotWidth : plotWidth / 2);
    const y = (v: number) =>
      this.padding.top +
      plotHeight -
      (maxValue > minValue ? ((v - minValue) / (maxValue - minValue)) * plotHeight : plotHeight / 2);

    return series.map((s) => {
      const points = [...s.series].sort((a, b) => a.name.getTime() - b.name.getTime());
      const d = points
        .map((p, index) => `${index ? 'L' : 'M'}${x(p.name.getTime()).toFixed(1)},${y(p.value).toFixed(1)}`)
        .join(' ');
      return { path: d, color: s.color };
    });
  }

  private scatter(points: MetricPoint[]): void {
    const grouped = new Map<string, SeriesPoint[]>();
    for (const point of points) {
      const key = point.metric;
      const series = grouped.get(key) ?? [];
      series.push({ name: new Date(point.timestamp), value: point.value });
      grouped.set(key, series);
    }
    this.series.set(
      Array.from(grouped.entries()).map(([name, series], index) => ({
        name,
        series,
        color: PALETTE[index % PALETTE.length],
      })),
    );
  }
}

function latestPerMetric(points: MetricPoint[], limit: number): MetricPoint[] {
  const byMetric = new Map<string, MetricPoint>();
  for (const point of points) {
    const current = byMetric.get(point.metric);
    if (!current || new Date(point.timestamp) > new Date(current.timestamp)) {
      byMetric.set(point.metric, point);
    }
  }
  return Array.from(byMetric.values())
    .sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime())
    .slice(0, limit);
}