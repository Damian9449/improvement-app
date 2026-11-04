import React from 'react';
import { useTranslation } from 'react-i18next';
import dayjs from 'dayjs';
import { Box, Typography } from '@mui/material';

import { MODULES } from './dashboardStatus';

const HISTORY_DAYS = 35;

export default function ActivityMosaic({ summary }) {
  const { t } = useTranslation();
  const days = Array.from({ length: HISTORY_DAYS }, (_, index) =>
    dayjs().subtract(HISTORY_DAYS - 1 - index, 'day')
  );

  return (
    <Box sx={{ mb: 4 }}>
      <Typography variant="subtitle1" fontWeight="600" sx={{ mb: 1.5 }}>
        {t('home.last35Days')}
      </Typography>

      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: `72px repeat(${HISTORY_DAYS}, minmax(0, 1fr))`,
          gap: { xs: '1px', sm: '3px' },
          alignItems: 'center',
        }}
      >
        {MODULES.map(({ key, daysField }) => {
          const filled = new Set(summary[daysField]);
          return (
            <React.Fragment key={key}>
              <Typography variant="caption" color="text.secondary" noWrap>
                {t(`home.module.${key}`)}
              </Typography>
              {days.map((day, index) => {
                const hasEntry = filled.has(day.format('YYYY-MM-DD'));
                return (
                  <Box
                    key={day.format('YYYY-MM-DD')}
                    title={`${t(`home.module.${key}`)}: ${day.format('DD-MM-YYYY')}`}
                    sx={{
                      aspectRatio: '1',
                      borderRadius: '2px',
                      bgcolor: hasEntry ? 'success.main' : 'action.hover',
                      outline: index === HISTORY_DAYS - 1 ? '1.5px solid' : 'none',
                      outlineColor: 'text.primary',
                    }}
                  />
                );
              })}
            </React.Fragment>
          );
        })}
      </Box>
    </Box>
  );
}
