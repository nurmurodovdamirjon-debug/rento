package uz.rento.domain.usecase.chat

import uz.rento.domain.model.Message
import uz.rento.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * SendMessageUseCase — xabar yuborish (WS yoki REST fallback)
 */
class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    /**
     * WebSocket orqali xabar yuborish.
     * Agar WS ulanmagan bo'lsa, REST fallback ishlatiladi.
     */
    suspend operator fun invoke(
        roomId: String,
        content: String?,
        messageType: String = "text",
        mediaUrl: String? = null,
        metadata: Map<String, Any>? = null,
        useWebSocket: Boolean = true
    ): Result<Message?> {
        // Validatsiya
        if (messageType == "text" && content.isNullOrBlank()) {
            return Result.failure(IllegalArgumentException("Text xabar uchun content talab qilinadi"))
        }
        if (messageType == "image" && mediaUrl.isNullOrBlank()) {
            return Result.failure(IllegalArgumentException("Image xabar uchun media_url talab qilinadi"))
        }

        return if (useWebSocket) {
            // WS orqali — javob new_message event orqali keladi
            chatRepository.sendMessageViaWs(roomId, content, messageType, mediaUrl, metadata)
            Result.success(null) // WS da message flow orqali qaytadi
        } else {
            // REST fallback
            chatRepository.sendMessage(roomId, content, messageType, mediaUrl, metadata)
        }
    }
}
