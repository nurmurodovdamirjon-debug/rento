import { createChatSchema, sendMessageSchema } from '../src/validators/chat.validator';

describe('Chat Validators', () => {
  describe('createChatSchema', () => {
    it("to'g'ri ma'lumot — muvaffaqiyat", () => {
      const result = createChatSchema.safeParse({
        listing_id: '550e8400-e29b-41d4-a716-446655440000',
        initial_message: 'Salom! Bu kvartira hali bandmi?',
      });
      expect(result.success).toBe(true);
    });

    it("bo'sh listing_id — xato", () => {
      const result = createChatSchema.safeParse({
        listing_id: '',
        initial_message: 'Salom',
      });
      expect(result.success).toBe(false);
    });

    it("noto'g'ri UUID — xato", () => {
      const result = createChatSchema.safeParse({
        listing_id: 'not-a-uuid',
        initial_message: 'Test',
      });
      expect(result.success).toBe(false);
    });

    it("bo'sh xabar — xato", () => {
      const result = createChatSchema.safeParse({
        listing_id: '550e8400-e29b-41d4-a716-446655440000',
        initial_message: '',
      });
      expect(result.success).toBe(false);
    });

    it('juda uzun xabar (2001 belgi) — xato', () => {
      const result = createChatSchema.safeParse({
        listing_id: '550e8400-e29b-41d4-a716-446655440000',
        initial_message: 'a'.repeat(2001),
      });
      expect(result.success).toBe(false);
    });
  });

  describe('sendMessageSchema', () => {
    it('text xabar — muvaffaqiyat', () => {
      const result = sendMessageSchema.safeParse({
        content: 'Test xabar',
        message_type: 'text',
      });
      expect(result.success).toBe(true);
    });

    it("text xabar content yo'q — xato", () => {
      const result = sendMessageSchema.safeParse({
        message_type: 'text',
      });
      expect(result.success).toBe(false);
    });

    it("image xabar media_url yo'q — xato", () => {
      const result = sendMessageSchema.safeParse({
        message_type: 'image',
      });
      expect(result.success).toBe(false);
    });

    it("image xabar media_url bilan — muvaffaqiyat", () => {
      const result = sendMessageSchema.safeParse({
        message_type: 'image',
        media_url: 'https://media.rento.uz/chat/img.webp',
      });
      expect(result.success).toBe(true);
    });

    it('location xabar — muvaffaqiyat', () => {
      const result = sendMessageSchema.safeParse({
        content: 'Mening lokatsiyam',
        message_type: 'location',
        metadata: { latitude: 41.2995, longitude: 69.2401 },
      });
      expect(result.success).toBe(true);
    });

    it("noto'g'ri message_type — xato", () => {
      const result = sendMessageSchema.safeParse({
        content: 'Test',
        message_type: 'video',
      });
      expect(result.success).toBe(false);
    });
  });
});
