import React from 'react';
import { useQuery, useMutation, keepPreviousData } from '@tanstack/react-query';
import REST, { queryClient } from '../../utils/REST';
import { useTranslation } from 'react-i18next';

import {
  Table,
  TableBody,
  TableContainer,
  TableHead,
  TableFooter,
  TablePagination,
  Paper,
  Card,
  Box,
  Typography,
  Chip,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
  Button,
  useTheme,
} from '@mui/material';
import { TrendingUp } from '@mui/icons-material';
import DeleteIcon from '@mui/icons-material/Delete';
import { useState } from 'react';

import StyledTableCell from '../../component/table/StyledTableCell';
import StyledTableRow from '../../component/table/StyledTableRow';
import PageLoader from '../../component/loader/PageLoader';
import ErrorAlert from '../../component/error/ErrorAlert';
import { useSnackbar } from '../../component/snackbar/SnackbarProvider';

export default function DailyPrintout() {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const { t } = useTranslation();
  const theme = useTheme();
  const { showSnackbar } = useSnackbar();
  // Wpis czekający na potwierdzenie usunięcia (historii nie da się odtworzyć, więc pytamy)
  const [toDelete, setToDelete] = useState(null);

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['daily-list', page, size],
    queryFn: () => REST.getDaily(page, size),
    placeholderData: keepPreviousData,
    staleTime: 1000 * 60 * 5,
  });

  const { mutate: deleteDaily, isPending: isDeleting } = useMutation({
    mutationFn: (daily) => REST.deleteDaily(daily.id),
    onSuccess: (_, daily) => {
      queryClient.invalidateQueries({ queryKey: ['daily-list'] });
      showSnackbar(t('daily.deleteSuccess', { date: daily.date }), 'success');
    },
    onError: (_, daily) => {
      showSnackbar(t('daily.deleteError', { date: daily.date }), 'error');
    },
    onSettled: () => setToDelete(null),
    retry: false,
  });

  const handleChangePage = (event, newPage) => {
    setPage(newPage);
  };

  const handleChangeRowsPerPage = (event) => {
    setSize(+event.target.value);
    setPage(0);
  };

  if (isLoading) {
    return <PageLoader text={t('messages.loading')} />;
  }

  if (isError) {
    return <ErrorAlert error={error} />;
  }

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
          <TrendingUp sx={{ fontSize: 22 }} />
          <Typography variant="subtitle1" fontWeight="600">
            {t('daily.history')}
          </Typography>
        </Box>

        {/* Table */}
        <TableContainer component={Paper} sx={{ borderRadius: 0 }}>
          <Table>
            <TableHead>
              <StyledTableRow>
                <StyledTableCell>{t('daily.date')}</StyledTableCell>
                <StyledTableCell>{t('daily.health')}</StyledTableCell>
                <StyledTableCell>{t('daily.exercise')}</StyledTableCell>
                <StyledTableCell>{t('daily.book')}</StyledTableCell>
                <StyledTableCell>{t('daily.software')}</StyledTableCell>
                <StyledTableCell>{t('daily.actions')}</StyledTableCell>
              </StyledTableRow>
            </TableHead>

            <TableBody>
              {data?.content?.map((daily, index) => (
                <StyledTableRow
                  key={daily.id || index}
                  sx={{
                    '&:hover': {
                      backgroundColor: theme.palette.action.hover,
                    },
                  }}
                >
                  <StyledTableCell>
                    <Typography variant="body2" fontWeight="600">
                      {daily.date}
                    </Typography>
                  </StyledTableCell>
                  <StyledTableCell>
                    <Chip
                      label={daily.health ? t('common.yes') : t('common.no')}
                      color={daily.health ? 'success' : 'error'}
                      variant="outlined"
                      size="small"
                    />
                  </StyledTableCell>
                  <StyledTableCell>
                    <Chip
                      label={daily.exercise ? t('common.yes') : t('common.no')}
                      color={daily.exercise ? 'success' : 'error'}
                      variant="outlined"
                      size="small"
                    />
                  </StyledTableCell>
                  <StyledTableCell>
                    <Chip
                      label={daily.book ? t('common.yes') : t('common.no')}
                      color={daily.book ? 'success' : 'error'}
                      variant="outlined"
                      size="small"
                    />
                  </StyledTableCell>
                  <StyledTableCell>
                    <Chip
                      label={daily.software ? t('common.yes') : t('common.no')}
                      color={daily.software ? 'success' : 'error'}
                      variant="outlined"
                      size="small"
                    />
                  </StyledTableCell>
                  <StyledTableCell sx={{ width: '80px' }}>
                    <IconButton
                      aria-label="delete daily"
                      size="small"
                      onClick={() => setToDelete(daily)}
                      disabled={isDeleting}
                      sx={{
                        backgroundColor: theme.palette.error.light,
                        color: theme.palette.error.main,
                        '&:hover': {
                          backgroundColor: theme.palette.error.main,
                          color: 'text.primary',
                        },
                      }}
                    >
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  </StyledTableCell>
                </StyledTableRow>
              ))}
            </TableBody>

            <TableFooter>
              <StyledTableRow>
                <TablePagination
                  size="small"
                  rowsPerPageOptions={[5, 10, 25]}
                  colSpan={6}
                  count={data?.totalElements || 0}
                  rowsPerPage={size}
                  page={page}
                  SelectProps={{
                    inputProps: { 'aria-label': 'rows per page' },
                    native: true,
                  }}
                  onPageChange={handleChangePage}
                  onRowsPerPageChange={handleChangeRowsPerPage}
                />
              </StyledTableRow>
            </TableFooter>
          </Table>
        </TableContainer>
      </Card>

      <Dialog open={toDelete !== null} onClose={() => !isDeleting && setToDelete(null)}>
        <DialogTitle>{t('daily.deleteConfirmTitle')}</DialogTitle>
        <DialogContent>
          <DialogContentText>
            {t('daily.deleteConfirmText', { date: toDelete?.date })}
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setToDelete(null)} disabled={isDeleting}>
            {t('common.cancel')}
          </Button>
          <Button
            color="error"
            variant="contained"
            onClick={() => deleteDaily(toDelete)}
            disabled={isDeleting}
          >
            {t('common.delete')}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
