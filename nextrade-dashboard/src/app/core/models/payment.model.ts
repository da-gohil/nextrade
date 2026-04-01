export interface Payment {
  id: number;
  paymentNumber: string;
  orderId: number;
  userId: number;
  amount: number;
  method: string;
  status: string;
  idempotencyKey: string;
  failureReason?: string;
  processedAt?: string;
  createdAt: string;
}
