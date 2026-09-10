import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { UserSummary } from '../models';

@Injectable({ providedIn: 'root' })
export class UsersApi {
  private http = inject(HttpClient);

  users(): Observable<UserSummary[]> {
    return this.http.get<UserSummary[]>(`${environment.apiUrl}/v1/users`);
  }
}