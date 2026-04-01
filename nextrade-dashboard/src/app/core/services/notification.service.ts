import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { ApiResponse, PageResponse } from '../models/api.model';
import { Notification } from '../models/notification.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  unreadCount = signal(0);

  constructor(private http: HttpClient) {}

  getNotifications(page = 0, size = 20): Observable<ApiResponse<PageResponse<Notification>>> {
    return this.http.get<ApiResponse<PageResponse<Notification>>>(`${environment.apiUrl}/notifications?page=${page}&size=${size}`);
  }

  markAsRead(id: number): Observable<ApiResponse<Notification>> {
    return this.http.put<ApiResponse<Notification>>(`${environment.apiUrl}/notifications/${id}/read`, {}).pipe(
      tap(() => this.unreadCount.update(c => Math.max(0, c - 1)))
    );
  }

  getUnreadCount(): Observable<ApiResponse<{count: number}>> {
    return this.http.get<ApiResponse<{count: number}>>(`${environment.apiUrl}/notifications/unread-count`).pipe(
      tap(res => this.unreadCount.set(res.data.count))
    );
  }
}
