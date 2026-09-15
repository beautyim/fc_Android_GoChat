package com.example.demoproject.ui.designsystem.gift

import android.content.Context
import com.opensource.svgaplayer.SVGAParser
import com.opensource.svgaplayer.SVGAVideoEntity
import java.net.URL
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

private typealias Waiter = (SVGAVideoEntity?) -> Unit

/**
 * Warms gift SVGA so fullscreen play starts without a download wait, and keeps decoded entities
 * around so replays are instant.
 *
 * The memory cache is bounded because every entity holds the gift's decoded bitmaps. Beyond it,
 * [SVGAParser] keeps the downloaded archive in its own disk cache, so an evicted gift still
 * replays without hitting the network.
 */
object GiftSvgaPreloader {
    private const val MAX_CACHED_ENTITIES = 6

    /** Access-ordered under its own lock: newest use last, oldest evicted first. */
    private val cache = LinkedHashMap<String, SVGAVideoEntity>()

    /** Decodes in progress with their waiters, so a tap joins a running prefetch. */
    private val inFlight = HashMap<String, MutableList<Waiter>>()

    @Volatile
    private var sharedParser: SVGAParser? = null

    /** Fire-and-forget warm-up; safe to call repeatedly for the same [url]. */
    fun prefetch(context: Context, url: String?) {
        val key = url?.trim().orEmpty()
        if (key.isBlank() || cached(key) != null) return
        request(context, key, waiter = null)
    }

    internal fun cached(url: String): SVGAVideoEntity? {
        val key = url.trim()
        return synchronized(cache) {
            cache.remove(key)?.also { cache[key] = it }
        }
    }

    /** Suspends until the gift is decoded; returns null when it can't be played. */
    internal suspend fun load(context: Context, url: String): SVGAVideoEntity? {
        val key = url.trim()
        if (key.isBlank()) return null
        cached(key)?.let { return it }
        return suspendCancellableCoroutine { continuation ->
            val waiter: Waiter = { entity ->
                if (continuation.isActive) continuation.resume(entity)
            }
            request(context, key, waiter)
            // Let the decode run on: it still fills the cache for the next tap.
            continuation.invokeOnCancellation { dropWaiter(key, waiter) }
        }
    }

    private fun request(context: Context, key: String, waiter: Waiter?) {
        synchronized(inFlight) {
            val waiters = inFlight[key]
            if (waiters != null) {
                if (waiter != null) waiters.add(waiter)
                return
            }
            inFlight[key] = mutableListOf<Waiter>().also { list ->
                if (waiter != null) list.add(waiter)
            }
        }
        decode(context, key) { entity ->
            val waiters = synchronized(inFlight) { inFlight.remove(key) }.orEmpty()
            waiters.forEach { it(entity) }
        }
    }

    private fun dropWaiter(key: String, waiter: Waiter) {
        synchronized(inFlight) { inFlight[key]?.remove(waiter) }
    }

    private fun decode(context: Context, key: String, onResult: Waiter) {
        val remote = runCatching { URL(key) }.getOrNull()
        if (remote == null) {
            onResult(null)
            return
        }
        parser(context).decodeFromURL(
            remote,
            object : SVGAParser.ParseCompletion {
                override fun onComplete(videoItem: SVGAVideoEntity) {
                    keep(key, videoItem)
                    onResult(videoItem)
                }

                override fun onError() = onResult(null)
            },
        )
    }

    private fun keep(key: String, entity: SVGAVideoEntity) {
        synchronized(cache) {
            cache.remove(key)
            cache[key] = entity
            while (cache.size > MAX_CACHED_ENTITIES) {
                cache.remove(cache.keys.first())
            }
        }
    }

    /** One parser for the whole app: it owns the shared decode thread pool and disk cache. */
    private fun parser(context: Context): SVGAParser =
        sharedParser ?: synchronized(this) {
            sharedParser ?: SVGAParser(context.applicationContext).also { sharedParser = it }
        }
}
