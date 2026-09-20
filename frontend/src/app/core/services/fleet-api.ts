import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Equipment, Site } from '../models';
import { DemoDataService } from './demo-data.service';

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
}

@Injectable({ providedIn: 'root' })
export class FleetApi {
  private http = inject(HttpClient);
  private demo = inject(DemoDataService);

  equipment(page = 0, size = 20): Observable<Page<Equipment>> {
    if (environment.demo) {
      return this.demo.fleetEquipment(page, size);
    }
    return this.http.get<Page<Equipment>>(`${environment.apiUrl}/v1/equipments`, {
      params: { page, size },
    });
  }

  equipmentById(id: string): Observable<Equipment> {
    if (environment.demo) {
      return this.demo.fleetEquipmentById(id);
    }
    return this.http.get<Equipment>(`${environment.apiUrl}/v1/equipments/${id}`);
  }

  createEquipment(payload: Record<string, unknown>): Observable<Equipment> {
    if (environment.demo) {
      return this.demo.fleetCreateEquipment(payload);
    }
    return this.http.post<Equipment>(`${environment.apiUrl}/v1/equipments`, payload);
  }

  setStatus(id: string, status: string): Observable<Equipment> {
    if (environment.demo) {
      return this.demo.fleetEquipmentById(id);
    }
    return this.http.patch<Equipment>(`${environment.apiUrl}/v1/equipments/${id}/status`, { status });
  }

  sites(): Observable<Page<Site>> {
    if (environment.demo) {
      return this.demo.fleetSites();
    }
    return this.http.get<Page<Site>>(`${environment.apiUrl}/v1/sites`, { params: { size: 100 } });
  }

  createSite(payload: Record<string, unknown>): Observable<Site> {
    if (environment.demo) {
      return this.demo.fleetCreateSite(payload);
    }
    return this.http.post<Site>(`${environment.apiUrl}/v1/sites`, payload);
  }

  models(): Observable<Page<{ id: string; name: string }>> {
    if (environment.demo) {
      return this.demo.fleetModels();
    }
    return this.http.get<Page<{ id: string; name: string }>>(`${environment.apiUrl}/v1/models`, {
      params: { size: 100 },
    });
  }
}