import { useCallback, useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAuthStore } from '../store/authStore';

export interface PresenceUser {
  userId: string;
  fullName: string;
  color: string;
}

export interface RemoteCursor {
  userId: string;
  fullName: string;
  color: string;
  x: number;
  y: number;
}

interface RemoteStateMessage {
  userId: string;
  schemaVersion: string;
  canvasStateJson: string;
}

const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws';
const CURSOR_THROTTLE_MS = 60;

/**
 * Connects to the project's live collaboration channel over STOMP/SockJS.
 * Handles presence (who's online), remote cursor positions, and remote
 * full-state broadcasts (see CollabController's Javadoc on the backend for
 * the last-write-wins limitation of the state channel).
 */
export function useCollabSocket(projectId: string | undefined) {
  const accessToken = useAuthStore((s) => s.accessToken);
  const selfUserId = useAuthStore((s) => s.user?.userId);
  const clientRef = useRef<Client | null>(null);
  const lastCursorSentAt = useRef(0);
  const remoteStateHandlerRef = useRef<((msg: RemoteStateMessage) => void) | null>(null);

  const [presence, setPresence] = useState<PresenceUser[]>([]);
  const [remoteCursors, setRemoteCursors] = useState<Record<string, RemoteCursor>>({});
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    if (!projectId || !accessToken) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(`${WS_URL}?token=${encodeURIComponent(accessToken)}`) as any,
      reconnectDelay: 3000,
      onConnect: () => {
        setConnected(true);
        client.publish({ destination: `/app/projects/${projectId}/join`, body: '{}' });

        client.subscribe(`/topic/projects/${projectId}/presence`, (message) => {
          const data = JSON.parse(message.body) as { users: PresenceUser[] };
          setPresence(data.users);
        });

        client.subscribe(`/topic/projects/${projectId}/cursors`, (message) => {
          const data = JSON.parse(message.body) as RemoteCursor;
          if (data.userId === selfUserId) return; // don't render our own broadcast cursor back to us
          setRemoteCursors((prev) => ({ ...prev, [data.userId]: data }));
        });

        client.subscribe(`/topic/projects/${projectId}/state`, (message) => {
          const data = JSON.parse(message.body) as RemoteStateMessage;
          if (data.userId === selfUserId) return;
          remoteStateHandlerRef.current?.(data);
        });
      },
      onDisconnect: () => setConnected(false),
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      clientRef.current = null;
      setPresence([]);
      setRemoteCursors({});
      setConnected(false);
    };
  }, [projectId, accessToken, selfUserId]);

  const sendCursor = useCallback(
    (x: number, y: number) => {
      if (!projectId || !clientRef.current?.connected) return;
      const now = Date.now();
      if (now - lastCursorSentAt.current < CURSOR_THROTTLE_MS) return;
      lastCursorSentAt.current = now;
      clientRef.current.publish({ destination: `/app/projects/${projectId}/cursor`, body: JSON.stringify({ x, y }) });
    },
    [projectId]
  );

  const sendState = useCallback(
    (schemaVersion: string, canvasStateJson: string) => {
      if (!projectId || !clientRef.current?.connected) return;
      clientRef.current.publish({
        destination: `/app/projects/${projectId}/state`,
        body: JSON.stringify({ schemaVersion, canvasStateJson }),
      });
    },
    [projectId]
  );

  const onRemoteState = useCallback((handler: (msg: RemoteStateMessage) => void) => {
    remoteStateHandlerRef.current = handler;
  }, []);

  return { presence, remoteCursors, connected, sendCursor, sendState, onRemoteState };
}
