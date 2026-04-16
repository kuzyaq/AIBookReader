package com.example.aibookreader.server.auth

import at.favre.lib.crypto.bcrypt.BCrypt

object PasswordHasher {

    private const val COST = 12

    fun hash(plain: String): String =
        BCrypt.withDefaults().hashToString(COST, plain.toCharArray())

    fun verify(plain: String, hash: String): Boolean =
        BCrypt.verifyer().verify(plain.toCharArray(), hash).verified
}
