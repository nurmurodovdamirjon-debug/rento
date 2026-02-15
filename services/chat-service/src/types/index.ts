import { Request } from 'express';
import { Socket } from 'socket.io';

/** JWT payload tuzilishi */
export interface JwtPayload {
  sub: string;   // user_id (UUID)
  role: string;
  phone: string;
  iat: number;
  exp: number;
}

/** Auth qilingan HTTP so'rov */
export interface AuthRequest extends Request {
  user?: {
    id: string;
    role: string;
    phone: string;
  };
}

/** Auth qilingan Socket */
export interface AuthSocket extends Socket {
  data: {
    userId: string;
    role: string;
    phone: string;
  };
}

/** Chat xonasi modeli */
export interface ChatRoom {
  id: string;
  listing_id: string;
  tenant_id: string;
  landlord_id: string;
  last_message_at: string | null;
  is_active: boolean;
  created_at: string;
}

/** Xabar modeli */
export interface Message {
  id: string;
  room_id: string;
  sender_id: string;
  content: string | null;
  message_type: 'text' | 'image' | 'location' | 'contact';
  media_url: string | null;
  metadata: Record<string, unknown> | null;
  is_read: boolean;
  read_at: string | null;
  created_at: string;
}

/** Chat ro'yxatidagi xona (qo'shimcha ma'lumotlar bilan) */
export interface ChatRoomListItem {
  room_id: string;
  listing: {
    id: string;
    title: string;
    image_url: string | null;
  };
  other_user: {
    id: string;
    full_name: string | null;
    avatar_url: string | null;
    is_online: boolean;
    last_seen_at: string | null;
  };
  last_message: {
    content: string | null;
    sender_id: string;
    created_at: string;
  } | null;
  unread_count: number;
  created_at: string;
}
