import { CommonModule, DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminApiService } from '../../core/admin-api.service';
import {
  AdminEnquiry,
  AdminOrderSummary,
  AdminProduct,
  Category,
  CreateQuoteRequest,
  ProductUpsert,
  Quote,
} from '../../models/models';
import { MoneyPipe } from '../../shared/money.pipe';

type Tab = 'products' | 'orders' | 'enquiries';

// Enquiry lifecycle states offered in the admin dropdown. Mirrors the backend
// EnquiryStatus enum; buyer-driven states (ACCEPTED/DECLINED) and EXPIRED are
// shown so an admin can see/correct them, but the normal path is via the quote.
const ENQUIRY_STATUSES = ['NEW', 'REVIEWING', 'QUOTED', 'ACCEPTED', 'DECLINED', 'EXPIRED', 'CLOSED'];

interface QuoteDraftLine {
  productId: number | null;
  quantity: number;
}

const EMPTY_DRAFT: ProductUpsert = {
  name: '',
  slug: '',
  description: '',
  priceCents: 0,
  imageUrl: '',
  stockQuantity: 0,
  categorySlug: '',
};

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe, MoneyPipe],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.css',
})
export class AdminComponent {
  private api = inject(AdminApiService);

  protected tab = signal<Tab>('products');
  protected error = signal<string | null>(null);

  protected products = signal<AdminProduct[]>([]);
  protected categories = signal<Category[]>([]);
  protected orders = signal<AdminOrderSummary[]>([]);
  protected enquiries = signal<AdminEnquiry[]>([]);

  protected editing = signal<AdminProduct | null>(null);
  protected draft = signal<ProductUpsert>({ ...EMPTY_DRAFT });
  protected saving = signal<boolean>(false);

  protected priceRupees(): number {
    return this.draft().priceCents / 100;
  }

  protected setPriceRupees(v: number) {
    this.draft.update(d => ({ ...d, priceCents: Math.round(v * 100) }));
  }

  constructor() {
    this.loadProducts();
    this.loadCategories();
  }

  switchTab(t: Tab) {
    this.tab.set(t);
    this.error.set(null);
    if (t === 'orders' && this.orders().length === 0) this.loadOrders();
    if (t === 'enquiries' && this.enquiries().length === 0) this.loadEnquiries();
  }

  // --- Products ---

  loadProducts() {
    this.api.listProducts().subscribe({
      next: p => this.products.set(p),
      error: () => this.error.set('Failed to load products.'),
    });
  }

  loadCategories() {
    this.api.listCategories().subscribe({
      next: c => this.categories.set(c),
      error: () => {},
    });
  }

  startNewProduct() {
    this.editing.set(null);
    this.draft.set({ ...EMPTY_DRAFT, categorySlug: this.categories()[0]?.slug ?? '' });
  }

  startEditProduct(p: AdminProduct) {
    this.editing.set(p);
    this.draft.set({
      name: p.name,
      slug: p.slug,
      description: p.description,
      priceCents: p.priceCents,
      imageUrl: p.imageUrl,
      stockQuantity: p.stockQuantity,
      categorySlug: p.categorySlug,
    });
  }

  cancelEdit() {
    this.editing.set(null);
    this.draft.set({ ...EMPTY_DRAFT });
  }

  saveProduct() {
    if (this.saving()) return;
    this.saving.set(true);
    this.error.set(null);
    const payload = { ...this.draft(), imageUrl: this.draft().imageUrl || null };
    const editingProduct = this.editing();
    const req = editingProduct
      ? this.api.updateProduct(editingProduct.id, payload)
      : this.api.createProduct(payload);
    req.subscribe({
      next: () => {
        this.saving.set(false);
        this.cancelEdit();
        this.loadProducts();
      },
      error: err => {
        this.saving.set(false);
        this.error.set(err?.error?.message ?? 'Save failed.');
      },
    });
  }

  deleteProduct(p: AdminProduct) {
    if (!confirm(`Delete ${p.name}? Existing orders keep their snapshot.`)) return;
    this.api.deleteProduct(p.id).subscribe({
      next: () => this.loadProducts(),
      error: err => this.error.set(err?.error?.message ?? 'Delete failed.'),
    });
  }

