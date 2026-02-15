import { z } from 'zod';

/** POST /chats — yangi chat boshlash */
export const createChatSchema = z.object({
  listing_id: z.string().uuid('Noto\'g\'ri listing_id formati'),
  initial_message: z
    .string()
    .min(1, 'Xabar bo\'sh bo\'lmasligi kerak')
    .max(2000, 'Xabar 2000 belgidan oshmasligi kerak'),
});

/** POST /chats/:room_id/messages — xabar yuborish (REST fallback) */
export const sendMessageSchema = z.object({
  content: z.string().max(2000).optional(),
  message_type: z.enum(['text', 'image', 'location', 'contact']).default('text'),
  media_url: z.string().url().optional(),
  metadata: z.record(z.string(), z.unknown()).optional(),
}).refine(
  (data) => {
    if (data.message_type === 'text') return !!data.content && data.content.trim().length > 0;
    if (data.message_type === 'image') return !!data.media_url;
    if (data.message_type === 'location') return !!data.metadata;
    return true;
  },
  { message: 'Xabar turi uchun kerakli maydonlar to\'ldirilmagan' }
);
