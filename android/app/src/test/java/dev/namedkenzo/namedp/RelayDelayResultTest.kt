package dev.namedkenzo.namedp

import org.junit.Assert.*
import org.junit.Test

class RelayDelayResultTest {
    @Test fun verified204HasDelay() {
        assertEquals(231L, RelayDelayResult.verified("relay_delay", RelayDelayResult.URL, 204, 231).milliseconds!!)
    }
    @Test fun handshakeWrongUrlStatusAndInvalidTimingCannotFakeDelay() {
        val cases = listOf(
            listOf("probe_ok", RelayDelayResult.URL, "204", "1"),
            listOf("relay_delay", "https://example.com", "204", "1"),
            listOf("relay_delay", RelayDelayResult.URL, "200", "1"),
            listOf("relay_delay", RelayDelayResult.URL, "204", "-1"),
            listOf("relay_delay", RelayDelayResult.URL, "204", "0"),
            listOf("relay_delay", RelayDelayResult.URL, "204", "15001")
        )
        for (c in cases) {
            try { RelayDelayResult.verified(c[0], c[1], c[2].toInt(), c[3].toLong()); fail("Invalid result accepted") }
            catch (_: IllegalArgumentException) { }
        }
    }
}
