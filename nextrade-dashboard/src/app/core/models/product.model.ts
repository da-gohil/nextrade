export interface Product {
  id: number;
  sku: string;
  name: string;
  description: string;
  categoryId: number;
  categoryName: string;
  vendorId: number;
  price: number;
  stockQuantity: number;
  reservedQuantity: number;
  availableQuantity: number;
  lowStockThreshold: number;
  imageUrl: string;
  isActive: boolean;
  createdAt: string;
}

export interface CreateProductRequest {
  sku: string;
  name: string;
  description?: string;
  categoryId: number;
  price: number;
  stockQuantity: number;
  lowStockThreshold?: number;
  imageUrl?: string;
}
