import dayjs from 'dayjs';

// Co ile dni oczekujemy wpisu w danym module — od tego zależy, czy jest "na dziś"
export const MODULES = [
  { key: 'diet', lastField: 'lastDietDay', daysField: 'dietDays', rhythm: 1 },
  { key: 'training', lastField: 'lastTraining', daysField: 'trainingDays', rhythm: 3 },
  { key: 'daily', lastField: 'lastDaily', daysField: 'dailyDays', rhythm: 1 },
  { key: 'weekly', lastField: 'lastWeekly', daysField: 'weeklyDays', rhythm: 7 },
];

export function moduleStatus(lastIso, rhythm, today = dayjs()) {
  if (!lastIso) {
    return { days: null, due: true, overdue: null, next: null };
  }
  const last = dayjs(lastIso);
  const days = today.startOf('day').diff(last, 'day');
  return {
    days,
    due: days >= rhythm,
    overdue: Math.max(days - rhythm, 0),
    next: last.add(rhythm, 'day'),
  };
}
