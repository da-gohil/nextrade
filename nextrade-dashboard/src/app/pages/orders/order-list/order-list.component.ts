import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { OrderService } from '../../../core/services/order.service';
import { Order } from '../../../core/models/order.model';

@Component({
  selector: 'app-order-list',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  template: `
    <div>
      <div class="flex items-center justify-between mb-6">
        <h2 class="text-2xl font-bold">Orders</h2>
        <a routerLink="/dashboard/orders/new" class="btn-primary">+ New Order</a>
      </div>

      <div class="card mb-4">
        <div class="flex gap-3">
          <select [(ngModel)]="statusFilter" (ngModelChange)="loadOrders()" class="form-input w-48">
            <option value="">All Statuses</option>
            @for (s of statuses; track s) { <option [value]="s">{{ s }}</option> }
          </select>
        </div>
      </div>

      <div class="card">
        @if (loading()) {
          <div class="text-center py-8 text-gray-500">Loading orders...</div>
        } @else if (orders().length === 0) {
          <div class="text-center py-8 text-gray-500">No orders found</div>
        } @else {
          <table class="w-full">
            <thead>
              <tr class="border-b">
                <th class="text-left py-3 px-2 text-sm font-medium text-gray-500">Order #</th>
                <th class="text-left py-3 px-2 text-sm font-medium text-gray-500">Status</th>
                <th class="text-left py-3 px-2 text-sm font-medium text-gray-500">Amount</th>
                <th class="text-left py-3 px-2 text-sm font-medium text-gray-500">Date</th>
                <th class="text-left py-3 px-2 text-sm font-medium text-gray-500">Actions</th>
              </tr>
            </thead>
            <tbody>
              @for (order of orders(); track order.id) {
                <tr class="border-b hover:bg-gray-50">
                  <td class="py-3 px-2 font-mono text-sm">{{ order.orderNumber }}</td>
                  <td class="py-3 px-2"><span class="badge" [class]="getStatusClass(order.status)">{{ order.status }}</span></td>
                  <td class="py-3 px-2 font-mono">\${{ order.totalAmount }}</td>
                  <td class="py-3 px-2 text-sm text-gray-500">{{ order.createdAt | date:'short' }}</td>
                  <td class="py-3 px-2">
                    <a [routerLink]="['/dashboard/orders', order.id]" class="text-primary text-sm hover:underline">View</a>
                  </td>
                </tr>
              }
            </tbody>
          </table>
          <div class="mt-4 flex items-center justify-between text-sm text-gray-500">
            <span>{{ totalElements() }} total orders</span>
            <div class="flex gap-2">
              <button (click)="prevPage()" [disabled]="page() === 0" class="btn-secondary">Previous</button>
              <span class="px-3 py-2">Page {{ page() + 1 }}</span>
              <button (click)="nextPage()" [disabled]="(page() + 1) >= totalPages()" class="btn-secondary">Next</button>
            </div>
          </div>
        }
      </div>
    </div>
  `
})
export class OrderListComponent implements OnInit {
  private orderService = inject(OrderService);
  orders = signal<Order[]>([]);
  loading = signal(false);
  statusFilter = '';
  page = signal(0);
  totalPages = signal(0);
  totalElements = signal(0);
  statuses = ['PENDING', 'CONFIRMED', 'PAID', 'PREPARING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'REFUNDED'];

  ngOnInit() { this.loadOrders(); }

  loadOrders() {
    this.loading.set(true);
    this.orderService.getOrders(this.page(), 20, this.statusFilter || undefined).subscribe({
      next: res => {
        if (res.data) {
          this.orders.set(res.data.content);
          this.totalPages.set(res.data.totalPages);
          this.totalElements.set(res.data.totalElements);
        }
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  prevPage() { if (this.page() > 0) { this.page.update(p => p - 1); this.loadOrders(); } }
  nextPage() { if (this.page() + 1 < this.totalPages()) { this.page.update(p => p + 1); this.loadOrders(); } }

  getStatusClass(status: string): string {
    const map: Record<string, string> = {
      'PENDING': 'badge-warning', 'CONFIRMED': 'badge-info', 'PAID': 'badge-success',
      'SHIPPED': 'badge-info', 'DELIVERED': 'badge-success', 'CANCELLED': 'badge-error', 'REFUNDED': 'badge-gray'
    };
    return map[status] || 'badge-gray';
  }
}
