import React, { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { Link as RouterLink } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import dayjs from 'dayjs';
import Grid from '@mui/material/Grid';
import { Box, Button, Card, Chip, TextField, Typography, useTheme } from '@mui/material';
import { alpha } from '@mui/material/styles';
import {
  Check as CheckIcon,
  FitnessCenter,
  MonitorWeight,
  Restaurant,
  TaskAlt,
} from '@mui/icons-material';

import REST, { queryClient } from '../utils/REST';
import { useSnackbar } from '../component/snackbar/SnackbarProvider';
import { FoodAddUrl, TrainingAddUrl } from '../utils/URLHelper';
import { MODULES, moduleStatus } from './dashboardStatus';

const DATE_FORMAT = 'DD-MM-YYYY';
const DAILY_FIELDS = ['health', 'exercise', 'book', 'software'];
const WEIGHT_PATTERN = /^\d+([.,]\d+)?$/;

const ICONS = {
  diet: <Restaurant fontSize="small" />,
  training: <FitnessCenter fontSize="small" />,
  daily: <TaskAlt fontSize="small" />,
  weekly: <MonitorWeight fontSize="small" />,
};

const MODULE_COLORS = { diet: 'warning', training: 'success', daily: 'primary', weekly: 'info' };

// Każdy moduł ma stałe miejsce: zaległy pokazuje akcję, wykonany tylko kolejny termin
function ModuleTile({ moduleKey, status, rhythm, lastDate, children }) {
  const { t } = useTranslation();
  const theme = useTheme();
  const color = status.due
    ? theme.palette[MODULE_COLORS[moduleKey]].main
    : theme.palette.success.main;

  let caption = t('home.noEntries');
  if (!status.due) {
    caption =
      status.days === 0
        ? t('home.savedToday')
        : t('home.doneNext', {
            last: dayjs(lastDate).format(DATE_FORMAT),
            next: status.next.format(DATE_FORMAT),
          });
  } else if (status.days !== null) {
    if (rhythm === 1) caption = t('home.daysWithoutEntry', { count: status.days });
    else if (status.overdue > 0) caption = t('home.overdueDays', { count: status.overdue });
    else caption = t('home.dueToday');
  }

  return (
    <Box
      sx={{
        p: 2,
        height: '100%',
        borderRadius: 2,
        border: `1px solid ${alpha(color, status.due ? 0.45 : 0.25)}`,
        backgroundColor: alpha(color, status.due ? 0.06 : 0.04),
      }}
    >
      <Box display="flex" alignItems="center" gap={1.5} sx={{ mb: status.due ? 1.5 : 0 }}>
        <Box
          sx={{
            width: 36,
            height: 36,
            flexShrink: 0,
            borderRadius: '50%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color,
            backgroundColor: alpha(color, 0.15),
          }}
        >
          {status.due ? ICONS[moduleKey] : <CheckIcon fontSize="small" />}
        </Box>
        <Box minWidth={0}>
          <Typography variant="subtitle2" fontWeight="600">
            {t(`home.module.${moduleKey}`)}
          </Typography>
          <Typography
            variant="caption"
            component="div"
            color={status.due ? 'error.main' : 'text.secondary'}
          >
            {caption}
          </Typography>
        </Box>
      </Box>
      {status.due && children}
    </Box>
  );
}

function DailyAction() {
  const { t } = useTranslation();
  const { showSnackbar } = useSnackbar();
  const [fields, setFields] = useState({});

  const { mutate, isPending } = useMutation({
    mutationFn: () =>
      REST.addDaily({
        date: dayjs().format(DATE_FORMAT),
        ...Object.fromEntries(DAILY_FIELDS.map((field) => [field, Boolean(fields[field])])),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] });
      queryClient.invalidateQueries({ queryKey: ['daily-list'] });
      showSnackbar(t('common.addedSuccessfully'), 'success');
    },
    onError: () => showSnackbar(t('common.addFailed'), 'error'),
    retry: false,
  });

  return (
    <>
      <Box display="flex" flexWrap="wrap" gap={0.75} sx={{ mb: 1.5 }}>
        {DAILY_FIELDS.map((field) => (
          <Chip
            key={field}
            label={t(`daily.${field}`)}
            size="small"
            color={fields[field] ? 'success' : 'default'}
            variant={fields[field] ? 'filled' : 'outlined'}
            onClick={() => setFields((prev) => ({ ...prev, [field]: !prev[field] }))}
          />
        ))}
      </Box>
      <Button size="small" variant="outlined" onClick={() => mutate()} disabled={isPending}>
        {t('home.saveToday')}
      </Button>
    </>
  );
}

