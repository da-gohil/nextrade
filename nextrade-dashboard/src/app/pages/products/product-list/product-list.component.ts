import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ProductService } from '../../../core/services/product.service';
import { Product } from '../../../core/models/product.model';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  template: `
    <div>
      <div class="flex items-center justify-between mb-6">
        <h2 class="text-2xl font-bold">Products</h2>
        <a routerLink="/dashboard/products/manage" class="btn-primary">+ Add Product</a>
      </div>
      <div class="card mb-4">
        <input [(ngModel)]="search" (ngModelChange)="load()" placeholder="Search products..." class="form-input w-64">
      </div>
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
        @for (product of products(); track product.id) {
          <div class="card hover:shadow-md transition-shadow">
            <div class="h-40 bg-gray-100 rounded-md mb-3 flex items-center justify-center">
              @if (product.imageUrl) {
                <img [src]="product.imageUrl" [alt]="product.name" class="h-full w-full object-cover rounded-md">
              } @else {
                <svg class="h-12 w-12 text-gray-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"/>
                </svg>
              }
            </div>
            <p class="font-semibold text-sm">{{ product.name }}</p>
            <p class="text-xs text-gray-500 mb-2">{{ product.categoryName }}</p>
            <div class="flex items-center justify-between">
              <span class="text-primary font-bold">\${{ product.price }}</span>
              <span class="text-xs text-gray-500">{{ product.availableQuantity }} in stock</span>
            </div>
            <a [routerLink]="['/dashboard/products', product.id]" class="mt-3 w-full btn-secondary text-center block text-sm">View Details</a>
          </div>
        }
      </div>
    </div>
  `
})
export class ProductListComponent implements OnInit {
  private productService = inject(ProductService);
  products = signal<Product[]>([]);
  search = '';
  ngOnInit() { this.load(); }
  load() {
    this.productService.getProducts(0, 20, this.search || undefined).subscribe(res => {
      if (res.data) this.products.set(res.data.content);
    });
  }
}
