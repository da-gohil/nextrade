export interface OrderItem {
  productId: number;
  productName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export interface StatusHistory {
  fromStatus: string;
  toStatus: string;
  note: string;
  createdAt: string;
}

export interface Order {
  id: number;
  orderNumber: string;
  userId: number;
  status: string;
  totalAmount: number;
  shippingAddress: string;
  notes: string;
  items: OrderItem[];
  statusHistory: StatusHistory[];
  createdAt: string;
}

export interface CreateOrderRequest {
  items: {
    productId: number;
    productName: string;
    quantity: number;
    unitPrice: number;
  }[];
  shippingAddress: string;
  notes?: string;
}
