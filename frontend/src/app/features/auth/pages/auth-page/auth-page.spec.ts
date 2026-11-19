import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideIcons } from '@ng-icons/core';
import {
  lucideArrowRight,
  lucideEye,
  lucideEyeOff,
  lucideMoon,
  lucideRadio,
  lucideShieldCheck,
  lucideSun,
  lucideTrophy,
  lucideUser,
} from '@ng-icons/lucide';
import { AuthPage } from './auth-page';

describe('AuthPage password visibility', () => {
  let fixture: ComponentFixture<AuthPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AuthPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideIcons({
          lucideArrowRight,
          lucideEye,
          lucideEyeOff,
          lucideMoon,
          lucideRadio,
          lucideShieldCheck,
          lucideSun,
          lucideTrophy,
          lucideUser,
        }),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AuthPage);
    fixture.detectChanges();
  });

  it('shows and hides the login password', () => {
    const input = query<HTMLInputElement>('#login-password');
    const toggle = query<HTMLButtonElement>('.reveal');

    expect(input.type).toBe('password');

    toggle.click();
    fixture.detectChanges();

    expect(input.type).toBe('text');
    expect(toggle.getAttribute('aria-label')).toBe('Ocultar contraseña');

    toggle.click();
    fixture.detectChanges();

    expect(input.type).toBe('password');
  });

  it('shows and hides the register password and its confirmation', () => {
    fixture.componentRef.setInput('mode', 'register');
    fixture.detectChanges();
    const [passwordToggle, confirmToggle] = Array.from(
      root().querySelectorAll<HTMLButtonElement>('.reveal'),
    );

    passwordToggle.click();
    fixture.detectChanges();

    expect(query<HTMLInputElement>('#register-password').type).toBe('text');
    expect(query<HTMLInputElement>('#register-confirm').type).toBe('password');

    confirmToggle.click();
    fixture.detectChanges();

    expect(query<HTMLInputElement>('#register-confirm').type).toBe('text');
  });

  function root(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function query<T extends Element>(selector: string): T {
    return root().querySelector(selector) as T;
  }
});
