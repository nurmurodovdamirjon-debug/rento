import { Router } from 'express';
import { chatController } from '../controllers/chat.controller';
import { authMiddleware } from '../middleware/auth.middleware';

const router = Router();

// Barcha chat route'lari autentifikatsiya talab qiladi
router.use(authMiddleware as never);

// POST /chats — yangi chat boshlash
router.post('/', (req, res, next) => chatController.createChat(req as never, res, next));

// GET /chats — chat xonalari ro'yxati
router.get('/', (req, res, next) => chatController.getChats(req as never, res, next));

// GET /chats/:room_id/messages — xabarlar tarixi
router.get('/:room_id/messages', (req, res, next) => chatController.getMessages(req as never, res, next));

// POST /chats/:room_id/messages — xabar yuborish (REST fallback)
router.post('/:room_id/messages', (req, res, next) => chatController.sendMessage(req as never, res, next));

// PUT /chats/:room_id/read — o'qildi belgilash
router.put('/:room_id/read', (req, res, next) => chatController.markAsRead(req as never, res, next));

export default router;
