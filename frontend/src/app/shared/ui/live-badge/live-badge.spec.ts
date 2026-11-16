import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LiveBadge } from './live-badge';

describe('LiveBadge', () => {
  let component: LiveBadge;
  let fixture: ComponentFixture<LiveBadge>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LiveBadge],
    }).compileComponents();

    fixture = TestBed.createComponent(LiveBadge);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
