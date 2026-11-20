package com.lzacademy.app.data.sms

/**
 * Pure helpers for the admin SMS composer: phone normalisation, the multi-recipient
 * `smsto:` URI and the batching used by some Messages apps that cap the recipient list.
 *
 * No Android dependency on purpose — every rule below is pinned by a JVM unit test.
 */
object SmsComposer {

    /** Below this many digits a "phone" is a typo, not a number. */
    private const val MIN_DIGITS = 6

    /** Most Messages apps accept 20 recipients; beyond that we open one batch at a time. */
    const val DEFAULT_BATCH_SIZE = 20

    /**
     * Keeps digits only, preserves a leading `+` (international format), and returns
     * null when the result cannot be a real phone number.
     */
    fun normalizePhone(raw: String?): String? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        val hasPlus = trimmed.startsWith("+")
        val digits = trimmed.filter(Char::isDigit)
        if (digits.length < MIN_DIGITS) return null
        return if (hasPlus) "+$digits" else digits
    }

    /** `smsUri(listOf("0555123456", "+213666778899")) == "smsto:0555123456;+213666778899"`. */
    fun smsUri(phones: List<String>): String = "smsto:" + phones.joinToString(";")

    /**
     * One entry per real phone number. Two siblings sharing a guardian number are ONE
     * recipient: counting them twice would overstate who is contacted (and some Messages
     * apps silently drop the duplicate anyway).
     */
    fun uniquePhones(phones: List<String>): List<String> = phones.distinct()

    /** Splits recipients into groups of [size]; empty input → empty list. */
    fun batches(phones: List<String>, size: Int = DEFAULT_BATCH_SIZE): List<List<String>> =
        if (size <= 0) emptyList() else uniquePhones(phones).chunked(size)
}
