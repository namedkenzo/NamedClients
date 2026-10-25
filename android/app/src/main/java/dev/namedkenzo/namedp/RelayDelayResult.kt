package dev.namedkenzo.namedp

data class RelayDelayResult(val milliseconds: Long?, val failure: String? = null) {
    companion object {
        const val URL = "https://www.gstatic.com/generate_204"
        fun verified(event: String, url: String, status: Int, milliseconds: Long): RelayDelayResult {
            require(event == "relay_delay" && url == URL && status == 204 && milliseconds in 1L..15000L) {
                "Invalid relay delay response"
            }
            return RelayDelayResult(milliseconds)
        }
    }
}
