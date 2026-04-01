import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, PageResponse } from '../models/api.model';
import { Order, CreateOrderRequest } from '../models/order.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class OrderService {
  constructor(private http: HttpClient) {}

  createOrder(request: CreateOrderRequest): Observable<ApiResponse<Order>> {
    return this.http.post<ApiResponse<Order>>(`${environment.apiUrl}/orders`, request);
  }

  getOrders(page = 0, size = 20, status?: string): Observable<ApiResponse<PageResponse<Order>>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    return this.http.get<ApiResponse<PageResponse<Order>>>(`${environment.apiUrl}/orders`, { params });
  }

  getOrder(id: number): Observable<ApiResponse<Order>> {
    return this.http.get<ApiResponse<Order>>(`${environment.apiUrl}/orders/${id}`);
  }

  updateStatus(id: number, status: string, note?: string): Observable<ApiResponse<Order>> {
    return this.http.put<ApiResponse<Order>>(`${environment.apiUrl}/orders/${id}/status`, { status, note });
  }

  cancelOrder(id: number, reason?: string): Observable<ApiResponse<Order>> {
    let params = new HttpParams();
    if (reason) params = params.set('reason', reason);
    return this.http.post<ApiResponse<Order>>(`${environment.apiUrl}/orders/${id}/cancel`, {}, { params });
  }
}
