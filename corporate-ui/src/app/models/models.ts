export interface Category {
  id: number;
  name: string;
  slug: string;
}

export interface Product {
  id: number;
  name: string;
  slug: string;
  description: string;
  priceCents: number;
  imageUrl: string | null;
  inStock: boolean;
  categoryName: string;
  categorySlug: string;
}

export interface Branding {
  message: string | null;
  logoUrl: string | null;
}

export interface CartLine {
  product: Product;
  quantity: number;
  branding?: Branding;
}

export interface OrderItem {
  productId: number;
  productName: string;
  unitPriceCents: number;
  quantity: number;
  lineTotalCents: number;
  brandingMessage: string | null;
  brandingLogoUrl: string | null;
}

export interface OrderAddress {
  line1: string;
  line2: string | null;
  city: string;
  state: string | null;
  postalCode: string;
  country: string;
}

export interface Order {
  orderNumber: string;
  status: string;
  paymentStatus: string | null;
  paidAt: string | null;
  paymentTerms: string;
  poNumber: string | null;
  invoiceNumber: string | null;
  dueDate: string | null;
  companyName: string;
  contactName: string;
  email: string;
  phone: string | null;
  shippingAddress: OrderAddress;
  items: OrderItem[];
  subtotalCents: number;
  createdAt: string;
}

export interface PaymentIntentResponse {
  paymentIntentId: string;
  clientSecret: string;
}

export interface CheckoutRequest {
  customer: {
    companyName: string;
    contactName: string;
    email: string;
    phone: string;
  };
  shippingAddress: OrderAddress;
  items: { productId: number; quantity: number; branding?: Branding | null }[];
  paymentTerms?: PaymentTerms;
  poNumber?: string;
}

export type PaymentTerms = 'IMMEDIATE' | 'NET_30';

export interface UserSummary {
  id: number;
  email: string;
  fullName: string;
  companyName: string | null;
  phone: string | null;
  role: string;
}

export interface AuthResponse {
  token: string;
  expiresInSeconds: number;
  user: UserSummary;
}

export interface RegisterRequest {
  email: string;
  password: string;
  fullName: string;
  companyName?: string;
  phone?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface EnquiryRequest {
  name: string;
  email: string;
  companyName?: string;
  phone?: string;
  message: string;
  estimatedQuantity?: number;
  occasion?: string;
  eventDate?: string;
  budgetRange?: string;
}

export interface OrderSummary {
  orderNumber: string;
  status: string;
  subtotalCents: number;
  itemCount: number;
  createdAt: string;
}

// ---- admin ----

export interface AdminProduct {
  id: number;
  name: string;
  slug: string;
  description: string;
  priceCents: number;
  imageUrl: string | null;
  inStock: boolean;
  stockQuantity: number;
  categoryName: string;
  categorySlug: string;
}

export interface ProductUpsert {
  name: string;
  slug: string;
  description: string;
  priceCents: number;
  imageUrl: string | null;
  stockQuantity: number;
  categorySlug: string;
}

export interface CategoryUpsert {
  name: string;
  slug: string;
}

export interface AdminOrderSummary {
  orderNumber: string;
  status: string;
  paymentStatus: string | null;
  paidAt: string | null;
  paymentTerms: string;
  invoiceNumber: string | null;
  subtotalCents: number;
  itemCount: number;
  companyName: string;
  contactName: string;
  email: string;
  createdAt: string;
}

export interface AdminEnquiry {
  id: number;
  name: string;
  email: string;
  companyName: string | null;
  phone: string | null;
  message: string;
  estimatedQuantity: number | null;
  occasion: string | null;
  eventDate: string | null;
  budgetRange: string | null;
  status: string;
  createdAt: string;
}

// ---- AI gifting agent ----

export interface AgentChatMessage {
  role: 'user' | 'assistant';
  content: string;
}

export interface DraftCartLine {
  productSlug: string;
  productName: string;
  quantity: number;
  unitPriceCents: number;
  lineTotalCents: number;
}

export interface DraftCart {
  token: string;
  lines: DraftCartLine[];
  totalCents: number;
  warnings: string[];
}

export interface GiftRecipient {
  name: string;
  city: string;
  notes: string;
}

export interface BulkOrderRequest {
  lines: { productSlug: string; quantity: number }[];
}

// ---- quotes (RFQ) ----

export interface QuoteLine {
  productName: string;
  unitPriceCents: number;
  quantity: number;
  lineTotalCents: number;
}

export interface Quote {
  token: string;
  status: string;
  totalCents: number;
  notes: string | null;
  validUntil: string | null;
  createdAt: string;
  companyName: string | null;
  contactName: string;
  email: string;
  occasion: string | null;
  lines: QuoteLine[];
}

export interface CreateQuoteRequest {
  lines: { productId: number; quantity: number }[];
  notes?: string;
  validUntil?: string;
}