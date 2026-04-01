import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OrderService } from '../../core/services/order.service';

interface StatusStat {
  status: string;
  count: number;
  pct: number;
  color: string;
}

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div>
      <h2 class="text-2xl font-bold mb-6">Analytics</h2>
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-8">
        <div class="card text-center">
          <p class="text-4xl font-bold text-primary font-mono">{{ totalOrders() }}</p>
          <p class="text-gray-500 mt-1">Total Orders</p>
        </div>
        <div class="card text-center">
          <p class="text-4xl font-bold text-success font-mono">{{ deliveredOrders() }}</p>
          <p class="text-gray-500 mt-1">Delivered</p>
        </div>
        <div class="card text-center">
          <p class="text-4xl font-bold text-error font-mono">{{ cancelledOrders() }}</p>
          <p class="text-gray-500 mt-1">Cancelled</p>
        </div>
      </div>

      <div class="card">
        <h3 class="text-lg font-semibold mb-4">Order Status Distribution</h3>
        <div class="space-y-3">
          @for (stat of statusStats(); track stat.status) {
            <div class="flex items-center gap-3">
              <span class="w-32 text-sm text-gray-600">{{ stat.status }}</span>
              <div class="flex-1 bg-gray-100 rounded-full h-4">
                <div class="h-4 rounded-full" [style.width.%]="stat.pct" [style.background-color]="stat.color"></div>
              </div>
              <span class="w-8 text-sm font-mono text-right">{{ stat.count }}</span>
            </div>
          }
        </div>
      </div>
    </div>
  `
})
export class AnalyticsComponent implements OnInit {
  private orderService = inject(OrderService);
  totalOrders = signal(0);
  deliveredOrders = signal(0);
  cancelledOrders = signal(0);
  statusStats = signal<StatusStat[]>([]);

  private statusColors: Record<string, string> = {
    'PENDING': '#F59E0B', 'CONFIRMED': '#3B82F6', 'PAID': '#8B5CF6',
    'SHIPPED': '#06B6D4', 'DELIVERED': '#10B981', 'CANCELLED': '#EF4444', 'REFUNDED': '#6B7280'
  };

  ngOnInit() {
    const statuses = ['PENDING', 'CONFIRMED', 'PAID', 'SHIPPED', 'DELIVERED', 'CANCELLED'];
    const counts: Record<string, number> = {};
    let completed = 0;

    this.orderService.getOrders(0, 1).subscribe(res => {
      if (res.data) this.totalOrders.set(res.data.totalElements);
    });

    statuses.forEach(status => {
      this.orderService.getOrders(0, 1, status).subscribe(res => {
        if (res.data) {
          counts[status] = res.data.totalElements;
          if (status === 'DELIVERED') this.deliveredOrders.set(res.data.totalElements);
          if (status === 'CANCELLED') this.cancelledOrders.set(res.data.totalElements);
          completed++;
          if (completed === statuses.length) {
            const total = Object.values(counts).reduce((a, b) => a + b, 0);
            this.statusStats.set(statuses.map(s => ({
              status: s,
              count: counts[s] || 0,
              pct: total > 0 ? Math.round(((counts[s] || 0) / total) * 100) : 0,
              color: this.statusColors[s]
            })));
          }
        }
      });
    });
  }
}
