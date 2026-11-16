import { Injectable } from '@angular/core';

type AudioContextConstructor = new () => AudioContext;
export type NotificationTone = 'WIN' | 'LOSS' | 'PROMO';

@Injectable({ providedIn: 'root' })
export class NotificationSound {
  private context?: AudioContext;
  private unlocked = false;

  unlock(): void {
    if (this.unlocked) {
      return;
    }
    const legacyWindow = globalThis as unknown as {
      webkitAudioContext?: AudioContextConstructor;
    };
    const Constructor = globalThis.AudioContext ?? legacyWindow.webkitAudioContext;
    if (!Constructor) {
      return;
    }
    const context = (this.context ??= new Constructor());
    if (context.state === 'suspended') {
      void context.resume();
    }
    this.unlocked = true;
  }

  play(tone: NotificationTone = 'PROMO'): void {
    const context = this.context;
    if (!context || context.state !== 'running') {
      return;
    }
    const start = context.currentTime;
    if (tone === 'WIN') {
      this.tone(context, 523.25, start, 0.2, 0.22, 'sine');
      this.tone(context, 659.25, start + 0.14, 0.2, 0.22, 'sine');
      this.tone(context, 783.99, start + 0.28, 0.34, 0.24, 'sine');
      return;
    }
    if (tone === 'LOSS') {
      this.tone(context, 392.0, start, 0.24, 0.18, 'triangle');
      this.tone(context, 293.66, start + 0.22, 0.42, 0.16, 'triangle');
      return;
    }
    this.tone(context, 880, start, 0.18, 0.16, 'sine');
  }

  private tone(
    context: AudioContext,
    frequency: number,
    start: number,
    duration: number,
    volume: number,
    type: OscillatorType,
  ): void {
    const oscillator = context.createOscillator();
    const gain = context.createGain();
    oscillator.type = type;
    oscillator.frequency.setValueAtTime(frequency, start);
    gain.gain.setValueAtTime(0.0001, start);
    gain.gain.exponentialRampToValueAtTime(volume, start + 0.02);
    gain.gain.exponentialRampToValueAtTime(0.0001, start + duration);
    oscillator.connect(gain);
    gain.connect(context.destination);
    oscillator.start(start);
    oscillator.stop(start + duration);
  }
}
