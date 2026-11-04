import { describe, expect, it } from 'vitest';
import dayjs from 'dayjs';
import { moduleStatus } from './dashboardStatus';

const today = dayjs('2026-11-03T15:30:00');

describe('moduleStatus', () => {
  it('treats a module without entries as due', () => {
    expect(moduleStatus(undefined, 7, today)).toEqual({
      days: null,
      due: true,
      overdue: null,
      next: null,
    });
  });

  it('is not due within the rhythm and points to the next date', () => {
    const status = moduleStatus('2026-11-01', 3, today);
    expect(status.days).toBe(2);
    expect(status.due).toBe(false);
    expect(status.next.format('YYYY-MM-DD')).toBe('2026-11-04');
  });

  it('is due on the rhythm day without being overdue', () => {
    const status = moduleStatus('2026-10-27', 7, today);
    expect(status.due).toBe(true);
    expect(status.overdue).toBe(0);
  });

  it('counts overdue days past the rhythm', () => {
    expect(moduleStatus('2026-10-22', 7, today).overdue).toBe(5);
  });

  it('marks a daily module saved today as done', () => {
    expect(moduleStatus('2026-11-03', 1, today).due).toBe(false);
  });
});
