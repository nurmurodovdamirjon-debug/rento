import { Request } from 'express';

// ===== User =====
export interface User {
  id: string;
  phone: string;
  phone_verified: boolean;
  full_name: string | null;
  email: string | null;
  avatar_url: string | null;
  role: 'tenant' | 'landlord' | 'both' | 'admin';
  id_verified: boolean;
  id_document_url: string | null;
  id_verified_at: Date | null;
  rating_avg: number;
  rating_count: number;
  subscription: 'free' | 'pro';
  sub_expires_at: Date | null;
  language: 'uz' | 'ru' | 'en';
  last_seen_at: Date | null;
  created_at: Date;
  updated_at: Date;
  is_active: boolean;
  is_blocked: boolean;
}

// ===== JWT Payloads =====
export interface AccessTokenPayload {
  sub: string;       // user id
  role: string;
  phone: string;
  iss: string;       // "rento.uz"
}

export interface RefreshTokenPayload extends AccessTokenPayload {
  jti: string;       // unique token id
  type: 'refresh';
}

// ===== Auth Request =====
export interface AuthRequest extends Request {
  user?: {
    id: string;
    role: string;
    phone: string;
  };
}

// ===== API Response =====
export interface ApiResponse<T = unknown> {
  success: boolean;
  data?: T;
  error?: {
    code: string;
    message: string;
    details?: unknown;
  };
  meta?: {
    page?: number;
    per_page?: number;
    total?: number;
    total_pages?: number;
    timestamp: string;
  };
}

// ===== Auth Responses =====
export interface SendOtpResponse {
  phone: string;
  expires_in: number;
  retry_after: number;
  attempts_remaining: number;
}

export interface VerifyOtpResponse {
  access_token: string;
  refresh_token: string;
  token_type: 'Bearer';
  expires_in: number;
  user: {
    id: string;
    phone: string;
    full_name: string | null;
    role: string;
    is_new_user: boolean;
  };
}

export interface RefreshTokenResponse {
  access_token: string;
  refresh_token: string;
  token_type: 'Bearer';
  expires_in: number;
}
