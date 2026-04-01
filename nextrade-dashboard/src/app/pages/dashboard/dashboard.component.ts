import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { OrderService } from '../../core/services/order.service';
import { ProductService } from '../../core/services/product.service';
import { Order } from '../../core/models/order.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div>
      <h2 class="text-2xl font-bold text-gray-900 mb-6">
        Welcome back, {{ authService.currentUser()?.firstName }}!
      </h2>
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <div class="card">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-gray-500">Total Orders</p>
              <p class="text-3xl font-bold font-mono">{{ totalOrders() }}</p>
            </div>
            <div class="h-12 w-12 bg-blue-100 rounded-full flex items-center justify-center">
              <svg class="h-6 w-6 text-primary" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/>
              </svg>
            </div>
          </div>
        </div>
        <div class="card">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-gray-500">Products</p>
              <p class="text-3xl font-bold font-mono">{{ totalProducts() }}</p>
            </div>
            <div class="h-12 w-12 bg-green-100 rounded-full flex items-center justify-center">
              <svg class="h-6 w-6 text-success" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"/>
              </svg>
            </div>
          </div>
        </div>
        <div class="card">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-gray-500">Pending Orders</p>
              <p class="text-3xl font-bold text-warning font-mono">{{ pendingOrders() }}</p>
            </div>
            <div class="h-12 w-12 bg-yellow-100 rounded-full flex items-center justify-center">
              <svg class="h-6 w-6 text-warning" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
              </svg>
            </div>
          </div>
        </div>
        <div class="card">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-gray-500">My Role</p>
              <p class="text-xl font-bold">{{ authService.currentUser()?.role }}</p>
            </div>
            <div class="h-12 w-12 bg-purple-100 rounded-full flex items-center justify-center">
              <svg class="h-6 w-6 text-purple-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/>
              </svg>
            </div>
          </div>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div class="card">
          <h3 class="text-lg font-semibold mb-4">Quick Actions</h3>
          <div class="space-y-3">
            <a routerLink="/dashboard/orders/new" class="flex items-center p-3 bg-blue-50 rounded-lg hover:bg-blue-100 transition-colors text-sm font-medium">
              + Create New Order
            </a>
            <a routerLink="/dashboard/products" class="flex items-center p-3 bg-green-50 rounded-lg hover:bg-green-100 transition-colors text-sm font-medium">
              Browse Products
            </a>
            <a routerLink="/dashboard/orders" class="flex items-center p-3 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors text-sm font-medium">
              View All Orders
            </a>
          </div>
        </div>

        <div class="card">
          <h3 class="text-lg font-semibold mb-4">Recent Orders</h3>
          @if (recentOrders().length > 0) {
            <div class="space-y-2">
              @for (order of recentOrders(); track order.id) {
                <div class="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                  <div>
                    <p class="text-sm font-medium font-mono">{{ order.orderNumber }}</p>
                    <p class="text-xs text-gray-500">\${{ order.totalAmount }}</p>
                  </div>
                  <span class="badge badge-info">{{ order.status }}</span>
                </div>
              }
            </div>
          } @else {
            <p class="text-gray-500 text-sm">No orders yet</p>
          }
        </div>
      </div>
    </div>
  `
})
export class DashboardComponent implements OnInit {
  authService = inject(AuthService);
  private orderService = inject(OrderService);
  private productService = inject(ProductService);

  totalOrders = signal(0);
  pendingOrders = signal(0);
  totalProducts = signal(0);
  recentOrders = signal<Order[]>([]);

  ngOnInit() {
    this.orderService.getOrders(0, 5).subscribe(res => {
      if (res.data) {
        this.totalOrders.set(res.data.totalElements);
        this.recentOrders.set(res.data.content.slice(0, 5));
      }
    });
    this.orderService.getOrders(0, 1, 'PENDING').subscribe(res => {
      if (res.data) this.pendingOrders.set(res.data.totalElements);
    });
    this.productService.getProducts(0, 1).subscribe(res => {
      if (res.data) this.totalProducts.set(res.data.totalElements);
    });
  }
}
