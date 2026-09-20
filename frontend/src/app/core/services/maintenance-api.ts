import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Availability, CostTrend, GlobalOee, MaintenanceKpi, MaintenanceSchedule, WorkOrder, WorkOrderStatus } from '../models';
import { Page } from './alerting-api';

@Injectable({ providedIn: 'root' })
export class MaintenanceApi {
  private http = inject(HttpClient);

  workOrders(status?: WorkOrderStatus, equipmentId?: string): Observable<Page<WorkOrder>> {
    const params: Record<string, string> = { size: '50' };
    if (status) {
      params['status'] = status;
    }
    if (equipmentId) {
      params['equipmentId'] = equipmentId;
    }
    return this.http.get<Page<WorkOrder>>(`${environment.apiUrl}/v1/work-orders`, { params });
  }

  kpi(): Observable<MaintenanceKpi> {
    return this.http.get<MaintenanceKpi>(`${environment.apiUrl}/v1/work-orders/kpi`);
  }

  oee(from?: string, to?: string): Observable<GlobalOee> {
    const params: Record<string, string> = {};
    if (from) {
      params['from'] = from;
    }
    if (to) {
      params['to'] = to;
    }
    return this.http.get<GlobalOee>(`${environment.apiUrl}/v1/downtime/oee`, { params });
  }

  createWorkOrder(payload: Record<string, unknown>): Observable<WorkOrder> {
    return this.http.post<WorkOrder>(`${environment.apiUrl}/v1/work-orders`, payload);
  }

  assignWorkOrder(id: string, assignedToUserId: string): Observable<WorkOrder> {
    return this.http.post<WorkOrder>(`${environment.apiUrl}/v1/work-orders/${id}/assign`, { assignedToUserId });
  }

  startWorkOrder(id: string): Observable<WorkOrder> {
    return this.http.post<WorkOrder>(`${environment.apiUrl}/v1/work-orders/${id}/start`, {});
  }

  completeWorkOrder(id: string, completionNotes?: string): Observable<WorkOrder> {
    const body: Record<string, unknown> = {};
    if (completionNotes) {
      body['completionNotes'] = completionNotes;
    }
    return this.http.post<WorkOrder>(`${environment.apiUrl}/v1/work-orders/${id}/complete`, body);
  }

  cancelWorkOrder(id: string): Observable<WorkOrder> {
    return this.http.post<WorkOrder>(`${environment.apiUrl}/v1/work-orders/${id}/cancel`, {});
  }

  availability(equipmentId: string): Observable<Availability> {
    return this.http.get<Availability>(`${environment.apiUrl}/v1/downtime/equipments/${equipmentId}/availability`, {
      params: { from: new Date(Date.now() - 24 * 3600 * 1000).toISOString() },
    });
  }

  history(): Observable<Page<WorkOrder>> {
    return this.http.get<Page<WorkOrder>>(`${environment.apiUrl}/v1/work-orders/history`, {
      params: { size: '100' },
    });
  }

  costs(): Observable<CostTrend> {
    return this.http.get<CostTrend>(`${environment.apiUrl}/v1/work-orders/costs`);
  }

  schedules(): Observable<MaintenanceSchedule[]> {
    return this.http.get<MaintenanceSchedule[]>(`${environment.apiUrl}/v1/work-orders/schedules`);
  }

  createSchedule(payload: Record<string, unknown>): Observable<MaintenanceSchedule> {
    return this.http.post<MaintenanceSchedule>(`${environment.apiUrl}/v1/work-orders/schedules`, payload);
  }

  deleteSchedule(id: string): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/v1/work-orders/schedules/${id}`);
  }
}