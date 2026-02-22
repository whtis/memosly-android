package com.whtis.memosly.core.model

data class Reaction(
    val id: Int,
    val creator: String,
    val contentId: String,
    val reactionType: String,
)

/** Common emojis for the reaction picker. */
object ReactionTypes {
    val supportedEmojis: List<String> = listOf(
        "\uD83D\uDC4D", // 👍
        "\uD83D\uDC4E", // 👎
        "❤\uFE0F",      // ❤️
        "\uD83D\uDD25", // 🔥
        "\uD83D\uDC4F", // 👏
        "\uD83D\uDE02", // 😂
        "\uD83D\uDC4C", // 👌
        "\uD83D\uDE80", // 🚀
        "\uD83D\uDC40", // 👀
        "\uD83E\uDD14", // 🤔
        "\uD83E\uDD21", // 🤡
        "❓",           // ❓
    )
}
