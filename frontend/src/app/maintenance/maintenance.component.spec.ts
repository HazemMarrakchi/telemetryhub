import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { MaintenanceComponent } from './maintenance.component';
import { provideRouter } from '@angular/router';
import { provideStore } from '@ngrx/store';
import { AUTH_FEATURE_KEY, authReducer } from '../auth/store/auth.reducer';

describe('MaintenanceComponent', () => {
  let fixture: ComponentFixture<MaintenanceComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MaintenanceComponent, ReactiveFormsModule, FormsModule],
      providers: [
        provideRouter([]),
        provideStore({ [AUTH_FEATURE_KEY]: authReducer }),
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(MaintenanceComponent);
  });

  it('creates the component', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('has a page title', () => {
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.page-title').textContent).toContain('Maintenance');
  });

  it('has 3 tabs by default', () => {
    fixture.detectChanges();
    const tabs = fixture.nativeElement.querySelectorAll('.tabs button');
    expect(tabs.length).toBe(3);
    expect(tabs[0].textContent).toContain('Ordres de travail');
    expect(tabs[1].textContent).toContain('Historique');
    expect(tabs[2].textContent).toContain('Maintenance préventive');
  });

  it('default view is orders', () => {
    fixture.detectChanges();
    const component = fixture.componentInstance;
    expect(component.view()).toBe('orders');
  });

  it('switches to history view on tab click', () => {
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.view.set('history');
    expect(component.view()).toBe('history');
  });

  it('switches to preventive view on tab click', () => {
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.view.set('preventive');
    expect(component.view()).toBe('preventive');
  });

  it('priorityClass returns danger for CRITICAL, warn for HIGH, info otherwise', () => {
    const component = fixture.componentInstance;
    expect(component.priorityClass('CRITICAL')).toBe('danger');
    expect(component.priorityClass('HIGH')).toBe('warn');
    expect(component.priorityClass('MEDIUM')).toBe('info');
    expect(component.priorityClass('LOW')).toBe('info');
  });

  it('statusClass returns info for COMPLETED, muted for CANCELLED, warn otherwise', () => {
    const component = fixture.componentInstance;
    expect(component.statusClass('COMPLETED')).toBe('info');
    expect(component.statusClass('CANCELLED')).toBe('muted');
    expect(component.statusClass('CREATED')).toBe('warn');
  });

  it('typeClass returns correct class', () => {
    const component = fixture.componentInstance;
    expect(component.typeClass('CORRECTIVE')).toBe('danger');
    expect(component.typeClass('PREVENTIVE')).toBe('info');
    expect(component.typeClass('INSPECTION')).toBe('warn');
  });

  it('userName returns dash for null id', () => {
    const component = fixture.componentInstance;
    expect(component.userName(null)).toBe('—');
  });

  it('userName returns fullName for known user', () => {
    const component = fixture.componentInstance;
    component.users.set([{ id: 'u1', fullName: 'Dupont Jean', role: 'TECHNICIAN' }]);
    expect(component.userName('u1')).toBe('Dupont Jean');
  });

  it('userName returns truncated id for unknown user', () => {
    const component = fixture.componentInstance;
    component.users.set([]);
    const result = component.userName('abc12345');
    expect(result).toBe('abc1234…');
  });

  it('setNotes updates notes map', () => {
    const component = fixture.componentInstance;
    component.setNotes('wo-1', 'Remplacement effectué');
    expect(component.notes()['wo-1']).toBe('Remplacement effectué');
  });
});