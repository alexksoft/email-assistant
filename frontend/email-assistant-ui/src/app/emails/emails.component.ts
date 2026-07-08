import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { EmailService, Email } from '../services/email.service';

@Component({
  selector: 'app-emails',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './emails.component.html',
  styleUrl: './emails.component.scss',
})
export class EmailsComponent implements OnInit {
  emails: Email[] = [];
  selectedEmail: Email | null = null;
  loading = true;

  constructor(private emailService: EmailService, private router: Router) {}

  ngOnInit(): void {
    this.emailService.getEmails().subscribe({
      next: (emails) => {
        this.emails = emails.sort((a, b) => (a.priority || 5) - (b.priority || 5));
        this.loading = false;
      },
      error: () => (this.loading = false),
    });
  }

  selectEmail(email: Email): void {
    this.selectedEmail = email;
  }

  getPriorityLabel(p: number): string {
    const labels: Record<number, string> = { 1: '🔴 Critical', 2: '🟠 High', 3: '🟡 Medium', 4: '🟢 Low', 5: '⚪ Minimal' };
    return labels[p] || '⚪ Unknown';
  }

  getCategoryColor(cat: string): string {
    const colors: Record<string, string> = {
      urgent: '#e53935', sales: '#43a047', support: '#1e88e5',
      newsletter: '#8e24aa', personal: '#fb8c00', other: '#757575',
    };
    return colors[cat] || '#757575';
  }

  back(): void {
    this.router.navigate(['/dashboard']);
  }
}
