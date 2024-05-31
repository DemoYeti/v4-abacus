package exchange.dydx.abacus.processor.router.skip

import exchange.dydx.abacus.tests.payloads.SkipRouteMock
import exchange.dydx.abacus.utils.Parser
import kotlin.test.Test
import kotlin.test.assertEquals

class SkipRouteProcessorTests {
    val parser = Parser()
    internal val skipRouteMock = SkipRouteMock()
    internal val skipRouteProcessor = SkipRouteProcessor(parser = parser)

    /**
     * Tests a CCTP deposit.
     */
    @Test
    fun testReceivedCCTPDeposit() {
        val payload = skipRouteMock.payload
        val result = skipRouteProcessor.received(existing = mapOf(), payload = templateToJson(payload), decimals = 6.0)
        val expected = mapOf(
            "toAmountUSD" to 11.64,
            "toAmount" to 11.64,
            "bridgeFees" to .36,
            "slippage" to "1",
            "requestPayload" to mapOf(
                "targetAddress" to "0xBC8552339dA68EB65C8b88B414B5854E0E366cFc",
                "data" to "d77d6ec00000000000000000000000000000000000000000000000000000000000b19cc000000000000000000000000000000000000000000000000000000000000000040000000000000000000000009dc5ce8a5722795f5723d32b921c53d3bb449348000000000000000000000000a0b86991c6218b36c1d19d4a2e9eb0ce3606eb480000000000000000000000000000000000000000000000000000000000057e40000000000000000000000000691cf4641d5608f085b2c1921172120bb603d074",
                "value" to "0",
                "fromChainId" to "1",
                "fromAddress" to "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48",
                "toChainId" to "noble-1",
                "toAddress" to "uusdc",
            ),
        )
        assertEquals(expected.toString(), result.toString())
    }
}
