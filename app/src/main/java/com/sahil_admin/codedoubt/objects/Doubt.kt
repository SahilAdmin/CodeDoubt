package com.sahil_admin.codedoubt.objects

data class Doubt (
    val doubtChannelId: String? = null,
    val question: String? = null,
    val solved: Boolean = false,
    val author: String? = null
)