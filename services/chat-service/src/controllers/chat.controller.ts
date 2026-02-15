import { NextFunction, Response } from 'express';
import { chatService } from '../services/chat.service';
import { messageService } from '../services/message.service';
import { AuthRequest } from '../types';
import { ValidationError } from '../utils/errors';
import { createChatSchema, sendMessageSchema } from '../validators/chat.validator';

class ChatController {
  /**
   * POST /chats — Yangi chat boshlash
   */
  async createChat(req: AuthRequest, res: Response, next: NextFunction): Promise<void> {
    try {
      const parsed = createChatSchema.safeParse(req.body);
      if (!parsed.success) {
        throw new ValidationError('Validatsiya xatosi', parsed.error.flatten().fieldErrors);
      }

      const userId = req.user!.id;
      const { listing_id, initial_message } = parsed.data;

      const { room, isNew } = await chatService.createOrGetRoom(userId, listing_id, initial_message);

      // E'lon va boshqa foydalanuvchi ma'lumotlarini olish (to'liq javob uchun)
      const rooms = await chatService.getRooms(userId, 1, 100);
      const roomData = rooms.rooms.find((r) => r.room_id === room.id);

      res.status(isNew ? 201 : 200).json({
        success: true,
        data: {
          room_id: room.id,
          listing: roomData?.listing ?? { id: listing_id, title: '', image_url: null },
          other_user: roomData?.other_user ?? null,
          created_at: room.created_at,
        },
      });
    } catch (error) {
      next(error);
    }
  }

  /**
   * GET /chats — Chat xonalari ro'yxati
   */
  async getChats(req: AuthRequest, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = req.user!.id;
      const page = parseInt(req.query.page as string) || 1;
      const perPage = Math.min(parseInt(req.query.per_page as string) || 20, 50);

      const { rooms, total } = await chatService.getRooms(userId, page, perPage);

      res.json({
        success: true,
        data: rooms,
        meta: {
          page,
          per_page: perPage,
          total,
          total_pages: Math.ceil(total / perPage),
        },
      });
    } catch (error) {
      next(error);
    }
  }

  /**
   * GET /chats/:room_id/messages — Xabarlar tarixi
   */
  async getMessages(req: AuthRequest, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = req.user!.id;
      const roomId = req.params.room_id;
      const page = parseInt(req.query.page as string) || 1;
      const perPage = Math.min(parseInt(req.query.per_page as string) || 50, 100);

      // Kirishni tekshirish
      await chatService.getRoom(roomId, userId);

      const { messages, total } = await messageService.getMessages(roomId, page, perPage);

      res.json({
        success: true,
        data: messages,
        meta: {
          page,
          per_page: perPage,
          total,
          total_pages: Math.ceil(total / perPage),
        },
      });
    } catch (error) {
      next(error);
    }
  }

  /**
   * POST /chats/:room_id/messages — Xabar yuborish (REST fallback)
   */
  async sendMessage(req: AuthRequest, res: Response, next: NextFunction): Promise<void> {
    try {
      const parsed = sendMessageSchema.safeParse(req.body);
      if (!parsed.success) {
        throw new ValidationError('Validatsiya xatosi', parsed.error.flatten().fieldErrors);
      }

      const userId = req.user!.id;
      const roomId = req.params.room_id;

      // Kirishni tekshirish
      await chatService.getRoom(roomId, userId);

      const message = await messageService.sendMessage(roomId, userId, {
        content: parsed.data.content,
        message_type: parsed.data.message_type,
        media_url: parsed.data.media_url,
        metadata: parsed.data.metadata,
      });

      res.status(201).json({
        success: true,
        data: message,
      });
    } catch (error) {
      next(error);
    }
  }

  /**
   * PUT /chats/:room_id/read — O'qildi belgilash
   */
  async markAsRead(req: AuthRequest, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = req.user!.id;
      const roomId = req.params.room_id;

      await chatService.getRoom(roomId, userId);
      await messageService.markAsRead(roomId, userId);

      res.status(204).send();
    } catch (error) {
      next(error);
    }
  }
}

export const chatController = new ChatController();
