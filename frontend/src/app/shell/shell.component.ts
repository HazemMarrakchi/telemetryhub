import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Store } from '@ngrx/store';

import { logout as logoutAction } from '../auth/store/auth.actions';
import { selectUser } from '../auth/store/auth.selectors';

@Component({
  selector: 'th-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="shell">
      <aside class="sidebar">
        <div class="logo">
          <img src="/favicon.svg" alt="" width="26" height="26" />
          <span>TelemetryHub</span>
        </div>
        <nav class="nav">
          <a routerLink="/dashboard" routerLinkActive="active" [routerLinkActiveOptions]="{exact:true}">
            Tableau de bord
          </a>
          <a routerLink="/fleet" routerLinkActive="active">Parc & équipements</a>
          <a routerLink="/alerts" routerLinkActive="active">Alertes</a>
          <a routerLink="/reports" routerLinkActive="active">Rapports</a>
          <a routerLink="/assistant" routerLinkActive="active">Assistant IA</a>
        </nav>
        <div class="footer">
          <div class="user">
            <strong>{{ user()?.name }}</strong>
            <span class="muted">{{ user()?.email }}</span>
          </div>
          <button class="secondary" (click)="logout()">Déconnexion</button>
        </div>
      </aside>
      <main class="content">
        <router-outlet></router-outlet>
      </main>
    </div>
  `,
  styles: `
    .shell { display: flex; min-height: 100vh; }
    .sidebar {
      width: 240px; flex-shrink: 0; background: var(--th-surface);
      border-right: 1px solid var(--th-border);
      display: flex; flex-direction: column; padding: 16px 12px; gap: 18px;
      position: sticky; top: 0; height: 100vh;
    }
    .logo { display: flex; gap: 8px; align-items: center; font-weight: 700; font-size: 16px; }
    .nav { display: flex; flex-direction: column; gap: 4px; }
    .nav a {
      color: var(--th-text); padding: 9px 12px; border-radius: 8px; font-weight: 500;
    }
    .nav a:hover { background: var(--th-surface-2); }
    .nav a.active { background: var(--th-primary); color: #fff; }
    .footer { margin-top: auto; display: flex; flex-direction: column; gap: 10px; }
    .user { display: flex; flex-direction: column; font-size: 13px; }
    .content { flex: 1; padding: 26px 30px; max-width: 1280px; }
  `,
})
export class ShellComponent {
  store = inject(Store);
  user = this.store.selectSignal(selectUser);

  logout(): void {
    this.store.dispatch(logoutAction());
  }
}