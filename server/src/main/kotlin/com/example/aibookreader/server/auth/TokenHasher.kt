package com.example.aibookreader.server.auth

import java.security.MessageDigest

object TokenHasher {

    fun sha256Hex(input: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input)
        return digest.joinToString("") { b -> "%02x".format(b) }
    }
}
