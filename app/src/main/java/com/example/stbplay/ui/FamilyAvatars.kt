package com.example.stbplay.ui

import androidx.annotation.DrawableRes
import com.example.stbplay.R

/** Stable IDs keep saved avatar choices independent of resource numbering. */
data class FamilyAvatar(val id: String, val label: String, @DrawableRes val drawable: Int)

val familyAvatars = listOf(
    FamilyAvatar("family:indian_dad", "Indian Dad", R.drawable.avatar_indian_dad),
    FamilyAvatar("family:indian_mom", "Indian Mom", R.drawable.avatar_indian_mom),
    FamilyAvatar("family:indian_boy", "Indian Boy", R.drawable.avatar_indian_boy),
    FamilyAvatar("family:indian_girl", "Indian Girl", R.drawable.avatar_indian_girl),
    FamilyAvatar("family:indian_toddler", "Indian Toddler", R.drawable.avatar_indian_toddler),
    FamilyAvatar("family:punjabi_dad", "Punjabi Dad", R.drawable.avatar_punjabi_dad),
    FamilyAvatar("family:white_mom", "White Mom", R.drawable.avatar_white_mom),
    FamilyAvatar("family:black_dad", "Black Dad", R.drawable.avatar_black_dad),
    FamilyAvatar("family:punjabi_mom", "Punjabi Mom", R.drawable.avatar_punjabi_mom),
    FamilyAvatar("family:white_dad", "White Dad", R.drawable.avatar_white_dad),
    FamilyAvatar("family:black_mom", "Black Mom", R.drawable.avatar_black_mom),
    FamilyAvatar("family:punjabi_boy", "Punjabi Boy", R.drawable.avatar_punjabi_boy),
    FamilyAvatar("family:punjabi_girl", "Punjabi Girl", R.drawable.avatar_punjabi_girl),
    FamilyAvatar("family:punjabi_toddler", "Punjabi Toddler", R.drawable.avatar_punjabi_toddler),
    FamilyAvatar("family:white_young_man", "White Young Man", R.drawable.avatar_white_young_man),
    FamilyAvatar("family:white_young_woman", "White Young Woman", R.drawable.avatar_white_young_woman),
    FamilyAvatar("family:black_young_man", "Black Young Man", R.drawable.avatar_black_young_man),
    FamilyAvatar("family:black_young_woman", "Black Young Woman", R.drawable.avatar_black_young_woman)
)
