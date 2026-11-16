import { Component, effect, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { NgIcon } from '@ng-icons/core';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthStore } from '../../../../core/auth/auth-store';
import { BrandLogo } from '../../../../shared/ui/brand-logo/brand-logo';
import { ThemeToggle } from '../../../../core/layout/theme-toggle/theme-toggle';

type AuthMode = 'login' | 'register';

@Component({
  selector: 'app-auth-page',
  imports: [ReactiveFormsModule, RouterLink, NgIcon, BrandLogo, ThemeToggle],
  templateUrl: './auth-page.html',
  styleUrl: './auth-page.css',
})
export class AuthPage {
  readonly mode = input<AuthMode>('login');
  private readonly auth = inject(AuthStore);
  private readonly router = inject(Router);
  private readonly forms = inject(FormBuilder);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly loginForm = this.forms.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  readonly registerForm = this.forms.nonNullable.group(
    {
      displayName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(80)]],
      email: ['', [Validators.required, Validators.email]],
      password: [
        '',
        [
          Validators.required,
          Validators.minLength(8),
          Validators.maxLength(72),
          Validators.pattern(/^(?=.*[A-Za-zÁÉÍÓÚÜÑáéíóúüñ])(?=.*\d).+$/),
        ],
      ],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: (group) => passwordMatch(group) },
  );

  constructor() {
    effect(() => {
      this.mode();
      this.error.set(null);
    });
  }

  submitLogin(): void {
    if (this.loginForm.invalid || this.loading()) {
      this.loginForm.markAllAsTouched();
      return;
    }
    const { email, password } = this.loginForm.getRawValue();
    this.run(this.auth.login(email, password));
  }

  submitRegister(): void {
    if (this.registerForm.invalid || this.loading()) {
      this.registerForm.markAllAsTouched();
      return;
    }
    const { displayName, email, password } = this.registerForm.getRawValue();
    this.run(this.auth.register(displayName, email, password));
  }

  private run(request: ReturnType<AuthStore['login']>): void {
    this.loading.set(true);
    this.error.set(null);
    request.subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigateByUrl('/inicio');
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.error.set(messageFor(error));
      },
    });
  }
}

function passwordMatch(group: {
  value: { password?: string; confirmPassword?: string };
}): { passwordMismatch: boolean } | null {
  return group.value.password === group.value.confirmPassword ? null : { passwordMismatch: true };
}

function messageFor(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const body = error.error as { message?: string } | null;
    if (body?.message) {
      return body.message;
    }
    if (error.status === 0) {
      return 'No se puede conectar con el servidor.';
    }
    if (error.status === 401) {
      return 'Correo o contraseña incorrectos.';
    }
  }
  return 'No se pudo completar la operación. Inténtalo de nuevo.';
}
