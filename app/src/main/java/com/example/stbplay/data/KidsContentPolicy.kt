package com.example.stbplay.data

import com.example.stbplay.data.model.PortalCategory
import com.example.stbplay.data.model.PortalStream

internal fun isKidsCategory(category: PortalCategory): Boolean = !category.isAdultCategory() &&
    Regex("(?i)(?:^|[^a-z])(kids|children|cartoons|cartoon|junior)(?:$|[^a-z])").containsMatchIn(category.name)

/** Unknown/unclassified categories stay hidden in Kids mode; adult flags always win. */
internal fun isKidsContentAllowed(stream: PortalStream, approvedCategories: Set<String>): Boolean =
    !stream.isAdultContent() && !stream.isLocked && stream.categoryId in approvedCategories
