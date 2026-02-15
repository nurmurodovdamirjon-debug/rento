package uz.rento.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import uz.rento.data.local.entity.ChatRoomEntity
import uz.rento.data.local.entity.MessageEntity

/**
 * ChatDao — chat xonalari va xabarlar offline cache
 */
@Dao
interface ChatDao {

    // ==================== CHAT ROOMS ====================

    @Query("SELECT * FROM chat_rooms ORDER BY updated_at DESC LIMIT :limit OFFSET :offset")
    suspend fun getChatRooms(limit: Int, offset: Int): List<ChatRoomEntity>

    @Query("SELECT * FROM chat_rooms WHERE room_id = :roomId")
    suspend fun getChatRoom(roomId: String): ChatRoomEntity?

    @Query("SELECT COUNT(*) FROM chat_rooms")
    suspend fun getChatRoomCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatRoom(chatRoom: ChatRoomEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatRooms(chatRooms: List<ChatRoomEntity>)

    @Query("DELETE FROM chat_rooms WHERE room_id = :roomId")
    suspend fun deleteChatRoom(roomId: String)

    @Query("DELETE FROM chat_rooms")
    suspend fun clearChatRooms()

    // ==================== MESSAGES ====================

    @Query("SELECT * FROM messages WHERE room_id = :roomId ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    suspend fun getMessages(roomId: String, limit: Int, offset: Int): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessage(messageId: String): MessageEntity?

    @Query("SELECT COUNT(*) FROM messages WHERE room_id = :roomId")
    suspend fun getMessageCount(roomId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("UPDATE messages SET is_read = 1, read_at = :readAt WHERE room_id = :roomId AND is_read = 0")
    suspend fun markAsRead(roomId: String, readAt: String)

    @Query("DELETE FROM messages WHERE room_id = :roomId")
    suspend fun deleteMessages(roomId: String)

    /** Eski xabarlarni tozalash (7 kundan eski) */
    @Query("DELETE FROM messages WHERE cached_at < :cutoff")
    suspend fun deleteOldMessages(cutoff: Long)

    @Query("DELETE FROM messages")
    suspend fun clearMessages()

    // ==================== TRANSACTION ====================

    @Transaction
    suspend fun deleteChatRoomWithMessages(roomId: String) {
        deleteMessages(roomId)
        deleteChatRoom(roomId)
    }

    @Transaction
    suspend fun clearAll() {
        clearMessages()
        clearChatRooms()
    }
}
