import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Pagination } from './pagination';

describe('Pagination', () => {
  let fixture: ComponentFixture<Pagination>;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [Pagination] });
    fixture = TestBed.createComponent(Pagination);
  });

  it('shows a window of three pages around the current one', () => {
    fixture.componentRef.setInput('page', 4);
    fixture.componentRef.setInput('totalPages', 10);
    fixture.detectChanges();

    expect(fixture.componentInstance.visiblePages()).toEqual([3, 4, 5]);
  });

  it('keeps the window inside the last page', () => {
    fixture.componentRef.setInput('page', 9);
    fixture.componentRef.setInput('totalPages', 10);
    fixture.detectChanges();

    expect(fixture.componentInstance.visiblePages()).toEqual([7, 8, 9]);
    expect(fixture.componentInstance.canGoForward()).toBe(false);
    expect(fixture.componentInstance.canGoBack()).toBe(true);
  });

  it('emits the requested page', () => {
    fixture.componentRef.setInput('page', 1);
    fixture.componentRef.setInput('totalPages', 5);
    fixture.detectChanges();
    const emitted: number[] = [];
    fixture.componentInstance.pageChange.subscribe((value) => emitted.push(value));

    fixture.componentInstance.goTo(2);

    expect(emitted).toEqual([2]);
  });

  it('disables navigation while a page is loading', () => {
    fixture.componentRef.setInput('page', 1);
    fixture.componentRef.setInput('totalPages', 5);
    fixture.componentRef.setInput('disabled', true);
    fixture.detectChanges();

    expect(fixture.componentInstance.canGoBack()).toBe(false);
    expect(fixture.componentInstance.canGoForward()).toBe(false);
  });
});
