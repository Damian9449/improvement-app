import React, { useState } from 'react';
import REST from '../../utils/REST';
import { useMutation } from '@tanstack/react-query';
import { queryClient } from '../../utils/REST';
import { useTranslation } from 'react-i18next';
import { useSnackbar } from '../../component/snackbar/SnackbarProvider';

import Grid from '@mui/material/Grid';
import {
  Typography,
  Card,
  CardContent,
  Box,
  FormControlLabel,
  Checkbox,
  Button,
  useTheme,
} from '@mui/material';
import { ChecklistRtl } from '@mui/icons-material';
import { DatePicker, LocalizationProvider } from '@mui/x-date-pickers';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import dayjs from 'dayjs';

// Ten sam format co w historii wpisów i w API (dd-MM-yyyy)
const DATE_FORMAT = 'DD-MM-YYYY';

export default function DailyForm() {
  const { showSnackbar } = useSnackbar();
  const { t } = useTranslation();
  const theme = useTheme();

  // Domyślnie dzisiaj; po zapisie data zostaje, żeby łatwo uzupełniać kolejne dni wstecz
  const [date, setDate] = useState(() => dayjs());

  const [dailyFields, setDailyFields] = useState({
    health: false,
    exercise: false,
    book: false,
    software: false,
  });

  const { mutate, isPending } = useMutation({
    mutationFn: () => REST.addDaily({ ...dailyFields, date: date.format(DATE_FORMAT) }),
    onSuccess: () => {
      queryClient.invalidateQueries(['daily-list']);
      showSnackbar(t('common.addedSuccessfully'), 'success');
      setDailyFields({ health: false, exercise: false, book: false, software: false });
    },
    onError: () => {
      showSnackbar(t('common.addFailed'), 'error');
    },
    retry: false,
  });

  const handleFormChange = (event) => {
    setDailyFields((prev) => ({
      ...prev,
      [event.target.name]: !prev[event.target.name],
    }));
  };

  return (
    <Box sx={{ py: 2 }}>
      <Card elevation={2} sx={{ borderRadius: 3, overflow: 'hidden' }}>
        {/* Header */}
        <Box
          sx={{
            p: 2,
            background: theme.palette.card.header,
            color: 'text.primary',
            display: 'flex',
            alignItems: 'center',
            gap: 2,
          }}
        >
          <ChecklistRtl sx={{ fontSize: 22 }} />
          <Typography variant="subtitle1" fontWeight="600">
            {t('daily.title')}
          </Typography>
        </Box>

        {/* Form body */}
        <CardContent sx={{ p: 3 }}>
          <Grid container spacing={2}>
            <Grid size={12}>
              <LocalizationProvider dateAdapter={AdapterDayjs}>
                <DatePicker
                  label={t('daily.date')}
                  value={date}
                  onChange={(newDate) => setDate(newDate)}
                  format={DATE_FORMAT}
                  disableFuture
                  slotProps={{ textField: { size: 'small', sx: { minWidth: 160 } } }}
                />
              </LocalizationProvider>
            </Grid>

            <Grid size={12}>
              <FormControlLabel
                control={<Checkbox checked={dailyFields.health} />}
                name="health"
                onChange={handleFormChange}
                label={t('daily.health')}
              />
              <FormControlLabel
                control={<Checkbox checked={dailyFields.exercise} />}
                name="exercise"
                onChange={handleFormChange}
                label={t('daily.exercise')}
              />
              <FormControlLabel
                control={<Checkbox checked={dailyFields.book} />}
                name="book"
                onChange={handleFormChange}
                label={t('daily.book')}
              />
              <FormControlLabel
                control={<Checkbox checked={dailyFields.software} />}
                name="software"
                onChange={handleFormChange}
                label={t('daily.software')}
              />
            </Grid>

            <Grid size={12}>
              <Button
                variant="contained"
                onClick={() => mutate()}
                disabled={isPending || !date?.isValid()}
                sx={{ minWidth: 120 }}
              >
                {isPending ? t('common.saving') : t('messages.submit')}
              </Button>
            </Grid>
          </Grid>
        </CardContent>
      </Card>
    </Box>
  );
}
