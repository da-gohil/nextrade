import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ProductService } from '../../../core/services/product.service';
import { Product } from '../../../core/models/product.model';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div>
      <a routerLink="/dashboard/products" class="text-gray-500 hover:text-gray-700 mb-4 block">← Back to Products</a>
      @if (product()) {
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-8">
          <div class="h-80 bg-gray-100 rounded-lg flex items-center justify-center">
            <svg class="h-24 w-24 text-gray-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"/>
            </svg>
          </div>
          <div>
            <p class="text-sm text-gray-500 mb-1">{{ product()!.categoryName }}</p>
            <h2 class="text-3xl font-bold mb-2">{{ product()!.name }}</h2>
            <p class="text-sm text-gray-500 font-mono mb-4">SKU: {{ product()!.sku }}</p>
            <p class="text-gray-600 mb-6">{{ product()!.description }}</p>
            <div class="text-4xl font-bold text-primary mb-6">\${{ product()!.price }}</div>
            <div class="grid grid-cols-2 gap-4 mb-6">
              <div class="card"><p class="text-sm text-gray-500">Available</p><p class="text-2xl font-bold">{{ product()!.availableQuantity }}</p></div>
              <div class="card"><p class="text-sm text-gray-500">In Stock</p><p class="text-2xl font-bold">{{ product()!.stockQuantity }}</p></div>
            </div>
            <a routerLink="/dashboard/orders/new" class="btn-primary w-full text-center block py-3">Order Now</a>
          </div>
        </div>
      }
    </div>
  `
})
export class ProductDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private productService = inject(ProductService);
  product = signal<Product | null>(null);
  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.productService.getProduct(id).subscribe(res => this.product.set(res.data));
  }
}
