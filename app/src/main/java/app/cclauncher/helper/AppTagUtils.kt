package app.cclauncher.helper

object AppTagUtils {
    const val MAX_TAGS_PER_APP = 20

    fun normalizeTagInput(tag: String): String =
        tag.trim().replace(Regex("\\s+"), " ")

    fun canonicalizeTag(tag: String): String =
        SearchAliasUtils.normalize(normalizeTagInput(tag))

    fun normalizeTags(tags: Iterable<String>, maxCount: Int = MAX_TAGS_PER_APP): List<String> {
        val normalized = ArrayList<String>(maxCount)
        val seen = HashSet<String>(maxCount)

        for (tag in tags) {
            val cleaned = normalizeTagInput(tag)
            val canonical = canonicalizeTag(cleaned)
            if (canonical.isBlank() || !seen.add(canonical)) continue
            normalized += cleaned
            if (normalized.size >= maxCount) break
        }

        return normalized
    }

    fun normalizeTagMap(tagsByApp: Map<String, List<String>>): Map<String, List<String>> =
        buildMap(tagsByApp.size) {
            tagsByApp.forEach { (appKey, tags) ->
                val normalized = normalizeTags(tags)
                if (appKey.isNotBlank() && normalized.isNotEmpty()) {
                    put(appKey, normalized)
                }
            }
        }
}
