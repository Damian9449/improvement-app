import React, { useEffect, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Box, Button, Typography, List, ListItem, Fade, CircularProgress } from '@mui/material';
import {
  Message as MessageIcon,
  Circle as CircleIcon,
  ExpandLess as ExpandLessIcon,
  ExpandMore as ExpandMoreIcon,
} from '@mui/icons-material';
import { useTranslation } from 'react-i18next';

const LogComponent = () => {
  const { t } = useTranslation();
  const [messages, setMessages] = useState([]);
  const [isConnected, setIsConnected] = useState(false);
  const [isConnecting, setIsConnecting] = useState(true);
  const [expanded, setExpanded] = useState(false);

  const host_url = import.meta.env.VITE_API_URL;

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS(`${host_url}ws`),
      reconnectDelay: 5000,
      onConnect: () => {
        setIsConnected(true);
        setIsConnecting(false);
        client.subscribe('/user/queue/messages', (message) => {
          // Czas zapisujemy w chwili nadejścia — inaczej każdy render pokazywałby bieżącą godzinę
          const entry = {
            id: `${Date.now()}-${Math.random()}`,
            text: message.body,
            time: new Date().toLocaleTimeString(),
          };
          setMessages((prevMessages) => {
            const updatedMessages = [entry, ...prevMessages];
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
    <Box>
      <Box display="flex" alignItems="center" justifyContent="space-between" mb={1}>
        <Box display="flex" alignItems="center" gap={1}>
          <MessageIcon fontSize="small" sx={{ color: 'text.secondary' }} />
          <Typography variant="body2" fontWeight="600" color="text.secondary">
            {t('home.activityLog')}
          </Typography>
        </Box>

        {/* Connection Status */}
        {isConnecting ? (
          <Box display="flex" alignItems="center" gap={1}>
            <CircularProgress size={12} sx={{ color: 'warning.main' }} />
            <Typography variant="caption" color="warning.main">
              {t('home.connecting')}
            </Typography>
          </Box>
        ) : (
          <Box display="flex" alignItems="center" gap={0.5}>
            <CircleIcon sx={{ fontSize: 10, color: isConnected ? 'success.main' : 'error.main' }} />
            <Typography variant="caption" color={isConnected ? 'success.main' : 'error.main'}>
              {isConnected ? t('home.connected') : t('home.disconnected')}
            </Typography>
          </Box>
        )}
      </Box>

      {messages.length ? (
        <List dense disablePadding sx={{ maxHeight: 240, overflow: 'auto' }}>
          {(expanded ? messages : messages.slice(0, 1)).map((msg) => (
            <Fade in={true} timeout={500} key={msg.id}>
              <ListItem disableGutters sx={{ py: 0.25, gap: 1.5, alignItems: 'baseline' }}>
                <Typography
                  variant="caption"
                  color="text.disabled"
                  sx={{ fontFamily: 'monospace', flexShrink: 0 }}
                >
                  {msg.time}
                </Typography>
                <Typography variant="body2" sx={{ wordBreak: 'break-word' }}>
                  {msg.text}
                </Typography>
              </ListItem>
            </Fade>
          ))}
        </List>
      ) : (
        <Typography variant="caption" color="text.disabled">
          {t('home.messagesWillAppear')}
        </Typography>
      )}

      {messages.length > 1 && (
        <Button
          size="small"
          onClick={() => setExpanded((prev) => !prev)}
          endIcon={expanded ? <ExpandLessIcon /> : <ExpandMoreIcon />}
          sx={{ mt: 0.5, textTransform: 'none' }}
        >
          {expanded ? t('home.hideLog') : t('home.fullLog')}
        </Button>
      )}
    </Box>
  );
};

export default LogComponent;
