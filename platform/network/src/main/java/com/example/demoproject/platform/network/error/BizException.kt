package com.example.demoproject.platform.network.error

/**
 * Server-side business rejection (equivalent to non-zero API `code` field).
 */
class BizException(
    val code: Int,
    override val message: String?,
) : Exception(message)
