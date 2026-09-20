import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ReportSummary } from '../models';
import { DemoDataService } from './demo-data.service';

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
}

@Injectable({ providedIn: 'root' })
export class ReportsApi {
  private http = inject(HttpClient);
  private demo = inject(DemoDataService);

  generate(payload: Record<string, unknown>): Observable<ReportSummary> {
    if (environment.demo) {
      return this.demo.generateReport(payload);
    }
    return this.http.post<ReportSummary>(`${environment.apiUrl}/v1/reports`, payload);
  }

  list(): Observable<Page<ReportSummary>> {
    if (environment.demo) {
      return this.demo.reportsList();
    }
    return this.http.get<Page<ReportSummary>>(`${environment.apiUrl}/v1/reports`, {
      params: { size: '30' },
    });
  }

  download(id: string): Observable<Blob> {
    if (environment.demo) {
      return this.demo.downloadReport(id);
    }
    return this.http.get(`${environment.apiUrl}/v1/reports/${id}`, { responseType: 'blob' });
  }
}