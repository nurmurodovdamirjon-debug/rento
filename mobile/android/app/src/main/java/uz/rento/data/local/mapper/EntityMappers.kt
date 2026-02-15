package uz.rento.data.local.mapper

import uz.rento.data.local.entity.ChatRoomEntity
import uz.rento.data.local.entity.FavoriteEntity
import uz.rento.data.local.entity.ListingEntity
import uz.rento.data.local.entity.ListingImageEntity
import uz.rento.data.local.entity.MessageEntity
import uz.rento.data.local.entity.UserEntity
import uz.rento.domain.model.ChatListing
import uz.rento.domain.model.ChatRoom
import uz.rento.domain.model.ChatUser
import uz.rento.domain.model.DealType
import uz.rento.domain.model.FavoriteItem
import uz.rento.domain.model.LastMessage
import uz.rento.domain.model.Listing
import uz.rento.domain.model.ListingImage
import uz.rento.domain.model.ListingStatus
import uz.rento.domain.model.ListingType
import uz.rento.domain.model.Message
import uz.rento.domain.model.MessageType
import uz.rento.domain.model.User

// ==================== LISTING ====================

fun ListingEntity.toDomain(images: List<ListingImageEntity>): Listing = Listing(
    id = id,
    userId = userId,
    type = ListingType.fromValue(type),
    dealType = DealType.fromValue(dealType),
    city = city,
    district = district,
    address = address,
    landmark = landmark,
    latitude = latitude,
    longitude = longitude,
    rooms = rooms,
    floor = floor,
    totalFloors = totalFloors,
    areaSqm = areaSqm,
    price = price,
    currency = currency,
    priceNegotiable = priceNegotiable,
    hasFurniture = hasFurniture,
    hasAppliances = hasAppliances,
    hasInternet = hasInternet,
    hasParking = hasParking,
    hasConditioner = hasConditioner,
    allowsPets = allowsPets,
    allowsChildren = allowsChildren,
    utilitiesIncluded = utilitiesIncluded,
    depositAmount = depositAmount,
    status = ListingStatus.fromValue(status),
    rejectionReason = rejectionReason,
    isPremium = isPremium,
    viewsCount = viewsCount,
    favoritesCount = favoritesCount,
    contactsCount = contactsCount,
    title = title,
    description = description,
    images = images.map { it.toDomain() },
    publishedAt = publishedAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ListingImageEntity.toDomain(): ListingImage = ListingImage(
    id = id,
    url = url,
    thumbnailUrl = thumbnailUrl,
    sortOrder = sortOrder,
    isMain = isMain
)

fun Listing.toEntity(): ListingEntity = ListingEntity(
    id = id,
    userId = userId,
    type = type.value,
    dealType = dealType.value,
    city = city,
    district = district,
    address = address,
    landmark = landmark,
    latitude = latitude,
    longitude = longitude,
    rooms = rooms,
    floor = floor,
    totalFloors = totalFloors,
    areaSqm = areaSqm,
    price = price,
    currency = currency,
    priceNegotiable = priceNegotiable,
    hasFurniture = hasFurniture,
    hasAppliances = hasAppliances,
    hasInternet = hasInternet,
    hasParking = hasParking,
    hasConditioner = hasConditioner,
    allowsPets = allowsPets,
    allowsChildren = allowsChildren,
    utilitiesIncluded = utilitiesIncluded,
    depositAmount = depositAmount,
    status = status.value,
    rejectionReason = rejectionReason,
    isPremium = isPremium,
    viewsCount = viewsCount,
    favoritesCount = favoritesCount,
    contactsCount = contactsCount,
    title = title,
    description = description,
    publishedAt = publishedAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ListingImage.toEntity(listingId: String): ListingImageEntity = ListingImageEntity(
    id = id,
    listingId = listingId,
    url = url,
    thumbnailUrl = thumbnailUrl,
    sortOrder = sortOrder,
    isMain = isMain
)

// ==================== CHAT ====================

fun ChatRoomEntity.toDomain(): ChatRoom = ChatRoom(
    roomId = roomId,
    listing = ChatListing(
        id = listingId,
        title = listingTitle,
        imageUrl = listingImageUrl
    ),
    otherUser = ChatUser(
        id = otherUserId,
        fullName = otherUserName,
        avatarUrl = otherUserAvatar,
        isOnline = otherUserOnline,
        lastSeenAt = otherUserLastSeen
    ),
    lastMessage = if (lastMessageContent != null || lastMessageSenderId != null) {
        LastMessage(
            content = lastMessageContent,
            senderId = lastMessageSenderId ?: "",
            createdAt = lastMessageCreatedAt ?: ""
        )
    } else null,
    unreadCount = unreadCount,
    createdAt = createdAt
)

fun ChatRoom.toEntity(): ChatRoomEntity = ChatRoomEntity(
    roomId = roomId,
    listingId = listing.id,
    listingTitle = listing.title,
    listingImageUrl = listing.imageUrl,
    otherUserId = otherUser.id,
    otherUserName = otherUser.fullName,
    otherUserAvatar = otherUser.avatarUrl,
    otherUserOnline = otherUser.isOnline,
    otherUserLastSeen = otherUser.lastSeenAt,
    lastMessageContent = lastMessage?.content,
    lastMessageSenderId = lastMessage?.senderId,
    lastMessageCreatedAt = lastMessage?.createdAt,
    unreadCount = unreadCount,
    createdAt = createdAt,
    updatedAt = lastMessage?.createdAt ?: createdAt
)

fun MessageEntity.toDomain(): Message = Message(
    id = id,
    roomId = roomId,
    senderId = senderId,
    content = content,
    messageType = MessageType.fromValue(messageType),
    mediaUrl = mediaUrl,
    metadata = null,
    isRead = isRead,
    readAt = readAt,
    createdAt = createdAt
)

fun Message.toEntity(): MessageEntity = MessageEntity(
    id = id,
    roomId = roomId,
    senderId = senderId,
    content = content,
    messageType = messageType.value,
    mediaUrl = mediaUrl,
    isRead = isRead,
    readAt = readAt,
    createdAt = createdAt
)

// ==================== FAVORITE ====================

fun FavoriteEntity.toDomain(): FavoriteItem = FavoriteItem(
    favoriteId = favoriteId,
    listingId = listingId,
    title = title,
    city = city,
    price = price,
    currency = currency,
    rooms = rooms,
    areaSqm = areaSqm,
    imageUrl = imageUrl,
    status = status,
    isPremium = isPremium,
    favoritedAt = favoritedAt
)

fun FavoriteItem.toEntity(): FavoriteEntity = FavoriteEntity(
    favoriteId = favoriteId,
    listingId = listingId,
    title = title,
    city = city,
    price = price,
    currency = currency,
    rooms = rooms,
    areaSqm = areaSqm,
    imageUrl = imageUrl,
    status = status,
    isPremium = isPremium,
    favoritedAt = favoritedAt
)

// ==================== USER ====================

fun UserEntity.toDomain(): User = User(
    id = id,
    phone = phone,
    phoneVerified = phoneVerified,
    fullName = fullName,
    email = email,
    avatarUrl = avatarUrl,
    role = role,
    idVerified = idVerified,
    ratingAvg = ratingAvg,
    ratingCount = ratingCount,
    subscription = subscription,
    language = language,
    lastSeenAt = lastSeenAt,
    createdAt = createdAt,
    isActive = isActive
)

fun User.toEntity(): UserEntity = UserEntity(
    id = id,
    phone = phone,
    phoneVerified = phoneVerified,
    fullName = fullName,
    email = email,
    avatarUrl = avatarUrl,
    role = role,
    idVerified = idVerified,
    ratingAvg = ratingAvg,
    ratingCount = ratingCount,
    subscription = subscription,
    language = language,
    lastSeenAt = lastSeenAt,
    createdAt = createdAt,
    isActive = isActive
)
