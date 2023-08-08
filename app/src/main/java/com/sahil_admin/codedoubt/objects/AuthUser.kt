package com.sahil_admin.codedoubt.objects

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AuthUser (
    val userId: String? = null,
    val email: String? = null,
    var name: String? = null,
    var upvotes: Int? = null,
    var upvoted_list: MutableList<String?>? = null
) : Parcelable
