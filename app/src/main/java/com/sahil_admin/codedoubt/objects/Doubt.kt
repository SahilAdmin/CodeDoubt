package com.sahil_admin.codedoubt.objects

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Doubt (
    val doubtChannelId: String? = null,
    val question: String? = null,
    val solved: Boolean = false,
    val author: String? = null
) : Parcelable