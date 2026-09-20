import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { AssistantComponent } from './assistant.component';
import { provideRouter } from '@angular/router';
import { provideStore } from '@ngrx/store';
import { AUTH_FEATURE_KEY, authReducer } from '../auth/store/auth.reducer';

describe('AssistantComponent', () => {
  let fixture: ComponentFixture<AssistantComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AssistantComponent, FormsModule],
      providers: [
        provideRouter([]),
        provideStore({ [AUTH_FEATURE_KEY]: authReducer }),
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(AssistantComponent);
  });

  it('creates the component', () => {
    const component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });

  it('shows empty state hint when no messages', () => {
    fixture.detectChanges();
    const hint = fixture.nativeElement.querySelector('.bubble.empty');
    expect(hint).toBeTruthy();
    expect(hint.textContent).toContain('Posez une question');
  });

  it('does not send empty question', () => {
    const component = fixture.componentInstance;
    component.question = '';
    spyOn(component['http'], 'post').and.returnValue({} as any);
    component.send();
    expect(component.messages().length).toBe(0);
  });

  it('adds user message on send and clears question', () => {
    const component = fixture.componentInstance;
    component.question = 'Quel est le seuil de température ?';
    spyOn(component['http'], 'post').and.returnValue({
      pipe: () => ({
        pipe: (_: any) => ({
          subscribe: (_: any) => {},
        }),
      }),
    } as any);
    component.send();
    expect(component.messages().length).toBe(1);
    expect(component.messages()[0].role).toBe('user');
    expect(component.question).toBe('');
  });

  it('sets busy to true while waiting', () => {
    const component = fixture.componentInstance;
    component.question = 'Test ?';
    let resolveCb: any;
    spyOn(component['http'], 'post').and.returnValue({
      pipe: () => ({
        pipe: (_: any) => ({
          subscribe: (cb: any) => {
            resolveCb = cb;
          },
        }),
      }),
    } as any);
    component.send();
    expect(component.busy()).toBeTrue();
    resolveCb.next({ answer: 'Réponse', sources: [] });
    expect(component.busy()).toBeFalse();
  });
});