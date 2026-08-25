import { Directive, ElementRef, Input, OnInit, OnDestroy, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

export type AnimationType =
  | 'fadeIn'
  | 'fadeInUp'
  | 'fadeInDown'
  | 'fadeInLeft'
  | 'fadeInRight'
  | 'scaleIn'
  | 'slideUp'
  | 'slideDown'
  | 'slideLeft'
  | 'slideRight'
  | 'bounceIn'
  | 'flipIn';

@Directive({
  selector: '[appAnimateOnScroll]',
  standalone: true
})
export class AnimateOnScrollDirective implements OnInit, OnDestroy {
  @Input() appAnimateOnScroll: AnimationType = 'fadeInUp';
  @Input() animationDelay: number = 0;
  @Input() animationDuration: number = 600;
  @Input() animationThreshold: number = 0.15;
  @Input() animateOnce: boolean = true;

  private observer: IntersectionObserver | null = null;
  private el = inject(ElementRef);
  private platformId = inject(PLATFORM_ID);
  private hasAnimated = false;

  ngOnInit() {
    if (!isPlatformBrowser(this.platformId)) return;

    // Set initial hidden state
    const element = this.el.nativeElement as HTMLElement;
    element.style.opacity = '0';
    element.style.transition = `opacity ${this.animationDuration}ms ease-out, transform ${this.animationDuration}ms ease-out`;
    element.style.transitionDelay = `${this.animationDelay}ms`;
    this.setInitialTransform(element);

    this.observer = new IntersectionObserver(
      (entries) => {
        entries.forEach(entry => {
          if (entry.isIntersecting) {
            if (!this.hasAnimated || !this.animateOnce) {
              this.animate(element);
              this.hasAnimated = true;
            }
          } else if (!this.animateOnce) {
            this.resetAnimation(element);
          }
        });
      },
      {
        threshold: this.animationThreshold,
        rootMargin: '0px 0px -50px 0px'
      }
    );

    this.observer.observe(element);
  }

  private setInitialTransform(element: HTMLElement) {
    switch (this.appAnimateOnScroll) {
      case 'fadeIn':
        break;
      case 'fadeInUp':
      case 'slideUp':
        element.style.transform = 'translateY(40px)';
        break;
      case 'fadeInDown':
      case 'slideDown':
        element.style.transform = 'translateY(-40px)';
        break;
      case 'fadeInLeft':
      case 'slideLeft':
        element.style.transform = 'translateX(-40px)';
        break;
      case 'fadeInRight':
      case 'slideRight':
        element.style.transform = 'translateX(40px)';
        break;
      case 'scaleIn':
        element.style.transform = 'scale(0.9)';
        break;
      case 'bounceIn':
        element.style.transform = 'scale(0.3)';
        break;
      case 'flipIn':
        element.style.transform = 'perspective(400px) rotateX(90deg)';
        break;
    }
  }

  private animate(element: HTMLElement) {
    element.style.opacity = '1';
    element.style.transform = 'translateY(0) translateX(0) scale(1) rotateX(0)';
  }

  private resetAnimation(element: HTMLElement) {
    element.style.opacity = '0';
    this.setInitialTransform(element);
    this.hasAnimated = false;
  }

  ngOnDestroy() {
    this.observer?.disconnect();
  }
}

