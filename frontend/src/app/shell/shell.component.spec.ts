import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { provideStore } from '@ngrx/store';
import { ShellComponent } from './shell.component';
import { AUTH_FEATURE_KEY, authReducer } from '../auth/store/auth.reducer';

describe('ShellComponent', () => {
  let fixture: ComponentFixture<ShellComponent>;
  let component: ShellComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ShellComponent],
      providers: [
        provideRouter([]),
        provideStore({ [AUTH_FEATURE_KEY]: authReducer }),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ShellComponent);
    component = fixture.componentInstance;
  });

  it('creates the component', () => {
    expect(component).toBeTruthy();
  });

  it('renders the nav links', () => {
    fixture.detectChanges();
    const navLinks = fixture.debugElement.queryAll(By.css('.nav a'));
    expect(navLinks.length).toBe(6);
    expect(navLinks[0].nativeElement.textContent).toContain('Tableau de bord');
    expect(navLinks[4].nativeElement.textContent).toContain('Rapports');
    expect(navLinks[5].nativeElement.textContent).toContain('Assistant IA');
  });

  it('displays TelemetryHub logo', () => {
    fixture.detectChanges();
    const logo = fixture.debugElement.query(By.css('.logo span'));
    expect(logo.nativeElement.textContent).toContain('TelemetryHub');
  });

  it('dispatches logout on button click', () => {
    fixture.detectChanges();
    spyOn(component.store, 'dispatch');
    const btn = fixture.debugElement.query(By.css('button'));
    btn.triggerEventHandler('click', null);
    expect(component.store.dispatch).toHaveBeenCalled();
  });
});