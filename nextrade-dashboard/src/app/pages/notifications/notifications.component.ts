import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NotificationService } from '../../core/services/notification.service';
import { Notification } from '../../core/models/notification.model';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div>
      <div class="flex items-center justify-between mb-6">
        <h2 class="text-2xl font-bold">Notifications</h2>
        <span class="badge badge-error">{{ notificationService.unreadCount() }} unread</span>
      </div>
      <div class="space-y-3">
        @if (notifications().length === 0) {
          <div class="card text-center py-8 text-gray-500">No notifications</div>
        }
        @for (n of notifications(); track n.id) {
          <div class="card flex items-start gap-4" [class.opacity-60]="n.isRead">
            <div class="h-10 w-10 rounded-full flex items-center justify-center flex-shrink-0"
                 [style.background-color]="getTypeColor(n.type) + '20'">
              <div class="h-3 w-3 rounded-full" [style.background-color]="getTypeColor(n.type)"></div>
            </div>
            <div class="flex-1">
              <div class="flex items-center justify-between">
                <p class="font-semibold text-sm">{{ n.title }}</p>
                <p class="text-xs text-gray-500">{{ n.createdAt | date:'short' }}</p>
              </div>
              <p class="text-sm text-gray-600 mt-1">{{ n.message }}</p>
              <span class="badge badge-gray text-xs mt-1">{{ n.type }}</span>
            </div>
            @if (!n.isRead) {
              <button (click)="markRead(n)" class="text-primary text-sm hover:underline flex-shrink-0">Mark read</button>
            }
          </div>
        }
      </div>
    </div>
  `
})
export class NotificationsComponent implements OnInit {
  notificationService = inject(NotificationService);
  notifications = signal<Notification[]>([]);
  ngOnInit() {
    this.notificationService.getNotifications().subscribe(res => {
      if (res.data) this.notifications.set(res.data.content);
    });
    this.notificationService.getUnreadCount().subscribe();
  }
  markRead(n: Notification) {
    this.notificationService.markAsRead(n.id).subscribe(() => {
      this.notifications.update(list => list.map(x => x.id === n.id ? { ...x, isRead: true } : x));
    });
  }
  getTypeColor(type: string): string {
    const map: Record<string, string> = {
      'ORDER_CONFIRMED': '#3B82F6', 'ORDER_SHIPPED': '#8B5CF6', 'PAYMENT_SUCCESS': '#10B981',
      'PAYMENT_FAILED': '#EF4444', 'LOW_STOCK': '#F59E0B', 'ORDER_CANCELLED': '#EF4444'
    };
    return map[type] || '#6B7280';
  }
}
