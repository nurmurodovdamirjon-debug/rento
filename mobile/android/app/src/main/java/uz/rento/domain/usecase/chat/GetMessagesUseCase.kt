package uz.rento.domain.usecase.chat

import uz.rento.domain.model.Message
import uz.rento.domain.repository.ChatRepository
import uz.rento.domain.repository.MessagesPage
import javax.inject.Inject

/**
 * GetMessagesUseCase — xabarlar tarixini olish
 */
class GetMessagesUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(
        roomId: String,
        page: Int = 1,
        perPage: Int = 50
    ): Result<MessagesPage> {
        return chatRepository.getMessages(roomId, page, perPage)
    }
}
