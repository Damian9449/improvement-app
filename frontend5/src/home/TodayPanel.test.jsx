import React from 'react';
import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { ThemeProvider } from '@mui/material/styles';
import dayjs from 'dayjs';

import theme from '../theme';
import '../language/i18n';
import { queryClient } from '../utils/REST';
import SnackbarProvider from '../component/snackbar/SnackbarProvider';
import TodayPanel from './TodayPanel';
import ActivityMosaic from './ActivityMosaic';

const iso = (daysAgo) => dayjs().subtract(daysAgo, 'day').format('YYYY-MM-DD');

const renderPanel = (summary) =>
  render(
    <QueryClientProvider client={queryClient}>
      <ThemeProvider theme={theme}>
        <SnackbarProvider>
          <MemoryRouter>
            <TodayPanel summary={summary} />
            <ActivityMosaic summary={summary} />
          </MemoryRouter>
        </SnackbarProvider>
      </ThemeProvider>
    </QueryClientProvider>
  );

describe('dashboard panel', () => {
  it('shows only overdue modules as actions and renders history', () => {
    renderPanel({
      lastDietDay: iso(0),
      lastTraining: iso(1),
      lastDaily: iso(3),
      lastWeekly: iso(12),
      dietDays: [iso(0)],
      trainingDays: [iso(1)],
      dailyDays: [iso(3)],
      weeklyDays: [iso(12)],
    });

    expect(screen.getByText('2 z 4 na dziś')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Zapisz dzisiaj' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Zapisz wagę' })).toBeTruthy();
    expect(screen.getByText('Zapisane dzisiaj')).toBeTruthy();
    expect(screen.getByText('Dni po terminie: 5')).toBeTruthy();
  });

  it('renders a new user without entries', () => {
    renderPanel({ dietDays: [], trainingDays: [], dailyDays: [], weeklyDays: [] });

    expect(screen.getByText('0 z 4 na dziś')).toBeTruthy();
    expect(screen.getAllByText('Brak wpisów')).toHaveLength(4);
  });
});
