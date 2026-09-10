import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Availability, MaintenanceKpi, WorkOrder, WorkOrderStatus } from '../models';
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

  createWorkOrder(payload: Record<string, unknown>): Observable<WorkOrder> {
    return this.http.post<WorkOrder>(`${environment.apiUrl}/v1/work-orders`, payload);
  }

  assignWorkOrder(id: string, assignedToUserId: string): Observable<WorkOrder> {
    return this.http.post<WorkOrder>(`${environment.apiUrl}/v1/work-orders/${id}/assign`, { assignedToUserId });
  }

  startWorkOrder(id: string): Observable<WorkOrder> {
    return this.http.post<WorkOrder>(`${environment.apiUrl}/v1/work-orders/${id}/start`, {});
  }

  completeWorkOrder(id: string): Observable<WorkOrder> {
    return this.http.post<WorkOrder>(`${environment.apiUrl}/v1/work-orders/${id}/complete`, {});
  }

  cancelWorkOrder(id: string): Observable<WorkOrder> {
    return this.http.post<WorkOrder>(`${environment.apiUrl}/v1/work-orders/${id}/cancel`, {});
  }

  availability(equipmentId: string): Observable<Availability> {
    return this.http.get<Availability>(`${environment.apiUrl}/v1/downtime/equipments/${equipmentId}/availability`, {
      params: { from: new Date(Date.now() - 24 * 3600 * 1000).toISOString() },
    });
  }
}