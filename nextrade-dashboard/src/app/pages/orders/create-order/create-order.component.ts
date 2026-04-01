import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { OrderService } from '../../../core/services/order.service';
import { ProductService } from '../../../core/services/product.service';
import { Product } from '../../../core/models/product.model';

interface CartItem {
  productId: number;
  productName: string;
  quantity: number;
  unitPrice: number;
}

@Component({
  selector: 'app-create-order',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div>
      <h2 class="text-2xl font-bold mb-6">Create New Order</h2>
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div class="lg:col-span-2">
          <div class="card mb-4">
            <h3 class="text-lg font-semibold mb-4">Select Products</h3>
            <input [(ngModel)]="search" (ngModelChange)="searchProducts()" placeholder="Search products..."
                   class="form-input mb-4">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-3 max-h-96 overflow-y-auto">
              @for (product of products(); track product.id) {
                <div class="border rounded-lg p-3 hover:border-primary cursor-pointer"
                     (click)="addToCart(product)">
                  <p class="font-medium text-sm">{{ product.name }}</p>
                  <p class="text-xs text-gray-500">SKU: {{ product.sku }}</p>
                  <p class="text-primary font-bold">\${{ product.price }}</p>
                  <p class="text-xs text-gray-500">Available: {{ product.availableQuantity }}</p>
                </div>
              }
            </div>
          </div>
        </div>

        <div>
          <div class="card sticky top-4">
            <h3 class="text-lg font-semibold mb-4">Cart</h3>
            @if (cart().length === 0) {
              <p class="text-gray-500 text-sm">No items in cart</p>
            } @else {
              <div class="space-y-3 mb-4">
                @for (item of cart(); track item.productId) {
                  <div class="flex items-center justify-between text-sm">
                    <div class="flex-1">
                      <p class="font-medium">{{ item.productName }}</p>
                      <div class="flex items-center gap-2 mt-1">
                        <button (click)="updateQty(item, -1)" class="w-6 h-6 rounded bg-gray-100 text-center">-</button>
                        <span class="font-mono">{{ item.quantity }}</span>
                        <button (click)="updateQty(item, 1)" class="w-6 h-6 rounded bg-gray-100 text-center">+</button>
                      </div>
                    </div>
                    <div class="text-right">
                      <p class="font-mono">\${{ (item.unitPrice * item.quantity).toFixed(2) }}</p>
                      <button (click)="removeFromCart(item)" class="text-red-500 text-xs">Remove</button>
                    </div>
                  </div>
                }
              </div>
              <div class="border-t pt-3 mb-4">
                <div class="flex justify-between font-bold"><span>Total:</span><span class="font-mono">\${{ cartTotal() }}</span></div>
              </div>
            }

            <div class="mb-4">
              <label class="block text-sm font-medium mb-1">Shipping Address</label>
              <textarea [(ngModel)]="shippingAddress" rows="3" class="form-input" placeholder="Enter shipping address"></textarea>
            </div>

            @if (error()) {
              <div class="mb-3 p-2 bg-red-50 text-red-600 text-sm rounded">{{ error() }}</div>
            }

            <button (click)="placeOrder()" [disabled]="cart().length === 0 || !shippingAddress || loading()"
                    class="w-full btn-primary">
              {{ loading() ? 'Placing Order...' : 'Place Order' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  `
})
export class CreateOrderComponent implements OnInit {
  private productService = inject(ProductService);
  private orderService = inject(OrderService);
  private router = inject(Router);

  products = signal<Product[]>([]);
  cart = signal<CartItem[]>([]);
  search = '';
  shippingAddress = '';
  loading = signal(false);
  error = signal('');

  ngOnInit() { this.searchProducts(); }

  searchProducts() {
    this.productService.getProducts(0, 20, this.search || undefined).subscribe(res => {
      if (res.data) this.products.set(res.data.content);
    });
  }

  addToCart(product: Product) {
    const existing = this.cart().find(i => i.productId === product.id);
    if (existing) {
      this.updateQty(existing, 1);
    } else {
      this.cart.update(c => [...c, { productId: product.id, productName: product.name, quantity: 1, unitPrice: product.price }]);
    }
  }

  updateQty(item: CartItem, delta: number) {
    const newQty = item.quantity + delta;
    if (newQty <= 0) { this.removeFromCart(item); return; }
    this.cart.update(c => c.map(i => i.productId === item.productId ? { ...i, quantity: newQty } : i));
  }

  removeFromCart(item: CartItem) {
    this.cart.update(c => c.filter(i => i.productId !== item.productId));
  }

  cartTotal(): string {
    return this.cart().reduce((sum, i) => sum + i.unitPrice * i.quantity, 0).toFixed(2);
  }

  placeOrder() {
    this.loading.set(true);
    this.error.set('');
    this.orderService.createOrder({ items: this.cart(), shippingAddress: this.shippingAddress }).subscribe({
      next: res => this.router.navigate(['/dashboard/orders', res.data.id]),
      error: err => { this.error.set(err.error?.message || 'Failed to place order'); this.loading.set(false); }
    });
  }
}
