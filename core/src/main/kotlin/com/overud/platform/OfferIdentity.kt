package com.overud.platform

import com.overud.model.Platform
import java.security.MessageDigest

fun fingerprint(platform: Platform, text: String): String = MessageDigest.getInstance("SHA-256")
    .digest((platform.name + text.lowercase().replace(Regex("\\s+"), " ").trim()).toByteArray())
    .take(12).joinToString("") { "%02x".format(it) }
