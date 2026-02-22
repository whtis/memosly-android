package com.whtis.memosly.core.network

import com.whtis.memosly.core.model.*
import com.whtis.memosly.core.network.dto.*

fun UserDto.toDomain() = User(
    name = name,
    id = name.substringAfterLast("/").toIntOrNull() ?: 0,
    role = role.toUserRole(),
    username = username,
    email = email,
    nickname = nickname.ifBlank { displayName },
    avatarUrl = avatarUrl,
    description = description,
    createTime = createTime,
    updateTime = updateTime,
)

fun MemoDto.toDomain() = Memo(
    name = name,
    uid = uid.ifBlank { name.substringAfterLast("/") },
    creator = creator,
    createTime = createTime,
    updateTime = updateTime,
    displayTime = displayTime,
    content = content,
    visibility = visibility.toVisibility(),
    state = state.toMemoState(),
    pinned = pinned,
    resources = ((resources ?: emptyList()) + (attachments ?: emptyList())).map { it.toDomain() },
    relations = relations?.map { it.toDomain() } ?: emptyList(),
    reactions = reactions?.map { it.toDomain() } ?: emptyList(),
    tags = tags ?: emptyList(),
    snippet = snippet ?: "",
)

fun ResourceDto.toDomain() = Resource(
    name = name,
    uid = uid.ifBlank { name.substringAfterLast("/") },
    createTime = createTime,
    filename = filename,
    type = type,
    size = size.toLongOrNull() ?: 0L,
    memo = memo ?: "",
    externalLink = externalLink ?: "",
)

fun MemoRelationDto.toDomain() = MemoRelation(
    memo = memo.name,
    relatedMemo = relatedMemo.name,
    type = type.toMemoRelationType(),
)

fun ReactionDto.toDomain() = Reaction(
    id = if (id != 0) id else name.substringAfterLast("/").toIntOrNull() ?: 0,
    creator = creator,
    contentId = contentId,
    reactionType = reactionType,
)

fun TagDto.toDomain() = Tag(
    name = name,
    creator = creator,
)

fun WorkspaceProfileDto.toDomain() = WorkspaceProfile(
    owner = owner,
    version = version,
    mode = mode,
)

fun AccessTokenDto.toDomain() = UserAccessToken(
    accessToken = accessToken,
    description = description,
    issuedAt = issuedAt,
    expiresAt = expiresAt ?: "",
)

fun UserStatsDto.toDomain() = UserStats(
    memoDisplayTimestamps = memoDisplayTimestamps ?: emptyList(),
    memoTypeStats = memoTypeStats ?: emptyMap(),
    tagCount = tagCount ?: emptyMap(),
)

fun WebhookDto.toDomain() = Webhook(
    id = id,
    creatorId = creatorId,
    name = name,
    url = url,
    createTime = createTime,
    updateTime = updateTime,
)

fun InboxMessageDto.toDomain() = InboxMessage(
    name = name,
    sender = sender,
    receiver = receiver,
    status = status.toInboxStatus(),
    createTime = createTime,
    type = type.toInboxType(),
    activityId = activityId,
)

fun IdentityProviderDto.toDomain() = IdentityProvider(
    name = name ?: "",
    id = id ?: 0,
    type = type.toIdentityProviderType(),
    title = title ?: "",
    identifierFilter = identifierFilter ?: "",
    config = config?.toDomain() ?: IdentityProviderConfig(oauth2 = null),
)

fun IdentityProviderConfigDto.toDomain() = IdentityProviderConfig(
    oauth2 = oauth2Config?.toDomain(),
)

fun OAuth2ConfigDto.toDomain() = OAuth2Config(
    clientId = clientId ?: "",
    clientSecret = clientSecret ?: "",
    authUrl = authUrl ?: "",
    tokenUrl = tokenUrl ?: "",
    userInfoUrl = userInfoUrl ?: "",
    scopes = scopes ?: emptyList(),
    fieldMapping = fieldMapping?.toDomain() ?: FieldMapping("", "", ""),
)

fun FieldMappingDto.toDomain() = FieldMapping(
    identifier = identifier ?: "",
    displayName = displayName ?: "",
    email = email ?: "",
)

private fun String.toUserRole(): UserRole = when (this) {
    "HOST", "1" -> UserRole.HOST
    "ADMIN", "2" -> UserRole.ADMIN
    "USER", "3" -> UserRole.USER
    else -> UserRole.UNKNOWN
}

private fun String.toMemoState(): MemoState = when (this) {
    "NORMAL", "1" -> MemoState.NORMAL
    "ARCHIVED", "2" -> MemoState.ARCHIVED
    else -> MemoState.UNKNOWN
}

private fun String.toVisibility(): Visibility = when (this) {
    "PRIVATE", "1" -> Visibility.PRIVATE
    "PROTECTED", "2" -> Visibility.PROTECTED
    "PUBLIC", "3" -> Visibility.PUBLIC
    else -> Visibility.UNKNOWN
}

private fun String.toMemoRelationType(): MemoRelationType = when (this) {
    "REFERENCE", "1" -> MemoRelationType.REFERENCE
    "COMMENT", "2" -> MemoRelationType.COMMENT
    else -> MemoRelationType.UNKNOWN
}

private fun String.toInboxStatus(): InboxStatus = when (this) {
    "UNREAD" -> InboxStatus.UNREAD
    "READ" -> InboxStatus.READ
    "ARCHIVED" -> InboxStatus.ARCHIVED
    else -> InboxStatus.UNKNOWN
}

private fun String?.toInboxType(): InboxType = when (this) {
    "MEMO_COMMENT" -> InboxType.MEMO_COMMENT
    "VERSION_UPDATE" -> InboxType.VERSION_UPDATE
    else -> InboxType.UNKNOWN
}

private fun String?.toIdentityProviderType(): IdentityProviderType = when (this) {
    "OAUTH2" -> IdentityProviderType.OAUTH2
    else -> IdentityProviderType.UNKNOWN
}