  // --- Orders ---

  loadOrders() {
    this.api.listOrders().subscribe({
      next: o => this.orders.set(o),
      error: () => this.error.set('Failed to load orders.'),
    });
  }

  changeOrderStatus(o: AdminOrderSummary, status: string) {
    this.api.updateOrderStatus(o.orderNumber, status).subscribe({
      next: () => this.loadOrders(),
      error: err => this.error.set(err?.error?.message ?? 'Status update failed.'),
    });
  }

  // Which transitions the admin can offer, mirroring backend AdminOrderService.
  availableTransitions(status: string): string[] {
    switch (status) {
      case 'PLACED':   return ['CANCELLED'];
      case 'PAID':     return ['FULFILLED', 'CANCELLED'];
      default:         return [];
    }
  }

  // --- Enquiries ---

  loadEnquiries() {
    this.api.listEnquiries().subscribe({
      next: e => this.enquiries.set(e),
      error: () => this.error.set('Failed to load enquiries.'),
    });
  }

  changeEnquiryStatus(e: AdminEnquiry, status: string) {
    this.api.updateEnquiryStatus(e.id, status).subscribe({
      next: updated => {
        this.enquiries.update(list => list.map(x => x.id === updated.id ? updated : x));
      },
      error: err => this.error.set(err?.error?.message ?? 'Status update failed.'),
    });
  }

  // --- Quotes (RFQ) ---

  protected readonly enquiryStatuses = ENQUIRY_STATUSES;
  protected quoteFor = signal<number | null>(null);
  protected quoteLines = signal<QuoteDraftLine[]>([]);
  protected quoteNotes = signal<string>('');
  protected quoteValidUntil = signal<string>('');
  protected quoteResult = signal<Quote | null>(null);
  protected quoteSaving = signal<boolean>(false);

  openQuoteBuilder(e: AdminEnquiry) {
    if (this.quoteFor() === e.id) {
      this.quoteFor.set(null);
      return;
    }
    this.quoteFor.set(e.id);
    this.quoteLines.set([{ productId: this.products()[0]?.id ?? null, quantity: 1 }]);
    this.quoteNotes.set('');
    this.quoteValidUntil.set('');
    this.quoteResult.set(null);
    this.error.set(null);
    if (this.products().length === 0) this.loadProducts();
    // Show an existing quote if one was already issued.
    this.api.getQuote(e.id).subscribe({
      next: q => this.quoteResult.set(q),
      error: () => {},
    });
  }

  addQuoteLine() {
    this.quoteLines.update(lines => [...lines, { productId: this.products()[0]?.id ?? null, quantity: 1 }]);
  }

  removeQuoteLine(index: number) {
    this.quoteLines.update(lines => lines.filter((_, i) => i !== index));
  }

  setQuoteLineProduct(index: number, productId: number) {
    this.quoteLines.update(lines => lines.map((l, i) => i === index ? { ...l, productId } : l));
  }

  setQuoteLineQuantity(index: number, quantity: number) {
    this.quoteLines.update(lines => lines.map((l, i) => i === index ? { ...l, quantity } : l));
  }

  submitQuote(enquiryId: number) {
    if (this.quoteSaving()) return;
    const lines = this.quoteLines()
      .filter(l => l.productId != null && l.quantity > 0)
      .map(l => ({ productId: l.productId as number, quantity: l.quantity }));
    if (lines.length === 0) {
      this.error.set('Add at least one line with a product and quantity.');
      return;
    }
    const req: CreateQuoteRequest = {
      lines,
      notes: this.quoteNotes().trim() || undefined,
      validUntil: this.quoteValidUntil() || undefined,
    };
    this.quoteSaving.set(true);
    this.error.set(null);
    this.api.createQuote(enquiryId, req).subscribe({
      next: q => {
        this.quoteSaving.set(false);
        this.quoteResult.set(q);
        // Issuing a quote moved the enquiry to QUOTED — reflect it.
        this.enquiries.update(list => list.map(x => x.id === enquiryId ? { ...x, status: 'QUOTED' } : x));
      },
      error: err => {
        this.quoteSaving.set(false);
        this.error.set(err?.error?.message ?? 'Failed to create quote.');
      },
    });
  }
}
