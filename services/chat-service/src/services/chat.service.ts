import { pool } from '../config/database';
import { redis } from '../config/redis';
import { ChatRoom, ChatRoomListItem } from '../types';
import { ChatRoomNotFoundError, ChatSelfMessageError, ChatUnauthorizedError } from '../utils/errors';
import { logger } from '../utils/logger';
import { messageService } from './message.service';

class ChatService {
  /**
   * Yangi chat boshlash yoki mavjud chat xonasini qaytarish.
   * E'lon egasi (landlord) va so'rovchi (tenant) o'rtasida yagona xona yaratiladi.
   */
  async createOrGetRoom(
    userId: string,
    listingId: string,
    initialMessage: string
  ): Promise<{ room: ChatRoom; isNew: boolean }> {
    const client = await pool.connect();
    try {
      await client.query('BEGIN');

      // E'lon egasini aniqlash
      const listingResult = await client.query(
        'SELECT user_id FROM listings WHERE id = $1 AND status = \'active\'',
        [listingId]
      );
      if (listingResult.rows.length === 0) {
        throw new ChatRoomNotFoundError("E'lon topilmadi yoki faol emas");
      }

      const landlordId = listingResult.rows[0].user_id;

      // O'ziga o'zi yozish tekshiruvi
      if (userId === landlordId) {
        throw new ChatSelfMessageError();
      }

      // Mavjud xonani tekshirish
      const existingRoom = await client.query(
        `SELECT * FROM chat_rooms
         WHERE listing_id = $1 AND tenant_id = $2 AND landlord_id = $3`,
        [listingId, userId, landlordId]
      );

      if (existingRoom.rows.length > 0) {
        await client.query('COMMIT');

        // Boshlang'ich xabar yuborish (agar yangi chat ochayotgan bo'lsa)
        if (initialMessage) {
          await messageService.sendMessage(existingRoom.rows[0].id, userId, {
            content: initialMessage,
            message_type: 'text',
          });
        }

        return { room: existingRoom.rows[0], isNew: false };
      }

      // Yangi xona yaratish
      const newRoom = await client.query(
        `INSERT INTO chat_rooms (listing_id, tenant_id, landlord_id)
         VALUES ($1, $2, $3)
         RETURNING *`,
        [listingId, userId, landlordId]
      );

      await client.query('COMMIT');

      const room = newRoom.rows[0] as ChatRoom;

      // Boshlang'ich xabar yuborish
      if (initialMessage) {
        await messageService.sendMessage(room.id, userId, {
          content: initialMessage,
          message_type: 'text',
        });
      }

      logger.info(`Chat room created: ${room.id} (listing: ${listingId})`);
      return { room, isNew: true };
    } catch (error) {
      await client.query('ROLLBACK');
      throw error;
    } finally {
      client.release();
    }
  }

  /**
   * Foydalanuvchining barcha chat xonalarini olish.
   * Oxirgi xabar, o'qilmagan soni, e'lon va boshqa foydalanuvchi ma'lumotlari bilan.
   */
  async getRooms(
    userId: string,
    page: number,
    perPage: number
  ): Promise<{ rooms: ChatRoomListItem[]; total: number }> {
    const offset = (page - 1) * perPage;

    // Umumiy soni
    const countResult = await pool.query(
      `SELECT COUNT(*) FROM chat_rooms
       WHERE (tenant_id = $1 OR landlord_id = $1) AND is_active = TRUE`,
      [userId]
    );
    const total = parseInt(countResult.rows[0].count, 10);

    // Xonalar ro'yxati
    const result = await pool.query(
      `SELECT
        cr.id AS room_id,
        cr.created_at,
        -- E'lon ma'lumotlari
        l.id AS listing_id,
        l.title AS listing_title,
        (SELECT li.url FROM listing_images li WHERE li.listing_id = l.id ORDER BY li.sort_order LIMIT 1) AS listing_image_url,
        -- Boshqa foydalanuvchi
        CASE WHEN cr.tenant_id = $1 THEN cr.landlord_id ELSE cr.tenant_id END AS other_user_id,
        ou.full_name AS other_user_name,
        ou.avatar_url AS other_user_avatar,
        ou.last_seen_at AS other_user_last_seen,
        -- Oxirgi xabar
        lm.content AS last_message_content,
        lm.sender_id AS last_message_sender,
        lm.created_at AS last_message_at,
        -- O'qilmagan xabarlar soni
        (SELECT COUNT(*) FROM messages m
         WHERE m.room_id = cr.id AND m.is_read = FALSE AND m.sender_id <> $1
        )::integer AS unread_count
       FROM chat_rooms cr
       JOIN listings l ON l.id = cr.listing_id
       JOIN users ou ON ou.id = CASE WHEN cr.tenant_id = $1 THEN cr.landlord_id ELSE cr.tenant_id END
       LEFT JOIN LATERAL (
         SELECT content, sender_id, created_at
         FROM messages
         WHERE room_id = cr.id
         ORDER BY created_at DESC
         LIMIT 1
       ) lm ON TRUE
       WHERE (cr.tenant_id = $1 OR cr.landlord_id = $1) AND cr.is_active = TRUE
       ORDER BY COALESCE(lm.created_at, cr.created_at) DESC
       LIMIT $2 OFFSET $3`,
      [userId, perPage, offset]
    );

    // Online statusni Redis dan olish
    const rooms: ChatRoomListItem[] = await Promise.all(
      result.rows.map(async (row) => {
        const isOnline = await this.isUserOnline(row.other_user_id);
        return {
          room_id: row.room_id,
          listing: {
            id: row.listing_id,
            title: row.listing_title,
            image_url: row.listing_image_url,
          },
          other_user: {
            id: row.other_user_id,
            full_name: row.other_user_name,
            avatar_url: row.other_user_avatar,
            is_online: isOnline,
            last_seen_at: row.other_user_last_seen,
          },
          last_message: row.last_message_at
            ? {
                content: row.last_message_content,
                sender_id: row.last_message_sender,
                created_at: row.last_message_at,
              }
            : null,
          unread_count: row.unread_count,
          created_at: row.created_at,
        };
      })
    );

    return { rooms, total };
  }

  /**
   * Chat xonasini topish va foydalanuvchining kirish huquqini tekshirish.
   */
  async getRoom(roomId: string, userId: string): Promise<ChatRoom> {
    const result = await pool.query('SELECT * FROM chat_rooms WHERE id = $1', [roomId]);

    if (result.rows.length === 0) {
      throw new ChatRoomNotFoundError();
    }

    const room = result.rows[0] as ChatRoom;

    if (room.tenant_id !== userId && room.landlord_id !== userId) {
      throw new ChatUnauthorizedError();
    }

    return room;
  }

  /**
   * Foydalanuvchini online deb belgilash (Redis SET, 5 daqiqa TTL).
   */
  async setUserOnline(userId: string): Promise<void> {
    await redis.set(`user:online:${userId}`, '1', 'EX', 300);
  }

  /**
   * Foydalanuvchini offline deb belgilash.
   */
  async setUserOffline(userId: string): Promise<void> {
    await redis.del(`user:online:${userId}`);
    // last_seen yangilash
    await pool.query(
      'UPDATE users SET last_seen_at = NOW() WHERE id = $1',
      [userId]
    );
  }

  /**
   * Foydalanuvchi online mi tekshirish.
   */
  async isUserOnline(userId: string): Promise<boolean> {
    const result = await redis.get(`user:online:${userId}`);
    return result === '1';
  }
}

export const chatService = new ChatService();
