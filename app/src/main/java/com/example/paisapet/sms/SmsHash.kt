package com.example.paisapet.sms

import java.security.MessageDigest

/**
 * Lowercase hex SHA-256 of sender + "\n" + body (UTF-8). Stored under a unique index,
 * so this format must never change once testers have installed the app.
 */
fun smsHash(sender: String, body: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest("$sender\n$body".toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
}
