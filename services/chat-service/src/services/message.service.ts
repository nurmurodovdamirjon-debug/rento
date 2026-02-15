import { pool } from '../config/database';
import { Message } from '../types';
import { logger } from '../utils/logger';

interface SendMessageInput {
  content?: string;
  message_type: 'text' | 'image' | 'location' | 'contact';
  media_url?: string;
  metadata?: Record<string, unknown>;
}

class MessageService {
  /**
   * Yangi xabar yuborish va chat_rooms.last_message_at yangilash.
   */
  async sendMessage(
    roomId: string,
    senderId: string,
    input: SendMessageInput
  ): Promise<Message> {
    const client = await pool.connect();
    try {
      await client.query('BEGIN');

      const result = await client.query(
        `INSERT INTO messages (room_id, sender_id, content, message_type, media_url, metadata)
         VALUES ($1, $2, $3, $4, $5, $6)
         RETURNING *`,
        [
          roomId,
          senderId,
          input.content || null,
          input.message_type,
          input.media_url || null,
          input.metadata ? JSON.stringify(input.metadata) : null,
        ]
      );

      // Chat xonasining oxirgi xabar vaqtini yangilash
      await client.query(
        'UPDATE chat_rooms SET last_message_at = NOW() WHERE id = $1',
        [roomId]
      );

      await client.query('COMMIT');

      const message = result.rows[0] as Message;
      logger.debug(`Message sent: ${message.id} in room ${roomId}`);
      return message;
    } catch (error) {
      await client.query('ROLLBACK');
      throw error;
    } finally {
      client.release();
    }
  }

  /**
   * Xabarlar tarixini olish (sahifalash bilan).
   */
  async getMessages(
    roomId: string,
    page: number,
    perPage: number
  ): Promise<{ messages: Message[]; total: number }> {
    const offset = (page - 1) * perPage;

    const countResult = await pool.query(
      'SELECT COUNT(*) FROM messages WHERE room_id = $1',
      [roomId]
    );
    const total = parseInt(countResult.rows[0].count, 10);

    const result = await pool.query(
      `SELECT * FROM messages
       WHERE room_id = $1
       ORDER BY created_at DESC
       LIMIT $2 OFFSET $3`,
      [roomId, perPage, offset]
    );

    return { messages: result.rows as Message[], total };
  }

  /**
   * Xonaning barcha o'qilmagan xabarlarini "o'qildi" deb belgilash.
   * Faqat boshqa foydalanuvchi yuborgan xabarlar belgilanadi.
   */
  async markAsRead(roomId: string, userId: string): Promise<number> {
    const result = await pool.query(
      `UPDATE messages
       SET is_read = TRUE, read_at = NOW()
       WHERE room_id = $1 AND sender_id <> $2 AND is_read = FALSE
       RETURNING id`,
      [roomId, userId]
    );
    return result.rowCount ?? 0;
  }

  /**
   * Bitta xabardan boshlab o'qildi belgilash (WS mark_read uchun).
   */
  async markAsReadUpTo(roomId: string, userId: string, messageId: string): Promise<number> {
    const result = await pool.query(
      `UPDATE messages
       SET is_read = TRUE, read_at = NOW()
       WHERE room_id = $1
         AND sender_id <> $2
         AND is_read = FALSE
         AND created_at <= (SELECT created_at FROM messages WHERE id = $3)
       RETURNING id`,
      [roomId, userId, messageId]
    );
    return result.rowCount ?? 0;
  }
}

export const messageService = new MessageService();
