import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HeadControllerComponent } from './head-controller.component';

describe('HeadControllerComponent', () => {
  let component: HeadControllerComponent;
  let fixture: ComponentFixture<HeadControllerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ HeadControllerComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(HeadControllerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
