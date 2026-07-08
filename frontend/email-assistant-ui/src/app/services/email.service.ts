import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Email {
  userId: string;
  emailId: string;
  subject: string;
  from: string;
  to: string;
  body: string;
  snippet: string;
  category: string;
  priority: number;
  draftReply: string;
  status: string;
  receivedAt: number;
  processedAt: number;
  provider: string;
}

export interface DashboardStats {
  total: number;
  processed: number;
  unprocessed: number;
  byCategory: Record<string, number>;
  byPriority: Record<string, number>;
}

@Injectable({ providedIn: 'root' })
export class EmailService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  syncEmails(): Observable<{ synced: number }> {
    return this.http.post<{ synced: number }>(`${this.apiUrl}/emails/sync`, {});
  }

  getEmails(): Observable<Email[]> {
    return this.http.get<Email[]>(`${this.apiUrl}/emails`);
  }

  getEmail(id: string): Observable<Email> {
    return this.http.get<Email>(`${this.apiUrl}/emails/${id}`);
  }

  processEmail(id: string): Observable<Email> {
    return this.http.post<Email>(`${this.apiUrl}/emails/${id}/process`, {});
  }

  getStats(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(`${this.apiUrl}/dashboard/stats`);
  }
}
