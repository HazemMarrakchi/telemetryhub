import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { AlertsComponent } from './alerts.component';
import { provideRouter } from '@angular/router';
import { provideStore } from '@ngrx/store';
import { AUTH_FEATURE_KEY, authReducer } from '../auth/store/auth.reducer';

describe('AlertsComponent', () => {
  let fixture: ComponentFixture<AlertsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AlertsComponent, ReactiveFormsModule],
      providers: [
        provideRouter([]),
        provideStore({ [AUTH_FEATURE_KEY]: authReducer }),
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(AlertsComponent);
  });

  it('creates the component', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('has a page title', () => {
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.page-title').textContent).toContain('Alertes');
  });

  it('has a rule creation form', () => {
    fixture.detectChanges();
    const form = fixture.nativeElement.querySelector('form');
    expect(form).toBeTruthy();
    const inputs = form.querySelectorAll('input, select');
    expect(inputs.length).toBe(5); // name, metric, operator, threshold, severity
  });

  it('submit button is disabled when form is invalid', () => {
    fixture.detectChanges();
    const btn = fixture.nativeElement.querySelector('button[type="submit"]');
    expect(btn.disabled).toBeTrue();
  });

  it('shows empty state when no alerts', () => {
    fixture.detectChanges();
    const empty = fixture.nativeElement.querySelector('.empty-state');
    expect(empty).toBeTruthy();
    expect(empty.textContent).toContain('Aucune alerte');
  });

  it('severityClass returns danger for CRITICAL/FATAL, warn otherwise', () => {
    const component = fixture.componentInstance;
    expect(component.severityClass('CRITICAL')).toBe('danger');
    expect(component.severityClass('FATAL')).toBe('danger');
    expect(component.severityClass('WARNING')).toBe('warn');
    expect(component.severityClass('INFO')).toBe('warn');
  });

  it('statusClass returns correct badge class', () => {
    const component = fixture.componentInstance;
    expect(component.statusClass('OPEN')).toBe('danger');
    expect(component.statusClass('ACKNOWLEDGED')).toBe('warn');
    expect(component.statusClass('RESOLVED')).toBe('info');
  });

  it('equipmentName returns dash for empty id and lookup name otherwise', () => {
    const component = fixture.componentInstance;
    component['equipmentNames'] = new Map();
    expect(component.equipmentName('')).toBe('Tous les équipements');
    expect(component.equipmentName('eq-123')).toBe('—');
    component['equipmentNames'] = new Map([['eq-123', 'Compresseur A']]);
    expect(component.equipmentName('eq-123')).toBe('Compresseur A');
  });
});