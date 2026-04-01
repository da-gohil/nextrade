import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductService } from '../../../core/services/product.service';

@Component({
  selector: 'app-product-manage',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="max-w-2xl mx-auto">
      <h2 class="text-2xl font-bold mb-6">Add New Product</h2>
      <div class="card">
        @if (success()) { <div class="mb-4 p-3 bg-green-50 text-green-700 rounded text-sm">Product created successfully!</div> }
        @if (error()) { <div class="mb-4 p-3 bg-red-50 text-red-700 rounded text-sm">{{ error() }}</div> }
        <form (ngSubmit)="submit()">
          <div class="grid grid-cols-2 gap-4 mb-4">
            <div><label class="block text-sm font-medium mb-1">SKU</label><input [(ngModel)]="form.sku" name="sku" required class="form-input"></div>
            <div><label class="block text-sm font-medium mb-1">Name</label><input [(ngModel)]="form.name" name="name" required class="form-input"></div>
          </div>
          <div class="mb-4"><label class="block text-sm font-medium mb-1">Description</label><textarea [(ngModel)]="form.description" name="description" rows="3" class="form-input"></textarea></div>
          <div class="grid grid-cols-2 gap-4 mb-4">
            <div><label class="block text-sm font-medium mb-1">Category ID</label><input type="number" [(ngModel)]="form.categoryId" name="categoryId" required class="form-input"></div>
            <div><label class="block text-sm font-medium mb-1">Price</label><input type="number" step="0.01" [(ngModel)]="form.price" name="price" required class="form-input"></div>
          </div>
          <div class="grid grid-cols-2 gap-4 mb-6">
            <div><label class="block text-sm font-medium mb-1">Stock Quantity</label><input type="number" [(ngModel)]="form.stockQuantity" name="stockQuantity" class="form-input"></div>
            <div><label class="block text-sm font-medium mb-1">Low Stock Threshold</label><input type="number" [(ngModel)]="form.lowStockThreshold" name="lowStockThreshold" class="form-input"></div>
          </div>
          <button type="submit" [disabled]="loading()" class="w-full btn-primary">{{ loading() ? 'Creating...' : 'Create Product' }}</button>
        </form>
      </div>
    </div>
  `
})
export class ProductManageComponent {
  private productService = inject(ProductService);
  form = { sku: '', name: '', description: '', categoryId: 1, price: 0, stockQuantity: 0, lowStockThreshold: 10 };
  loading = signal(false);
  success = signal(false);
  error = signal('');

  submit() {
    this.loading.set(true);
    this.error.set('');
    this.productService.createProduct(this.form).subscribe({
      next: () => { this.success.set(true); this.loading.set(false); },
      error: err => { this.error.set(err.error?.message || 'Failed'); this.loading.set(false); }
    });
  }
}
