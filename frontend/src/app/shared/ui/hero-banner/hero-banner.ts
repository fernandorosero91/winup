import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NgIcon } from '@ng-icons/core';
import { BrandLogo } from '../brand-logo/brand-logo';

interface HeroSlide {
  src: string;
  label: string;
  position: string;
}

@Component({
  selector: 'app-hero-banner',
  imports: [RouterLink, NgIcon, BrandLogo],
  templateUrl: './hero-banner.html',
  styleUrl: './hero-banner.css',
})
export class HeroBanner implements OnInit, OnDestroy {
  readonly slides: HeroSlide[] = [
    { src: '/banner/estadio.jpg', label: 'Estadio', position: '50% 50%' },
    { src: '/banner/futbol-festejo.jpg', label: 'Festejo', position: '50% 35%' },
    { src: '/banner/futbol-balon.jpg', label: 'Fútbol', position: '50% 50%' },
    { src: '/banner/futbol-accion.jpg', label: 'Acción', position: '50% 45%' },
    { src: '/banner/baloncesto-mate.jpg', label: 'Mate', position: '50% 50%' },
    { src: '/banner/baloncesto-balon.jpg', label: 'Baloncesto', position: '50% 50%' },
    { src: '/banner/tenis.jpg', label: 'Tenis', position: '50% 50%' },
    { src: '/banner/formula1.jpg', label: 'Fórmula 1', position: '50% 55%' },
    { src: '/banner/winup-estadio.jpg', label: 'WINUP', position: '50% 50%' },
    { src: '/banner/multideporte.jpg', label: 'Multideporte', position: '50% 50%' },
  ];

  readonly active = signal(0);
  readonly paused = signal(false);
  private timer?: ReturnType<typeof setInterval>;

  ngOnInit(): void {
    this.start();
  }

  ngOnDestroy(): void {
    this.stop();
  }

  goTo(index: number): void {
    this.active.set(index);
    if (!this.paused()) {
      this.start();
    }
  }

  toggle(): void {
    this.paused.update((value) => !value);
    if (this.paused()) {
      this.stop();
    } else {
      this.start();
    }
  }

  private start(): void {
    this.stop();
    this.timer = setInterval(
      () => this.active.update((index) => (index + 1) % this.slides.length),
      6000,
    );
  }

  private stop(): void {
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = undefined;
    }
  }
}
