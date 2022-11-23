package com.sahil_admin.codedoubt.objects

data class AuthUser (
    val userId: String? = null,
    val email: String? = null,
    val name: String? = null,
    val doubts_asked: Long? = null,
    val doubts_solved: Long? = null,
)
