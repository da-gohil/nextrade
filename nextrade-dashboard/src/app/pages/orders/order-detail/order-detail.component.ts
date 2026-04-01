import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { OrderService } from '../../../core/services/order.service';
import { Order } from '../../../core/models/order.model';

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div>
      <div class="flex items-center gap-4 mb-6">
        <a routerLink="/dashboard/orders" class="text-gray-500 hover:text-gray-700">← Back</a>
        <h2 class="text-2xl font-bold">Order Details</h2>
      </div>

      @if (order()) {
        <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div class="lg:col-span-2 space-y-6">
            <div class="card">
              <div class="flex justify-between items-start mb-4">
                <div>
                  <h3 class="text-lg font-semibold font-mono">{{ order()!.orderNumber }}</h3>
                  <p class="text-sm text-gray-500">{{ order()!.createdAt | date:'medium' }}</p>
                </div>
                <span class="badge badge-info text-base">{{ order()!.status }}</span>
              </div>
              <p class="text-sm text-gray-600"><strong>Shipping:</strong> {{ order()!.shippingAddress }}</p>
              @if (order()!.notes) {
                <p class="text-sm text-gray-600 mt-2"><strong>Notes:</strong> {{ order()!.notes }}</p>
              }
            </div>

            <div class="card">
              <h3 class="text-lg font-semibold mb-4">Order Items</h3>
              <table class="w-full">
                <thead>
                  <tr class="border-b">
                    <th class="text-left py-2 text-sm font-medium text-gray-500">Product</th>
                    <th class="text-right py-2 text-sm font-medium text-gray-500">Qty</th>
                    <th class="text-right py-2 text-sm font-medium text-gray-500">Unit Price</th>
                    <th class="text-right py-2 text-sm font-medium text-gray-500">Subtotal</th>
                  </tr>
                </thead>
                <tbody>
                  @for (item of order()!.items; track item.productId) {
                    <tr class="border-b">
                      <td class="py-2 text-sm">{{ item.productName }}</td>
                      <td class="py-2 text-sm text-right">{{ item.quantity }}</td>
                      <td class="py-2 text-sm text-right font-mono">\${{ item.unitPrice }}</td>
                      <td class="py-2 text-sm text-right font-mono">\${{ item.subtotal }}</td>
                    </tr>
                  }
                  <tr>
                    <td colspan="3" class="py-3 text-right font-semibold">Total:</td>
                    <td class="py-3 text-right font-bold font-mono text-lg">\${{ order()!.totalAmount }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <div class="space-y-6">
            <div class="card">
              <h3 class="text-lg font-semibold mb-4">Status Timeline</h3>
              <div class="space-y-3">
                @for (h of order()!.statusHistory || []; track h.createdAt) {
                  <div class="flex items-start gap-3">
                    <div class="h-2 w-2 rounded-full bg-primary mt-2 flex-shrink-0"></div>
                    <div>
                      <p class="text-sm font-medium">{{ h.toStatus }}</p>
                      @if (h.note) { <p class="text-xs text-gray-500">{{ h.note }}</p> }
                      <p class="text-xs text-gray-400">{{ h.createdAt | date:'short' }}</p>
                    </div>
                  </div>
                }
              </div>
            </div>
          </div>
        </div>
      } @else {
        <div class="card text-center py-8 text-gray-500">Loading order...</div>
      }
    </div>
  `
})
export class OrderDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private orderService = inject(OrderService);
  order = signal<Order | null>(null);

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.orderService.getOrder(id).subscribe(res => this.order.set(res.data));
  }
}
