import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DashboardComponent } from './dashboard.component';
import { provideRouter } from '@angular/router';
import { MetricPoint } from '../core/models';

const fakePoints: MetricPoint[] = [
  { metric: 'temperature', timestamp: '2026-09-20T10:00:00Z', value: 72.5 },
  { metric: 'temperature', timestamp: '2026-09-20T10:15:00Z', value: 73.1 },
  { metric: 'temperature', timestamp: '2026-09-20T10:30:00Z', value: 74.0 },
  { metric: 'vibration', timestamp: '2026-09-20T10:00:00Z', value: 0.42 },
  { metric: 'vibration', timestamp: '2026-09-20T10:15:00Z', value: 0.45 },
];

describe('DashboardComponent', () => {
  let fixture: ComponentFixture<DashboardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [provideRouter([])],
    }).compileComponents();
    fixture = TestBed.createComponent(DashboardComponent);
  });

  it('creates the component', () => {
    const component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });

  it('shows empty state when no data', () => {
    fixture.detectChanges();
    const empty = fixture.nativeElement.querySelector('.empty-state');
    expect(empty).toBeTruthy();
  });

  it('renders the page title', () => {
    fixture.detectChanges();
    const title = fixture.nativeElement.querySelector('.page-title');
    expect(title.textContent).toContain('Tableau de bord');
  });

  it('has a refresh button', () => {
    fixture.detectChanges();
    const btn = fixture.nativeElement.querySelector('button');
    expect(btn.textContent).toContain('Actualiser');
  });

  it('series() is empty initially', () => {
    const component = fixture.componentInstance;
    expect(component.series().length).toBe(0);
  });

  it('latest() is empty initially', () => {
    const component = fixture.componentInstance;
    expect(component.latest().length).toBe(0);
  });

  it('gridLines returns 5 lines with percentage labels', () => {
    const component = fixture.componentInstance;
    const lines = component.gridLines();
    expect(lines.length).toBe(5);
    expect(lines[0].label).toBe('0%');
    expect(lines[4].label).toBe('100%');
  });

  it('lines() returns empty when no series', () => {
    const component = fixture.componentInstance;
    expect(component.lines()).toEqual([]);
  });

  describe('latestPerMetric utility', () => {
    it('keeps only the most recent point per metric', () => {
      const component = fixture.componentInstance;
      const result = (component as any).latestPerMetric(fakePoints, 10);
      expect(result.length).toBe(2); // temperature and vibration
      expect(result.some((p: MetricPoint) => p.metric === 'temperature')).toBeTrue();
      expect(result.some((p: MetricPoint) => p.metric === 'vibration')).toBeTrue();
    });

    it('limits results to requested count', () => {
      const component = fixture.componentInstance;
      const result = (component as any).latestPerMetric(fakePoints, 1);
      expect(result.length).toBe(1);
    });
  });
});