import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PaymentService } from '../../core/services/payment.service';
import { Payment } from '../../core/models/payment.model';

@Component({
  selector: 'app-payment-history',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div>
      <h2 class="text-2xl font-bold mb-6">Payment History</h2>
      <div class="card">
        @if (payments().length === 0) {
          <p class="text-gray-500 text-center py-8">No payments found</p>
        } @else {
          <table class="w-full">
            <thead>
              <tr class="border-b">
                <th class="text-left py-3 text-sm font-medium text-gray-500">Payment #</th>
                <th class="text-left py-3 text-sm font-medium text-gray-500">Order ID</th>
                <th class="text-left py-3 text-sm font-medium text-gray-500">Method</th>
                <th class="text-right py-3 text-sm font-medium text-gray-500">Amount</th>
                <th class="text-left py-3 text-sm font-medium text-gray-500">Status</th>
                <th class="text-left py-3 text-sm font-medium text-gray-500">Date</th>
              </tr>
            </thead>
            <tbody>
              @for (p of payments(); track p.id) {
                <tr class="border-b hover:bg-gray-50">
                  <td class="py-3 font-mono text-sm">{{ p.paymentNumber }}</td>
                  <td class="py-3 font-mono text-sm">{{ p.orderId }}</td>
                  <td class="py-3 text-sm">{{ p.method }}</td>
                  <td class="py-3 text-right font-mono font-bold">\${{ p.amount }}</td>
                  <td class="py-3"><span class="badge" [class]="getClass(p.status)">{{ p.status }}</span></td>
                  <td class="py-3 text-sm text-gray-500">{{ p.createdAt | date:'short' }}</td>
                </tr>
              }
            </tbody>
          </table>
        }
      </div>
    </div>
  `
})
export class PaymentHistoryComponent implements OnInit {
  private paymentService = inject(PaymentService);
  payments = signal<Payment[]>([]);
  ngOnInit() {
    this.paymentService.getTransactions().subscribe(res => {
      if (res.data) this.payments.set(res.data.content);
    });
  }
  getClass(status: string): string {
    const map: Record<string, string> = {
      'COMPLETED': 'badge-success', 'FAILED': 'badge-error', 'PENDING': 'badge-warning', 'REFUNDED': 'badge-gray'
    };
    return map[status] || 'badge-gray';
  }
}
