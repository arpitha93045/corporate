import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BulkOrderRequest, Category, CheckoutRequest, DraftCart, EnquiryRequest, Order, OrderSummary, PaymentIntentResponse, Product, Quote } from '../models/models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);
  private base = '/api';

  categories(): Observable<Category[]> {
    return this.http.get<Category[]>(`${this.base}/categories`);
  }

  products(categorySlug?: string, query?: string): Observable<Product[]> {
    let params = new HttpParams();
    if (categorySlug) params = params.set('category', categorySlug);
    if (query && query.trim()) params = params.set('q', query.trim());
    return this.http.get<Product[]>(`${this.base}/products`, { params });
  }

  product(slug: string): Observable<Product> {
    return this.http.get<Product>(`${this.base}/products/${slug}`);
  }

  checkout(req: CheckoutRequest, idempotencyKey?: string): Observable<Order> {
    const headers = idempotencyKey
      ? new HttpHeaders({ 'Idempotency-Key': idempotencyKey })
      : undefined;
    return this.http.post<Order>(`${this.base}/checkout`, req, { headers });
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

  createPaymentIntent(orderNumber: string): Observable<PaymentIntentResponse> {
    return this.http.post<PaymentIntentResponse>(`${this.base}/payments/intent/${orderNumber}`, {});
  }

  bulkOrderEstimate(req: BulkOrderRequest): Observable<DraftCart> {
    return this.http.post<DraftCart>(`${this.base}/bulk-order/estimate`, req);
  }

  quote(token: string): Observable<Quote> {
    return this.http.get<Quote>(`${this.base}/quotes/${token}`);
  }

  acceptQuote(token: string): Observable<Quote> {
    return this.http.post<Quote>(`${this.base}/quotes/${token}/accept`, {});
  }

  declineQuote(token: string): Observable<Quote> {
    return this.http.post<Quote>(`${this.base}/quotes/${token}/decline`, {});
  }

  // Newsletter
  subscribeNewsletter(email: string): Observable<{ success: boolean; message: string }> {
    return this.http.post<{ success: boolean; message: string }>(`${this.base}/newsletter/subscribe`, { email });
  }
}
