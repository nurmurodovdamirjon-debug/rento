package uz.rento.domain.usecase.chat

import uz.rento.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * CreateChatUseCase — yangi chat boshlash
 */
class CreateChatUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    /**
     * E'lon uchun yangi chat yaratish yoki mavjud chatga o'tish.
     * @return room_id
     */
    suspend operator fun invoke(listingId: String, initialMessage: String): Result<String> {
        if (initialMessage.isBlank()) {
            return Result.failure(IllegalArgumentException("Boshlang'ich xabar bo'sh bo'lishi mumkin emas"))
        }
        if (initialMessage.length > 2000) {
            return Result.failure(IllegalArgumentException("Xabar juda uzun (max 2000 belgi)"))
        }
        return chatRepository.createChat(listingId, initialMessage)
    }
}
