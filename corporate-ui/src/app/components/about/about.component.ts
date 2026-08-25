import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AnimateOnScrollDirective } from '../../shared/directives/animate-on-scroll.directive';

@Component({
  selector: 'app-about',
  standalone: true,
  imports: [RouterLink, AnimateOnScrollDirective],
  templateUrl: './about.component.html',
  styleUrl: './about.component.css'
})
export class AboutComponent {}
