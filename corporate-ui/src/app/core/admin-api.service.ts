import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AdminEnquiry,
  AdminOrderSummary,
  AdminProduct,
  Category,
  CategoryUpsert,
  Order,
  ProductUpsert,
} from '../models/models';

@Injectable({ providedIn: 'root' })
export class AdminApiService {
  private http = inject(HttpClient);
  private base = '/api/admin';

  listProducts(): Observable<AdminProduct[]> {
    return this.http.get<AdminProduct[]>(`${this.base}/products`);
  }

  createProduct(req: ProductUpsert): Observable<AdminProduct> {
    return this.http.post<AdminProduct>(`${this.base}/products`, req);
  }

  updateProduct(id: number, req: ProductUpsert): Observable<AdminProduct> {
    return this.http.put<AdminProduct>(`${this.base}/products/${id}`, req);
  }

  deleteProduct(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/products/${id}`);
  }

  listCategories(): Observable<Category[]> {
    return this.http.get<Category[]>(`${this.base}/categories`);
  }

  createCategory(req: CategoryUpsert): Observable<Category> {
    return this.http.post<Category>(`${this.base}/categories`, req);
  }

  listOrders(): Observable<AdminOrderSummary[]> {
    return this.http.get<AdminOrderSummary[]>(`${this.base}/orders`);
  }

  getOrder(orderNumber: string): Observable<Order> {
    return this.http.get<Order>(`${this.base}/orders/${orderNumber}`);
  }

  updateOrderStatus(orderNumber: string, status: string): Observable<Order> {
    return this.http.patch<Order>(`${this.base}/orders/${orderNumber}/status`, { status });
  }

  listEnquiries(): Observable<AdminEnquiry[]> {
    return this.http.get<AdminEnquiry[]>(`${this.base}/enquiries`);
  }

  updateEnquiryStatus(id: number, status: string): Observable<AdminEnquiry> {
    return this.http.patch<AdminEnquiry>(`${this.base}/enquiries/${id}/status`, { status });
  }
}
