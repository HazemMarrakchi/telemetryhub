import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { UserSummary } from '../models';
import { DemoDataService } from './demo-data.service';

@Injectable({ providedIn: 'root' })
export class UsersApi {
  private http = inject(HttpClient);
  private demo = inject(DemoDataService);

  users(): Observable<UserSummary[]> {
    if (environment.demo) {
      return this.demo.userList();
    }
    return this.http.get<UserSummary[]>(`${environment.apiUrl}/v1/users`);
  }
}