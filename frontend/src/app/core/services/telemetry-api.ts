import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { MetricPoint } from '../models';

@Injectable({ providedIn: 'root' })
export class TelemetryApi {
  private http = inject(HttpClient);

  raw(from: string, to: string, equipmentId?: string): Observable<MetricPoint[]> {
    let params = new HttpParams().set('from', from).set('to', to);
    if (equipmentId) {
      params = params.set('equipmentId', equipmentId);
    }
    return this.http.get<MetricPoint[]>(`${environment.apiUrl}/v1/telemetry/raw`, { params });
  }

  aggregate(bucket: string, from: string, to: string, equipmentId?: string): Observable<MetricPoint[]> {
    let params = new HttpParams()
      .set('bucket', bucket)
      .set('from', from)
      .set('to', to);
    if (equipmentId) {
      params = params.set('equipmentId', equipmentId);
    }
    return this.http.get<MetricPoint[]>(`${environment.apiUrl}/v1/telemetry/aggregate`, { params });
  }

  latest(): Observable<MetricPoint[]> {
    return this.http.get<MetricPoint[]>(`${environment.apiUrl}/v1/telemetry/latest`);
  }

  ingest(payload: Record<string, unknown>): Observable<unknown> {
    return this.http.post(`${environment.apiUrl}/v1/ingestion/telemetry`, payload);
  }
}