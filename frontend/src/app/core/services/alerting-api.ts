import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AlertEvent } from '../models';

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
}

@Injectable({ providedIn: 'root' })
export class AlertingApi {
  private http = inject(HttpClient);

  alerts(status?: string, severity?: string): Observable<Page<AlertEvent>> {
    const params: Record<string, string> = { size: '50' };
    if (status) {
      params['status'] = status;
    }
    if (severity) {
      params['severity'] = severity;
    }
    return this.http.get<Page<AlertEvent>>(`${environment.apiUrl}/v1/alerts`, { params });
  }

  acknowledge(id: string): Observable<AlertEvent> {
    return this.http.post<AlertEvent>(`${environment.apiUrl}/v1/alerts/${id}/acknowledge`, {});
  }

  resolve(id: string): Observable<AlertEvent> {
    return this.http.post<AlertEvent>(`${environment.apiUrl}/v1/alerts/${id}/resolve`, {});
  }

  rules(): Observable<AlertRuleApi[]> {
    return this.http.get<AlertRuleApi[]>(`${environment.apiUrl}/v1/rules`);
  }

  createRule(payload: Record<string, unknown>): Observable<AlertRuleApi> {
    return this.http.post<AlertRuleApi>(`${environment.apiUrl}/v1/rules`, payload);
  }

  setRuleEnabled(id: string, enabled: boolean): Observable<AlertRuleApi> {
    return this.http.patch<AlertRuleApi>(`${environment.apiUrl}/v1/rules/${id}/enabled`, { enabled });
  }
}

export interface AlertRuleApi {
  id: string;
  name: string;
  metric: string;
  operator: string;
  threshold: number;
  severity: string;
  enabled: boolean;
}