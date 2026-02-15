import { Server } from 'socket.io';
import { config } from '../config';
import { redis } from '../config/redis';
import { chatService } from '../services/chat.service';
import { messageService } from '../services/message.service';
import { AuthSocket } from '../types';
import { logger } from '../utils/logger';

/**
 * WebSocket event handler — barcha real-time chat logikasi.
 *
 * Client → Server: join_room, leave_room, send_message, typing_start, typing_stop, mark_read, ping
 * Server → Client: new_message, user_typing, user_stop_typing, message_read, user_online, user_offline, pong, error
 */
export function registerSocketHandlers(io: Server): void {
  io.on('connection', async (rawSocket) => {
    const socket = rawSocket as AuthSocket;
    const userId = socket.data.userId;

    logger.info(`User connected: ${userId} (socket: ${socket.id})`);

    // Online belgilash
    await chatService.setUserOnline(userId);
    socket.broadcast.emit('user_online', { user_id: userId });

    // ————— join_room —————
    socket.on('join_room', async (data: { room_id: string }) => {
      try {
        const room = await chatService.getRoom(data.room_id, userId);
        await socket.join(room.id);
        logger.debug(`User ${userId} joined room ${room.id}`);
      } catch (error) {
        socket.emit('error', toErrorPayload(error));
      }
    });

    // ————— leave_room —————
    socket.on('leave_room', (data: { room_id: string }) => {
      socket.leave(data.room_id);
      logger.debug(`User ${userId} left room ${data.room_id}`);
    });

    // ————— send_message —————
    socket.on('send_message', async (data: {
      room_id: string;
      content?: string;
      type?: string;
      media_url?: string;
      metadata?: Record<string, unknown>;
    }) => {
      try {
        // Rate limiting: 30 msg/min/room
        const rateLimitKey = `ws:ratelimit:${data.room_id}:${userId}`;
        const currentCount = await redis.incr(rateLimitKey);
        if (currentCount === 1) {
          await redis.pexpire(rateLimitKey, config.wsRateLimit.windowMs);
        }
        if (currentCount > config.wsRateLimit.maxMessages) {
          socket.emit('error', {
            code: 'RATE_LIMIT_EXCEEDED',
            message: 'Xabar yuborish limiti oshib ketdi (30/min)',
          });
          return;
        }

        // Xonaga kirish huquqi
        await chatService.getRoom(data.room_id, userId);

        const messageType = (data.type || 'text') as 'text' | 'image' | 'location' | 'contact';

        const message = await messageService.sendMessage(data.room_id, userId, {
          content: data.content,
          message_type: messageType,
          media_url: data.media_url,
          metadata: data.metadata,
        });

        // new_message xonaga yuborish (yuboruvchiga ham)
        io.to(data.room_id).emit('new_message', {
          id: message.id,
          room_id: message.room_id,
          sender_id: message.sender_id,
          content: message.content,
          type: message.message_type,
          media_url: message.media_url,
          metadata: message.metadata,
          created_at: message.created_at,
        });
      } catch (error) {
        socket.emit('error', toErrorPayload(error));
      }
    });

    // ————— typing_start —————
    socket.on('typing_start', (data: { room_id: string }) => {
      socket.to(data.room_id).emit('user_typing', {
        room_id: data.room_id,
        user_id: userId,
      });
    });

    // ————— typing_stop —————
    socket.on('typing_stop', (data: { room_id: string }) => {
      socket.to(data.room_id).emit('user_stop_typing', {
        room_id: data.room_id,
        user_id: userId,
      });
    });

    // ————— mark_read —————
    socket.on('mark_read', async (data: { room_id: string; message_id: string }) => {
      try {
        await chatService.getRoom(data.room_id, userId);
        await messageService.markAsReadUpTo(data.room_id, userId, data.message_id);

        socket.to(data.room_id).emit('message_read', {
          room_id: data.room_id,
          reader_id: userId,
          last_read_id: data.message_id,
        });
      } catch (error) {
        socket.emit('error', toErrorPayload(error));
      }
    });

    // ————— ping/pong —————
    socket.on('ping', () => {
      socket.emit('pong', {});
      // Online TTL yangilash
      chatService.setUserOnline(userId).catch(() => {});
    });

    // ————— disconnect —————
    socket.on('disconnect', async (reason) => {
      logger.info(`User disconnected: ${userId} (reason: ${reason})`);
      await chatService.setUserOffline(userId);
      socket.broadcast.emit('user_offline', { user_id: userId });
    });
  });
}

/**
 * Xato payloadini standartlashtirish.
 */
function toErrorPayload(error: unknown): { code: string; message: string } {
  if (error && typeof error === 'object' && 'code' in error && 'message' in error) {
    const e = error as { code: string; message: string };
    return { code: e.code, message: e.message };
  }
  return { code: 'INTERNAL_ERROR', message: 'Ichki xato yuz berdi' };
}
