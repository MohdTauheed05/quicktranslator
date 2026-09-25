package com.example.domain.model

import java.util.Locale

data class Language(
    val code: String,
    val name: String,
    val nativeName: String,
    val flagEmoji: String,
    val isRtl: Boolean = false,
    val locale: Locale = Locale.forLanguageTag(code)
) {
    companion object {
        val AUTO = Language(
            code = "auto",
            name = "Auto Detect",
            nativeName = "تحديد تلقائي / Auto",
            flagEmoji = "🌐",
            isRtl = false,
            locale = Locale.getDefault()
        )

        val ENGLISH = Language("en", "English", "English", "🇺🇸", false, Locale.ENGLISH)
        val ARABIC = Language("ar", "Arabic", "العربية", "🇸🇦", true, Locale("ar"))
        val HINDI = Language("hi", "Hindi", "हिन्दी", "🇮🇳", false, Locale("hi", "IN"))
        val URDU = Language("ur", "Urdu", "اردو", "🇵🇰", true, Locale("ur", "PK"))
        val NEPALI = Language("ne", "Nepali", "नेपाली", "🇳🇵", false, Locale("ne", "NP"))
        val FRENCH = Language("fr", "French", "Français", "🇫🇷", false, Locale.FRENCH)
        val SPANISH = Language("es", "Spanish", "Español", "🇪🇸", false, Locale("es", "ES"))
        val GERMAN = Language("de", "German", "Deutsch", "🇩🇪", false, Locale.GERMAN)
        val PORTUGUESE = Language("pt", "Portuguese", "Português", "🇧🇷", false, Locale("pt", "BR"))
        val RUSSIAN = Language("ru", "Russian", "Русский", "🇷🇺", false, Locale("ru", "RU"))
        val CHINESE = Language("zh", "Chinese", "中文 (简体)", "🇨🇳", false, Locale.SIMPLIFIED_CHINESE)
        val TURKISH = Language("tr", "Turkish", "Türkçe", "🇹🇷", false, Locale("tr", "TR"))
        val PERSIAN = Language("fa", "Persian", "فارسی", "🇮🇷", true, Locale("fa", "IR"))
        val ITALIAN = Language("it", "Italian", "Italiano", "🇮🇹", false, Locale.ITALIAN)
        val JAPANESE = Language("ja", "Japanese", "日本語", "🇯🇵", false, Locale.JAPANESE)
        val KOREAN = Language("ko", "Korean", "한국어", "🇰🇷", false, Locale.KOREAN)
        val INDONESIAN = Language("id", "Indonesian", "Bahasa Indonesia", "🇮🇩", false, Locale("id", "ID"))
        val MALAY = Language("ms", "Malay", "Bahasa Melayu", "🇲🇾", false, Locale("ms", "MY"))
        val BENGALI = Language("bn", "Bengali", "বাংলা", "🇧🇩", false, Locale("bn", "BD"))

        val ALL_LANGUAGES: List<Language> = listOf(
            ENGLISH,
            ARABIC,
            HINDI,
            URDU,
            NEPALI,
            FRENCH,
            SPANISH,
            GERMAN,
            PORTUGUESE,
            RUSSIAN,
            CHINESE,
            TURKISH,
            PERSIAN,
            ITALIAN,
            JAPANESE,
            KOREAN,
            INDONESIAN,
            MALAY,
            BENGALI
        )

        fun fromCode(code: String): Language {
            if (code == "auto") return AUTO
            return ALL_LANGUAGES.find { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
        }
    }
}