function WeeklyAction() {
  const { t } = useTranslation();
  const { showSnackbar } = useSnackbar();
  const [weight, setWeight] = useState('');
  const [invalid, setInvalid] = useState(false);

  const { mutate, isPending } = useMutation({
    mutationFn: () => REST.addRecordToWeeklyList({ name: weight.trim(), category: 'Waga' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] });
      queryClient.invalidateQueries({ queryKey: ['weekly-records'] });
      showSnackbar(t('weekly.addSuccess'), 'success');
    },
    onError: () => showSnackbar(t('weekly.addError'), 'error'),
    retry: false,
  });

  const handleSubmit = (event) => {
    event.preventDefault();
    if (!WEIGHT_PATTERN.test(weight.trim())) {
      setInvalid(true);
      return;
    }
    mutate();
  };

  return (
    <Box component="form" onSubmit={handleSubmit} noValidate>
      <TextField
        size="small"
        fullWidth
        label={t('home.weightKg')}
        value={weight}
        error={invalid}
        helperText={invalid ? t('home.weightInvalid') : undefined}
        slotProps={{ htmlInput: { inputMode: 'decimal' } }}
        onChange={(event) => {
          setWeight(event.target.value);
          setInvalid(false);
        }}
        sx={{ mb: 1.5 }}
      />
      <Button type="submit" size="small" variant="outlined" disabled={isPending}>
        {t('home.saveWeight')}
      </Button>
    </Box>
  );
}

function LinkAction({ to, label }) {
  return (
    <Button component={RouterLink} to={to} size="small" variant="outlined">
      {label}
    </Button>
  );
}

export default function TodayPanel({ summary }) {
  const { t, i18n } = useTranslation();
  const theme = useTheme();

  const modules = MODULES.map((module) => ({
    ...module,
    status: moduleStatus(summary[module.lastField], module.rhythm),
  }));
  const doneCount = modules.filter((module) => !module.status.due).length;

  const dateLabel = new Intl.DateTimeFormat(i18n.language, {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
  }).format(new Date());

  const actions = {
    daily: <DailyAction />,
    weekly: <WeeklyAction />,
    diet: <LinkAction to={FoodAddUrl} label={t('home.addDay')} />,
    training: <LinkAction to={TrainingAddUrl} label={t('home.addTraining')} />,
  };

  return (
    <Card
      elevation={3}
      sx={{
        p: { xs: 2, sm: 3 },
        mb: 4,
        borderRadius: 3,
        background: theme.palette.card.header,
        border: theme.palette.card.border,
      }}
    >
      <Typography
        variant="body2"
        color="text.secondary"
        sx={{ '&::first-letter': { textTransform: 'uppercase' } }}
      >
        {dateLabel}
      </Typography>
      <Typography variant="h4" fontWeight="700" sx={{ mb: 1.5 }}>
        {doneCount === modules.length
          ? t('home.allDone')
          : t('home.todayProgress', { done: doneCount, total: modules.length })}
      </Typography>

      {/* Pasek postępu: jeden segment na moduł */}
      <Box display="flex" gap={0.75} sx={{ mb: 3 }} aria-hidden="true">
        {modules.map((module, index) => (
          <Box
            key={module.key}
            sx={{
              flex: 1,
              height: 6,
              borderRadius: 3,
              backgroundColor: index < doneCount ? 'success.main' : 'action.selected',
            }}
          />
        ))}
      </Box>

      <Grid container spacing={2}>
        {modules.map((module) => (
          <Grid key={module.key} size={{ xs: 12, sm: 6, lg: 3 }}>
            <ModuleTile
              moduleKey={module.key}
              status={module.status}
              rhythm={module.rhythm}
              lastDate={summary[module.lastField]}
            >
              {actions[module.key]}
            </ModuleTile>
          </Grid>
        ))}
      </Grid>
    </Card>
  );
}
