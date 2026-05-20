package io.github.kiranshny.rhinolens.shared.domain

object Languages {

    val arabic = Language(LanguageCode("ar"), "Arabic", "العربية")
    val german = Language(LanguageCode("de"), "German", "Deutsch")
    val english = Language(LanguageCode("en"), "English", "English")
    val spanish = Language(LanguageCode("es"), "Spanish", "Español")
    val french = Language(LanguageCode("fr"), "French", "Français")
    val hindi = Language(LanguageCode("hi"), "Hindi", "हिन्दी")
    val indonesian = Language(LanguageCode("id"), "Indonesian", "Bahasa Indonesia")
    val italian = Language(LanguageCode("it"), "Italian", "Italiano")
    val japanese = Language(LanguageCode("ja"), "Japanese", "日本語")
    val korean = Language(LanguageCode("ko"), "Korean", "한국어")
    val dutch = Language(LanguageCode("nl"), "Dutch", "Nederlands")
    val polish = Language(LanguageCode("pl"), "Polish", "Polski")
    val portuguese = Language(LanguageCode("pt"), "Portuguese", "Português")
    val russian = Language(LanguageCode("ru"), "Russian", "Русский")
    val thai = Language(LanguageCode("th"), "Thai", "ไทย")
    val turkish = Language(LanguageCode("tr"), "Turkish", "Türkçe")
    val ukrainian = Language(LanguageCode("uk"), "Ukrainian", "Українська")
    val vietnamese = Language(LanguageCode("vi"), "Vietnamese", "Tiếng Việt")
    val chinese = Language(LanguageCode("zh"), "Chinese", "中文")

    val all: List<Language> = listOf(
        arabic, german, english, spanish, french, hindi, indonesian, italian,
        japanese, korean, dutch, polish, portuguese, russian, thai, turkish,
        ukrainian, vietnamese, chinese,
    )

    private val byCodeMap: Map<String, Language> = all.associateBy { it.code.value }

    fun byCode(code: LanguageCode): Language? = byCodeMap[code.value]

    fun byTag(tag: String): Language? {
        val normalised = tag.lowercase().substringBefore('-').substringBefore('_')
        return byCodeMap[normalised]
    }

    val default: Language = english
}
