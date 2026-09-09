import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./auth/login.component').then((m) => m.LoginComponent),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./shell/shell.component').then((m) => m.ShellComponent),
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () => import('./dashboard/dashboard.component').then((m) => m.DashboardComponent),
      },
      {
        path: 'fleet',
        loadComponent: () => import('./fleet/fleet.component').then((m) => m.FleetComponent),
      },
      {
        path: 'alerts',
        loadComponent: () => import('./alerts/alerts.component').then((m) => m.AlertsComponent),
      },
      {
        path: 'reports',
        loadComponent: () => import('./reports/reports.component').then((m) => m.ReportsComponent),
      },
      {
        path: 'assistant',
        loadComponent: () => import('./assistant/assistant.component').then((m) => m.AssistantComponent),
      },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];