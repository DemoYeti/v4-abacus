package exchange.dydx.abacus.processor.router

import exchange.dydx.abacus.protocols.ParserProtocol

class SharedRouterProcessor(val parser: ParserProtocol) {
    fun usdcAmount(data: Map<String, Any>): Double? {
        var toAmountUSD = parser.asString(parser.value(data, "transfer.route.toAmountUSD"))
        toAmountUSD = toAmountUSD?.replace(",", "")
        var toAmount = parser.asString(parser.value(data, "transfer.route.toAmount"))
        toAmount = toAmount?.replace(",", "")
        return parser.asDouble(toAmountUSD) ?: parser.asDouble(toAmount)
    }
}