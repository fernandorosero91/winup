import { TestBed } from '@angular/core/testing';
import { ThemeStore } from './theme-store';

describe('ThemeStore', () => {
  let service: ThemeStore;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
    service = TestBed.inject(ThemeStore);
  });

  it('starts in system mode when there is no stored preference', () => {
    expect(service.mode()).toBe('system');
  });

  it('persists the selected light mode', () => {
    service.setMode('light');

    expect(service.mode()).toBe('light');
    expect(service.resolved()).toBe('light');
    expect(localStorage.getItem('winup-theme')).toBe('light');
  });

  it('toggles from dark to light', () => {
    service.setMode('dark');
    service.toggle();

    expect(service.mode()).toBe('light');
    expect(localStorage.getItem('winup-theme')).toBe('light');
  });
});
