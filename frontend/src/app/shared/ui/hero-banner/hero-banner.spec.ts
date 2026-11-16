import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { HeroBanner } from './hero-banner';

describe('HeroBanner', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    TestBed.configureTestingModule({
      imports: [HeroBanner],
      providers: [provideRouter([])],
    });
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('rotates the banner every six seconds', () => {
    const fixture = TestBed.createComponent(HeroBanner);
    fixture.detectChanges();

    expect(fixture.componentInstance.active()).toBe(0);

    vi.advanceTimersByTime(6000);
    expect(fixture.componentInstance.active()).toBe(1);

    vi.advanceTimersByTime(54000);
    expect(fixture.componentInstance.active()).toBe(0);

    fixture.destroy();
  });

  it('jumps to the selected slide and restarts the timer', () => {
    const fixture = TestBed.createComponent(HeroBanner);
    fixture.detectChanges();

    fixture.componentInstance.goTo(9);
    expect(fixture.componentInstance.active()).toBe(9);

    vi.advanceTimersByTime(6000);
    expect(fixture.componentInstance.active()).toBe(0);

    fixture.destroy();
  });

  it('pauses and resumes the rotation', () => {
    const fixture = TestBed.createComponent(HeroBanner);
    fixture.detectChanges();

    fixture.componentInstance.toggle();
    vi.advanceTimersByTime(12000);
    expect(fixture.componentInstance.active()).toBe(0);

    fixture.componentInstance.toggle();
    vi.advanceTimersByTime(6000);
    expect(fixture.componentInstance.active()).toBe(1);

    fixture.destroy();
  });
});
