import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthStore } from './auth-store';

describe('AuthStore', () => {
  let store: AuthStore;
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [AuthStore, provideHttpClient(), provideHttpClientTesting()],
    });
    store = TestBed.inject(AuthStore);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    sessionStorage.clear();
  });

  it('stores token and user after login', () => {
    store.login('alex@winup.app', 'Winup2026').subscribe();

    const request = http.expectOne('/api/v1/auth/login');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ email: 'alex@winup.app', password: 'Winup2026' });
    request.flush({
      token: 'token-1',
      user: { id: 'u1', email: 'alex@winup.app', displayName: 'Alex Martínez' },
    });

    expect(store.token()).toBe('token-1');
    expect(store.user()?.displayName).toBe('Alex Martínez');
    expect(store.isAuthenticated()).toBe(true);
    expect(store.initials()).toBe('AM');
    expect(sessionStorage.getItem('winup.token')).toBe('token-1');
  });

  it('clears the session on logout', () => {
    store.login('alex@winup.app', 'Winup2026').subscribe();
    http.expectOne('/api/v1/auth/login').flush({
      token: 'token-1',
      user: { id: 'u1', email: 'alex@winup.app', displayName: 'Alex Martínez' },
    });

    store.clear();

    expect(store.token()).toBeNull();
    expect(store.user()).toBeNull();
    expect(store.isAuthenticated()).toBe(false);
    expect(sessionStorage.getItem('winup.token')).toBeNull();
  });
});
