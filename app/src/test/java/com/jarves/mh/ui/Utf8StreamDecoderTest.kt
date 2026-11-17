package com.jarves.mh.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class Utf8StreamDecoderTest {
    // A mix of 1-, 2-, 3- and 4-byte UTF-8 characters plus an ASCII tail.
    private val sample = "aé中🙂b plain ascii tail"

    @Test
    fun reassemblesMultibyteCharsAcrossEveryChunkBoundary() {
        val bytes = sample.encodeToByteArray()
        for (chunkSize in 1..bytes.size) {
            val decoder = Utf8StreamDecoder()
            val out = StringBuilder()
            var i = 0
            while (i < bytes.size) {
                val end = minOf(i + chunkSize, bytes.size)
                val chunk = bytes.copyOfRange(i, end)
                out.append(decoder.decode(chunk, chunk.size))
                i = end
            }
            out.append(decoder.flush())
            assertEquals("chunkSize=$chunkSize", sample, out.toString())
        }
    }

    @Test
    fun holdsBackAPartialSequenceAndEmitsItWhenCompleted() {
        val bytes = "中".encodeToByteArray() // 3 bytes: E4 B8 AD
        val decoder = Utf8StreamDecoder()
        assertEquals("", decoder.decode(bytes.copyOfRange(0, 2), 2)) // incomplete -> nothing yet
        assertEquals("中", decoder.decode(bytes.copyOfRange(2, 3), 1)) // completed
    }

    @Test
    fun flushIsEmptyWhenNothingPending() {
        assertEquals("", Utf8StreamDecoder().flush())
    }

    @Test
    fun completePrefixLengthExcludesTrailingPartialSequence() {
        // "a中" = 1 + 3 bytes; truncated to 3 bytes -> only the leading 'a' is complete.
        assertEquals(1, completeUtf8PrefixLength("a中".encodeToByteArray().copyOfRange(0, 3)))
        // All ASCII -> every byte is complete.
        assertEquals(3, completeUtf8PrefixLength("abc".encodeToByteArray()))
    }
}
