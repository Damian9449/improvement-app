import React, { useEffect, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import {
  Box,
  Card,
  CardContent,
  Paper,
  Typography,
  List,
  ListItem,
  ListItemText,
  Fade,
  CircularProgress,
  Chip,
  useTheme,
} from '@mui/material';
import { alpha } from '@mui/material/styles';
import { Message as MessageIcon, Circle as CircleIcon } from '@mui/icons-material';
import { useTranslation } from 'react-i18next';

const LogComponent = () => {
  const { t } = useTranslation();
  const theme = useTheme();
  const [messages, setMessages] = useState([]);
  const [isConnected, setIsConnected] = useState(false);
  const [isConnecting, setIsConnecting] = useState(true);

  const host_url = import.meta.env.VITE_API_URL;

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS(`${host_url}ws`),
      reconnectDelay: 5000,
      onConnect: () => {
        setIsConnected(true);
        setIsConnecting(false);
        client.subscribe('/user/queue/messages', (message) => {
          setMessages((prevMessages) => {
            const updatedMessages = [message.body, ...prevMessages];
            return updatedMessages.slice(0, 10);
          });
        });
      },
      onStompError: (frame) => {
        console.error('❌ Błąd STOMP:', frame.headers['message']);
        setIsConnected(false);
        setIsConnecting(false);
      },
      onWebSocketError: (error) => {
        console.error('❌ Błąd WebSocket:', error);
        setIsConnected(false);
        setIsConnecting(false);
      },
    });

    client.activate();

    return () => {
      client.deactivate();
    };
  }, [host_url]);

  return (
    <Card
      elevation={2}
      sx={{
        borderRadius: 3,
        bgcolor: 'background.surface',
        border: theme.palette.card.border,
        overflow: 'hidden',
      }}
    >
      <Box
        sx={{
          p: 2,
          background: theme.palette.card.header,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
        }}
      >
        <Box display="flex" alignItems="center" gap={2}>
          <MessageIcon sx={{ fontSize: 24, color: 'success.main' }} />
          <Typography variant="subtitle1" fontWeight="600" color="text.primary">
            {t('home.activityLog')}
          </Typography>
        </Box>

        {/* Connection Status */}
        <Box display="flex" alignItems="center" gap={1}>
          {isConnecting ? (
            <Box display="flex" alignItems="center" gap={1}>
              <CircularProgress size={16} sx={{ color: 'warning.main' }} />
              <Chip
                label={t('home.connecting')}
                size="small"
                sx={{
                  backgroundColor: alpha(theme.palette.warning.main, 0.2),
                  color: 'warning.main',
                  border: `1px solid ${alpha(theme.palette.warning.main, 0.3)}`,
                }}
              />
            </Box>
          ) : (
            <Chip
              icon={<CircleIcon sx={{ fontSize: 12 }} />}
              label={isConnected ? t('home.connected') : t('home.disconnected')}
              size="small"
              sx={{
                backgroundColor: alpha(
                  isConnected ? theme.palette.success.main : theme.palette.error.main,
                  0.2
                ),
                color: isConnected ? 'success.main' : 'error.main',
                border: `1px solid ${alpha(
                  isConnected ? theme.palette.success.main : theme.palette.error.main,
                  0.3
                )}`,
              }}
            />
          )}
        </Box>
      </Box>

      <CardContent sx={{ p: 3 }}>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          {t('home.last10Messages')}
        </Typography>

        {/* Messages List */}
        <Box
          sx={{
            overflow: 'auto',
            '&::-webkit-scrollbar': {
              width: '8px',
            },
            '&::-webkit-scrollbar-track': {
              background: 'rgba(255, 255, 255, 0.1)',
              borderRadius: '4px',
            },
            '&::-webkit-scrollbar-thumb': {
              background: 'rgba(255, 255, 255, 0.3)',
              borderRadius: '4px',
              '&:hover': {
                background: 'rgba(255, 255, 255, 0.4)',
              },
            },
          }}
        >
          {messages.length ? (
            <List sx={{ p: 0 }}>
              {messages.map((msg, index) => (
                <Fade in={true} timeout={500} key={index}>
                  <ListItem
                    sx={{
                      mb: 1,
                      p: 0,
                      '&:last-child': { mb: 0 },
                    }}
                  >
                    <Paper
                      variant="outlined"
                      sx={{
                        width: '100%',
                        borderRadius: 2,
                        p: 2,
                        textAlign: 'center',
                      }}
                    >
                      <ListItemText
                        primary={
                          <Typography
                            variant="body2"
                            sx={{
                              color: 'success.main',
                              fontWeight: 500,
                              fontSize: '0.9rem',
                              wordBreak: 'break-word',
                            }}
                          >
                            {msg}
                          </Typography>
                        }
                        secondary={
                          <Typography
                            variant="caption"
                            sx={{
                              color: 'text.disabled',
                              fontSize: '0.75rem',
                              mt: 0.5,
                            }}
                          >
                            {new Date().toLocaleTimeString()}
                          </Typography>
                        }
                      />
                    </Paper>
                  </ListItem>
                </Fade>
              ))}
            </List>
          ) : (
            <Paper
              variant="outlined"
              sx={{
                borderRadius: 2,
                py: 4,
                textAlign: 'center',
              }}
            >
              <MessageIcon sx={{ fontSize: 48, color: 'text.disabled', mb: 2 }} />
              <Typography variant="body1" color="text.secondary" fontWeight="500">
                {t('home.noMessages')}
              </Typography>
              <Typography variant="body2" color="text.disabled" sx={{ mt: 1 }}>
                {t('home.messagesWillAppear')}
              </Typography>
            </Paper>
          )}
        </Box>
      </CardContent>
    </Card>
  );
};

export default LogComponent;
