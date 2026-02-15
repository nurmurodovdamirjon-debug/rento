package uz.rento.domain.usecase.chat

import uz.rento.domain.model.ChatRoom
import uz.rento.domain.repository.ChatRepository
import uz.rento.domain.repository.ChatsPage
import javax.inject.Inject

/**
 * GetChatsUseCase — chat xonalari ro'yxatini olish
 */
class GetChatsUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(page: Int = 1, perPage: Int = 20): Result<ChatsPage> {
        return chatRepository.getChats(page, perPage)
    }
}
