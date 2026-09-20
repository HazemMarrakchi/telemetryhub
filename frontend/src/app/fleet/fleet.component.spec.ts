import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { FleetComponent } from './fleet.component';
import { provideRouter } from '@angular/router';
import { provideStore } from '@ngrx/store';
import { AUTH_FEATURE_KEY, authReducer } from '../auth/store/auth.reducer';

describe('FleetComponent', () => {
  let fixture: ComponentFixture<FleetComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FleetComponent, ReactiveFormsModule],
      providers: [
        provideRouter([]),
        provideStore({ [AUTH_FEATURE_KEY]: authReducer }),
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(FleetComponent);
  });

  it('creates the component', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('has a page title', () => {
    fixture.detectChanges();
    const title = fixture.nativeElement.querySelector('.page-title');
    expect(title.textContent).toContain('Parc & équipements');
  });

  it('has a form with name, serialNumber, modelId, siteId fields', () => {
    fixture.detectChanges();
    const inputs = fixture.nativeElement.querySelectorAll('input, select');
    expect(inputs.length).toBe(4);
  });

  it('submit button is disabled when form is invalid', () => {
    fixture.detectChanges();
    const btn = fixture.nativeElement.querySelector('button[type="submit"]');
    expect(btn.disabled).toBeTrue();
  });

  it('shows empty state when no equipment', () => {
    fixture.detectChanges();
    const empty = fixture.nativeElement.querySelector('.empty-state');
    expect(empty).toBeTruthy();
    expect(empty.textContent).toContain('Aucun équipement');
  });

  it('statusClass returns correct badge class', () => {
    const component = fixture.componentInstance;
    expect(component.statusClass('ACTIVE')).toBe('ok');
    expect(component.statusClass('MAINTENANCE')).toBe('warn');
    expect(component.statusClass('ALERT')).toBe('danger');
    expect(component.statusClass('STOPPED')).toBe('info');
  });

  it('dedupeByName filters duplicates', () => {
    const component = fixture.componentInstance as any;
    const items = [
      { name: 'Compresseur' },
      { name: 'compresseur' },
      { name: '  compresseur  ' },
      { name: 'Pompe A' },
    ];
    const result = component.dedupeByName(items);
    expect(result.length).toBe(2);
  });
});