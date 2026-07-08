import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { EmailService, DashboardStats } from '../services/email.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent implements OnInit {
  stats: DashboardStats | null = null;
  syncing = false;
  error = '';

  constructor(
    private emailService: EmailService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadStats();
  }

  loadStats(): void {
    this.emailService.getStats().subscribe({
      next: (stats) => (this.stats = stats),
      error: (err) => (this.error = err.error?.error || 'Failed to load stats'),
    });
  }

  syncEmails(): void {
    this.syncing = true;
    this.emailService.syncEmails().subscribe({
      next: (res) => {
        this.syncing = false;
        this.loadStats();
      },
      error: (err) => {
        this.syncing = false;
        this.error = err.error?.error || 'Sync failed';
      },
    });
  }

  connectGmail(): void {
    this.authService.connectGmail();
  }

  connectOutlook(): void {
    this.authService.connectOutlook();
  }

  goToEmails(): void {
    this.router.navigate(['/emails']);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  getCategoryKeys(): string[] {
    return this.stats?.byCategory ? Object.keys(this.stats.byCategory) : [];
  }
}
