package com.example.stbplay.data

/**
 * Providers are inconsistent about their adult flags.  We keep provider
 * flags intact and add a narrow, display-name based safety net.  It only
 * controls local PIN prompts; it never changes the provider category id.
 */
object ContentSafety {
    private val adultTerms = Regex(
        """(?:^|\\b)(adult|xxx|porn|erotic|sex(?:y)?|18\\+|x-rated|hentai)(?:\\b|$)""",
        RegexOption.IGNORE_CASE
    )

    fun isRestricted(vararg values: String?): Boolean = values
        .filterNotNull()
        .any { adultTerms.containsMatchIn(it) }
}
