package com.lzacademy.app.components

/**
 * How a subscription of [total] sessions is split into ring segments:
 * [filled] sessions already consumed, [empty] sessions left.
 */
data class RingSegments(
    val total: Int,
    val filled: Int,
    val empty: Int,
    val hasSegments: Boolean,
)

/** Above this many sessions the arcs become unreadable, so the ring is capped. */
private const val MAX_RING_SEGMENTS = 12

/**
 * Turns a subscription counter into ring segments. An unknown pack ([total] <= 0) yields
 * no segment at all, so the plain avatar ring is kept — never a made-up progress.
 */
fun ringSegments(remaining: Int, total: Int): RingSegments {
    if (total <= 0) return RingSegments(total = 0, filled = 0, empty = 0, hasSegments = false)
    val cappedTotal = total.coerceAtMost(MAX_RING_SEGMENTS)
    // A counter larger than its pack is shown as "nothing consumed left": a full ring.
    val usableRemaining = remaining.coerceIn(0, cappedTotal)
    val filled = cappedTotal - usableRemaining
    return RingSegments(
        total = cappedTotal,
        filled = filled,
        empty = cappedTotal - filled,
        hasSegments = true,
    )
}
