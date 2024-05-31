package exchange.dydx.abacus.processor.router.skip

import exchange.dydx.abacus.processor.base.BaseProcessor
import exchange.dydx.abacus.protocols.ParserProtocol
import exchange.dydx.abacus.utils.safeSet
import kotlin.math.pow

internal class SkipRouteProcessor(internal val parser: ParserProtocol) {
    private val keyMap = mapOf(
        "string" to mapOf(
            "route.usd_amount_out" to "toAmountUSD",
            "route.estimated_amount_out" to "toAmount",
            "swap_price_impact_percent" to "aggregatePriceImpact",
//            [Q] we probably need to handle this separately. should return a 4XX error
//            "errors" to "errors",
            "message" to "errorMessage",

//            SQUID PARAMS THAT ARE NOW DEPRECATED:
//            "route.estimate.gasCosts.0.amountUSD" to "gasFee",
//            "route.estimate.exchangeRate" to "exchangeRate",
//            "route.estimate.estimatedRouteDuration" to "estimatedRouteDuration",
//            "route.estimate.toAmountMin" to "toAmountMin",

        ),
    )

    private fun findFee(payload: Map<String, Any>, key: String): Double? {
        val estimatedFees = parser.asList(parser.value(payload, "route.estimated_fees"))
        val foundFeeObj = estimatedFees?.find { it ->
            parser.asString(parser.asNativeMap(it)?.get("fee_type")) == key
        }
        val feeInUSD = parser.asDouble(parser.asNativeMap(foundFeeObj)?.get("usd_amount"))
        return feeInUSD
    }

    fun received(
        existing: Map<String, Any>?,
        payload: Map<String, Any>,
        decimals: Double?
    ): Map<String, Any> {
        val modified = BaseProcessor(parser).transform(existing, payload, keyMap)

        var bridgeFees = findFee(payload, "BRIDGE")
//        TODO: update web UI to show smart relay fees
//        For now we're just bundling it with the bridge fees
        val smartRelayFees = findFee(payload, "SMART_RELAY")
        if (bridgeFees == null) {
            bridgeFees = smartRelayFees
        } else if (smartRelayFees != null) {
            bridgeFees += smartRelayFees
        }
        val gasFees = findFee(payload, "GAS")

        modified.safeSet("gasFees", gasFees)
        modified.safeSet("bridgeFees", bridgeFees)

        val toAmount = parser.asLong(parser.value(payload, "route.estimated_amount_out"))
        if (toAmount != null && decimals != null) {
            modified.safeSet("toAmount", toAmount / 10.0.pow(decimals))
        }
        val toAmountUSD = parser.asDouble(parser.value(payload, "route.usd_amount_out"))
        if (toAmountUSD != null) {
            modified.safeSet("toAmountUSD", toAmountUSD)
        }

        val payloadProcessor = SkipRoutePayloadProcessor(parser)
//        TODO: Remove slippage.
//        This is just hard coded in our params so we're keeping it to be
//        at parity for now. Fast follow squid -> skip migration project to removing max slippage
//        because we already show the actual slippage.
        modified.safeSet("slippage", "1")
        modified.safeSet("requestPayload", payloadProcessor.received(null, payload))
        return modified
    }
}
