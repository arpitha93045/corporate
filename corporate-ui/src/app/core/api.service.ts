import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Category, CheckoutRequest, EnquiryRequest, Order, OrderSummary, Product } from '../models/models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);
  private base = '/api';

  categories(): Observable<Category[]> {
    return this.http.get<Category[]>(`${this.base}/categories`);
  }

  products(categorySlug?: string): Observable<Product[]> {
    let params = new HttpParams();
    if (categorySlug) params = params.set('category', categorySlug);
    return this.http.get<Product[]>(`${this.base}/products`, { params });
  }

  product(slug: string): Observable<Product> {
    return this.http.get<Product>(`${this.base}/products/${slug}`);
  }

  checkout(req: CheckoutRequest): Observable<Order> {
    return this.http.post<Order>(`${this.base}/checkout`, req);
  }

  order(orderNumber: string): Observable<Order> {
    return this.http.get<Order>(`${this.base}/orders/${orderNumber}`);
  }

  myOrders(): Observable<OrderSummary[]> {
    return this.http.get<OrderSummary[]>(`${this.base}/orders`);
  }

  submitEnquiry(req: EnquiryRequest): Observable<unknown> {
    return this.http.post(`${this.base}/enquiries`, req);
  }
}
