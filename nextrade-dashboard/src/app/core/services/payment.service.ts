import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, PageResponse } from '../models/api.model';
import { Payment } from '../models/payment.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class PaymentService {
  constructor(private http: HttpClient) {}

  getPaymentForOrder(orderId: number): Observable<ApiResponse<Payment>> {
    return this.http.get<ApiResponse<Payment>>(`${environment.apiUrl}/payments/${orderId}`);
  }

  getTransactions(page = 0, size = 20): Observable<ApiResponse<PageResponse<Payment>>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ApiResponse<PageResponse<Payment>>>(`${environment.apiUrl}/payments/transactions`, { params });
  }

  refund(id: number, reason: string): Observable<ApiResponse<Payment>> {
    return this.http.post<ApiResponse<Payment>>(`${environment.apiUrl}/payments/${id}/refund`, { reason });
  }
}
