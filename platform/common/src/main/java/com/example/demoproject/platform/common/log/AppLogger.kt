package com.example.demoproject.platform.common.log

import android.util.Log

/** Centralized logging facade (no PII: tokens, phones, emails). */
object AppLogger {
    private const val PREFIX = "DemoProject"

    fun d(tag: String, message: String) {
        Log.d("$PREFIX/$tag", message)
    }

    fun i(tag: String, message: String) {
        Log.i("$PREFIX/$tag", message)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        Log.w("$PREFIX/$tag", message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e("$PREFIX/$tag", message, throwable)
    }
}
