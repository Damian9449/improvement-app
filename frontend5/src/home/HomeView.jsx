import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import REST, { queryClient } from '../utils/REST';

import Button from '@mui/material/Button';
import { useTranslation } from 'react-i18next';
import { Box, Card, Container, Divider, Typography, useTheme } from '@mui/material';
import { CloudDownload, FitnessCenter, Restaurant } from '@mui/icons-material';

import PageLoader from '../component/loader/PageLoader';
import ErrorAlert from '../component/error/ErrorAlert';
import LogComponent from './LogComponent';
import TodayPanel from './TodayPanel';
import ActivityMosaic from './ActivityMosaic';

function HomeView() {
  const [loadingTrainingModule, setLoadingTrainingModule] = useState(false);
  const [loadingFoodModule, setLoadingFoodModule] = useState(false);
  const { t } = useTranslation();
  const theme = useTheme();

  const {
    data: summary,
    isLoading,
    isError,
    error,
  } = useQuery({
    queryKey: ['dashboard-summary'],
    queryFn: REST.getDashboardSummary,
  });

  function handleClickTrainingModule() {
    setLoadingTrainingModule(true);
    REST.initTrainingModule()
      .catch((error) => {
        console.error('Failed to init training module', error);
      })
      .finally(() => {
        setLoadingTrainingModule(false);
        // Import podmienia dane — odśwież panel
        queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] });
      });
  }

  function handleClickFoodModule() {
    setLoadingFoodModule(true);
    REST.initFoodModule()
      .catch((error) => {
        console.error('Failed to init food module', error);
      })
      .finally(() => {
        setLoadingFoodModule(false);
        queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] });
      });
  }

  return (
    <Box sx={{ py: 4, minHeight: '100vh' }}>
      <Container maxWidth="lg">
        {isLoading && <PageLoader text={t('messages.loading')} />}
        {isError && (
          <Box sx={{ mb: 3 }}>
            <ErrorAlert error={error} />
          </Box>
        )}
        {summary && (
          <>
            <TodayPanel summary={summary} />
            <ActivityMosaic summary={summary} />
          </>
        )}

        {/* Import działa też wtedy, gdy podsumowanie się nie wczytało */}
        <Card
          elevation={2}
          sx={{
            p: 2.5,
            borderRadius: 3,
            background: theme.palette.card.header,
            border: theme.palette.card.border,
          }}
        >
          <Box display="flex" alignItems="center" gap={1} sx={{ mb: 1.5 }}>
            <CloudDownload fontSize="small" />
            <Typography variant="subtitle1" fontWeight="600">
              {t('home.importTitle')}
            </Typography>
          </Box>
          <Box display="flex" flexWrap="wrap" gap={1} sx={{ mb: 1.5 }}>
            <Button
              variant="outlined"
              color="success"
              startIcon={<FitnessCenter />}
              onClick={handleClickTrainingModule}
              loading={loadingTrainingModule}
              sx={{ textTransform: 'none' }}
            >
              {t('home.importTraining')}
            </Button>
            <Button
              variant="outlined"
              color="warning"
              startIcon={<Restaurant />}
              onClick={handleClickFoodModule}
              loading={loadingFoodModule}
              sx={{ textTransform: 'none' }}
            >
              {t('home.importFood')}
            </Button>
          </Box>
          <Divider sx={{ mb: 1.5 }} />
          <LogComponent />
        </Card>
      </Container>
    </Box>
  );
}

export default HomeView;
