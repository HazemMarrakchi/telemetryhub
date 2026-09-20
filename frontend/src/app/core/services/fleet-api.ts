import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Equipment, Site } from '../models';

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
}

@Injectable({ providedIn: 'root' })
export class FleetApi {
  private http = inject(HttpClient);

  equipment(page = 0, size = 20): Observable<Page<Equipment>> {
    return this.http.get<Page<Equipment>>(`${environment.apiUrl}/v1/equipments`, {
      params: { page, size },
    });
  }

  equipmentById(id: string): Observable<Equipment> {
    return this.http.get<Equipment>(`${environment.apiUrl}/v1/equipments/${id}`);
  }

  createEquipment(payload: Record<string, unknown>): Observable<Equipment> {
    return this.http.post<Equipment>(`${environment.apiUrl}/v1/equipments`, payload);
  }

  setStatus(id: string, status: string): Observable<Equipment> {
    return this.http.patch<Equipment>(`${environment.apiUrl}/v1/equipments/${id}/status`, { status });
  }

  sites(): Observable<Page<Site>> {
    return this.http.get<Page<Site>>(`${environment.apiUrl}/v1/sites`, { params: { size: 100 } });
  }

  createSite(payload: Record<string, unknown>): Observable<Site> {
    return this.http.post<Site>(`${environment.apiUrl}/v1/sites`, payload);
  }

  models(): Observable<Page<{ id: string; name: string }>> {
    return this.http.get<Page<{ id: string; name: string }>>(`${environment.apiUrl}/v1/models`, {
      params: { size: 100 },
    });
  }
}