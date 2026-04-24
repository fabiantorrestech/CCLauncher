package app.cclauncher.helper

import android.os.Build
import java.text.Normalizer

/**
 * Utilities for building and matching search aliases (transliteration and keyboard layout swap).
 */
object SearchAliasUtils {

    object Mode {
        const val OFF = 0
        const val TRANSLITERATION = 1
        const val KEYBOARD_SWAP = 2
        const val BOTH = 3
    }

    // RU<->EN keyboard layout (ЙЦУКЕН ↔ QWERTY) mapping; includes some punctuation on main rows
    private val en = charArrayOf(
        '`','q','w','e','r','t','y','u','i','o','p','[',']',
        'a','s','d','f','g','h','j','k','l',';','\'',
        'z','x','c','v','b','n','m',',','.'
    )
    private val ru = charArrayOf(
        'ё','й','ц','у','к','е','н','г','ш','щ','з','х','ъ',
        'ф','ы','в','а','п','р','о','л','д','ж','э',
        'я','ч','с','м','и','т','ь','б','ю'
    )
    private val enToRu = en.zip(ru).toMap() + en.map { it.uppercaseChar() }.zip(ru.map { it.uppercaseChar() }).toMap()
    private val ruToEn = ru.zip(en).toMap() + ru.map { it.uppercaseChar() }.zip(en.map { it.uppercaseChar() }).toMap()

    fun swapKeyboardLayout(text: String, ruToEnDirection: Boolean): String {
        val map = if (ruToEnDirection) ruToEn else enToRu
        val sb = StringBuilder(text.length)
        for (ch in text) sb.append(map[ch] ?: ch)
        return sb.toString()
    }

    fun asciiFold(input: String): String {
        val norm = Normalizer.normalize(input, Normalizer.Form.NFD)
        val sb = StringBuilder(norm.length)
        for (c in norm) {
            if (Character.getType(c) != Character.NON_SPACING_MARK.toInt()) sb.append(c)
        }
        return sb.toString().replace("[^\\p{ASCII}]".toRegex(), "")
    }

    private fun transliterate(id: String, text: String): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return try {
                val cls = Class.forName("android.icu.text.Transliterator")
                val getInstance = cls.getMethod("getInstance", String::class.java)
                val transform = cls.getMethod("transliterate", String::class.java)
                val inst = getInstance.invoke(null, id)
                transform.invoke(inst, text) as String
            } catch (_: Throwable) {
                text
            }
        }
        return text
    }

    fun anyToLatin(text: String): String {
        // Any script -> Latin -> ASCII-ish
        val toLatin = transliterate("Any-Latin", text)
        return asciiFold(toLatin)
    }

    fun latinToCyrillic(text: String): String = transliterate("Latin-Cyrillic", text)

    fun normalize(s: String): String = s.lowercase().trim()

    fun buildSearchTerms(text: String, mode: Int): Set<String> {
        val normalized = normalize(text)
        if (normalized.isBlank()) return emptySet()

        val out = LinkedHashSet<String>(8)
        out += normalized
        out += asciiFold(normalized)

        if (mode == Mode.TRANSLITERATION || mode == Mode.BOTH) {
            val toLatin = normalize(anyToLatin(text))
            val toCyr = normalize(latinToCyrillic(text))
            out += toLatin
            out += asciiFold(toLatin)
            out += toCyr
        }

        if (mode == Mode.KEYBOARD_SWAP || mode == Mode.BOTH) {
            out += normalize(swapKeyboardLayout(text, ruToEnDirection = true))
            out += normalize(swapKeyboardLayout(text, ruToEnDirection = false))
        }

        return out.filter { it.isNotBlank() }.toSet()
    }

    /**
     * Build a set of aliases for an app label and optional package name.
     */
    fun buildAppAliases(
        label: String,
        packageName: String?,
        mode: Int,
        includePkg: Boolean
    ): Set<String> {
        val out = LinkedHashSet<String>(8)
        out += buildSearchTerms(label, mode)

        if (includePkg && !packageName.isNullOrBlank()) {
            val pkgTail = packageName.substringAfterLast('.')
            out += normalize(pkgTail)
            out += asciiFold(pkgTail)
        }

        return out.filter { it.isNotBlank() }.toSet()
    }

    /**
     * Build normalized variants of a user query based on selected mode.
     */
    fun buildQueryVariants(query: String, mode: Int): Set<String> {
        return buildSearchTerms(query, mode)
    }
}
