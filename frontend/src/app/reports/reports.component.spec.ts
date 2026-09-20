import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { ReportsComponent } from './reports.component';
import { provideRouter } from '@angular/router';
import { provideStore } from '@ngrx/store';
import { AUTH_FEATURE_KEY, authReducer } from '../auth/store/auth.reducer';

describe('ReportsComponent', () => {
  let fixture: ComponentFixture<ReportsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReportsComponent, ReactiveFormsModule],
      providers: [
        provideRouter([]),
        provideStore({ [AUTH_FEATURE_KEY]: authReducer }),
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ReportsComponent);
  });

  it('creates the component', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('has a page title', () => {
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.page-title').textContent).toContain('Rapports');
  });

  it('has a generate form with kind, metric, from, to fields', () => {
    fixture.detectChanges();
    const selects = fixture.nativeElement.querySelectorAll('select');
    const inputs = fixture.nativeElement.querySelectorAll('input');
    expect(selects.length).toBe(1); // kind
    expect(inputs.length).toBe(3); // metric, from, to
  });

  it('submit is disabled when form is invalid', () => {
    fixture.detectChanges();
    const btn = fixture.nativeElement.querySelector('button[type="submit"]');
    expect(btn.disabled).toBeTrue();
  });

  it('shows empty state when no reports', () => {
    fixture.detectChanges();
    const empty = fixture.nativeElement.querySelector('.empty-state');
    expect(empty).toBeTruthy();
    expect(empty.textContent).toContain('Aucun rapport');
  });

  it('reports is initially empty', () => {
    const component = fixture.componentInstance;
    expect(component.reports()).toEqual([]);
  });
});