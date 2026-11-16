import { NotificationSound } from './notification-sound';

class FakeParam {
  setValueAtTime(): void {}
  exponentialRampToValueAtTime(): void {}
}

class FakeOscillator {
  type = 'sine';
  frequency = new FakeParam();
  connect(): void {}
  start(): void {}
  stop(): void {}
}

class FakeGain {
  gain = new FakeParam();
  connect(): void {}
}

class FakeContext {
  state = 'running';
  currentTime = 0;
  destination = {};
  oscillators: FakeOscillator[] = [];

  createOscillator(): FakeOscillator {
    const oscillator = new FakeOscillator();
    this.oscillators.push(oscillator);
    return oscillator;
  }

  createGain(): FakeGain {
    return new FakeGain();
  }

  resume(): Promise<void> {
    this.state = 'running';
    return Promise.resolve();
  }
}

describe('NotificationSound', () => {
  it('does nothing until the audio context is unlocked by a gesture', () => {
    const service = new NotificationSound();

    expect(() => service.play()).not.toThrow();
  });

  it('plays a winners fanfare of three tones', () => {
    const context = new FakeContext();
    class FakeAudioContext extends FakeContext {}
    const original = globalThis.AudioContext;
    globalThis.AudioContext = FakeAudioContext as unknown as typeof AudioContext;

    const service = new NotificationSound();
    service.unlock();
    service.play('WIN');

    const created = (service as unknown as { context: FakeContext }).context;
    expect(created).toBeInstanceOf(FakeAudioContext);
    expect(created.oscillators).toHaveLength(3);

    globalThis.AudioContext = original;
  });

  it('plays a descending two tone alert for a loss', () => {
    const context = new FakeContext();
    class FakeAudioContext extends FakeContext {}
    const original = globalThis.AudioContext;
    globalThis.AudioContext = FakeAudioContext as unknown as typeof AudioContext;

    const service = new NotificationSound();
    service.unlock();
    service.play('LOSS');

    const created = (service as unknown as { context: FakeContext }).context;
    expect(created.oscillators).toHaveLength(2);

    globalThis.AudioContext = original;
  });
});
