import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductService } from '../../core/services/product.service';
import { Product } from '../../core/models/product.model';

@Component({
  selector: 'app-inventory',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div>
      <h2 class="text-2xl font-bold mb-6">Inventory Management</h2>
      <div class="card">
        <table class="w-full">
          <thead>
            <tr class="border-b">
              <th class="text-left py-3 text-sm font-medium text-gray-500">Product</th>
              <th class="text-left py-3 text-sm font-medium text-gray-500">SKU</th>
              <th class="text-right py-3 text-sm font-medium text-gray-500">Stock</th>
              <th class="text-right py-3 text-sm font-medium text-gray-500">Reserved</th>
              <th class="text-right py-3 text-sm font-medium text-gray-500">Available</th>
              <th class="text-left py-3 text-sm font-medium text-gray-500">Status</th>
              <th class="text-left py-3 text-sm font-medium text-gray-500">Adjust</th>
            </tr>
          </thead>
          <tbody>
            @for (p of products(); track p.id) {
              <tr class="border-b hover:bg-gray-50">
                <td class="py-3 font-medium text-sm">{{ p.name }}</td>
                <td class="py-3 font-mono text-sm text-gray-500">{{ p.sku }}</td>
                <td class="py-3 text-right font-mono">{{ p.stockQuantity }}</td>
                <td class="py-3 text-right font-mono text-warning">{{ p.reservedQuantity }}</td>
                <td class="py-3 text-right font-mono font-bold">{{ p.availableQuantity }}</td>
                <td class="py-3">
                  @if (p.availableQuantity <= p.lowStockThreshold) {
                    <span class="badge badge-error">Low Stock</span>
                  } @else {
                    <span class="badge badge-success">OK</span>
                  }
                </td>
                <td class="py-3">
                  <div class="flex items-center gap-2">
                    <input type="number" [(ngModel)]="adjustments[p.id]" class="form-input w-20 text-sm" placeholder="+-qty">
                    <button (click)="adjust(p)" class="btn-secondary text-xs py-1 px-2">Apply</button>
                  </div>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    </div>
  `
})
export class InventoryComponent implements OnInit {
  private productService = inject(ProductService);
  products = signal<Product[]>([]);
  adjustments: Record<number, number> = {};
  ngOnInit() { this.load(); }
  load() {
    this.productService.getProducts(0, 100).subscribe(res => {
      if (res.data) this.products.set(res.data.content);
    });
  }
  adjust(product: Product) {
    const qty = this.adjustments[product.id];
    if (qty === undefined || qty === 0) return;
    this.productService.adjustStock(product.id, qty, 'Manual adjustment').subscribe(() => {
      this.adjustments[product.id] = 0;
      this.load();
    });
  }
}
