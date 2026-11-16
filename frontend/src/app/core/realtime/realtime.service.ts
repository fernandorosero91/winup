import { Injectable } from '@angular/core';

export interface RealtimeMatchEvent {
  matchId: string;
  status: string;
  home: string;
  away: string;
  homeScore: number;
  awayScore: number;
  minute: number;
  winner: string;
}

@Injectable({ providedIn: 'root' })
export class RealtimeService {
  private socket?: WebSocket;
  private retry?: ReturnType<typeof setTimeout>;
  private closed = true;

  connect(
    handler: (event: RealtimeMatchEvent) => void,
    onNotifications?: () => void,
  ): void {
    this.closed = false;
    this.open(handler, onNotifications);
  }

  disconnect(): void {
    this.closed = true;
    if (this.retry) {
      clearTimeout(this.retry);
      this.retry = undefined;
    }
    this.socket?.close();
    this.socket = undefined;
  }

  private open(
    handler: (event: RealtimeMatchEvent) => void,
    onNotifications?: () => void,
  ): void {
    if (this.closed) {
      return;
    }
    const protocol = globalThis.location?.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = globalThis.location?.host ?? 'localhost:8080';
    const socket = new WebSocket(`${protocol}//${host}/ws/events`);
    this.socket = socket;

    socket.onmessage = (message) => {
      try {
        const event = JSON.parse(message.data) as RealtimeMatchEvent | { type?: string };
        if ('type' in event && event.type === 'NOTIFICATIONS_UPDATED') {
          onNotifications?.();
          return;
        }
        handler(event as RealtimeMatchEvent);
      } catch {
        return;
      }
    };

    socket.onclose = () => {
      if (!this.closed) {
        this.retry = setTimeout(() => this.open(handler, onNotifications), 5000);
      }
    };

    socket.onerror = () => {
      socket.close();
    };
  }
}
