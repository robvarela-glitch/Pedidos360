import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Sidebar } from '../../shared/components/sidebar/sidebar';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, Sidebar],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard {
}