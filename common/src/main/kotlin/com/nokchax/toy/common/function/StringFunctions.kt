package com.nokchax.toy.common.function

import java.security.MessageDigest

fun String.sha256(): String =
    MessageDigest.getInstance("SHA-256")
        .digest(toByteArray())
        .joinToString("") { "%02x".format(it) }

fun String.ellipsis(max: Int): String = if (length <= max) this else take(max - 1) + "…"
