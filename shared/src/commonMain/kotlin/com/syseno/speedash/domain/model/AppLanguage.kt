package com.syseno.speedash.domain.model

/**
 * Supported application languages for multilingual localization.
 *
 * @property code BCP 47 language tag (e.g. "en", "id").
 * @property displayName English display name.
 * @property nativeName Native display name in the language itself.
 * @property flagEmoji Regional flag emoji for UI display.
 */
enum class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val flagEmoji: String
) {
    ENGLISH(
        code = "en",
        displayName = "English",
        nativeName = "English",
        flagEmoji = "🇺🇸"
    ),
    INDONESIAN(
        code = "id",
        displayName = "Indonesian",
        nativeName = "Bahasa Indonesia",
        flagEmoji = "🇮🇩"
    );

    fun next(): AppLanguage = entries[(ordinal + 1) % entries.size]
}
