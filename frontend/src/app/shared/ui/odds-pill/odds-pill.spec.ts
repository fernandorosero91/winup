import { ComponentFixture, TestBed } from '@angular/core/testing';
import { OddsPill } from './odds-pill';

describe('OddsPill', () => {
  let component: OddsPill;
  let fixture: ComponentFixture<OddsPill>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OddsPill],
    }).compileComponents();

    fixture = TestBed.createComponent(OddsPill);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
