package exchange.dydx.abacus.processor.markets

import exchange.dydx.abacus.processor.base.BaseProcessor
import exchange.dydx.abacus.protocols.ParserProtocol
import exchange.dydx.abacus.state.internalstate.InternalStatePerpetualMarket
import exchange.dydx.abacus.state.internalstate.InternalStatePerpetualMarkets
import exchange.dydx.abacus.utils.Logger
import exchange.dydx.abacus.utils.mutable
import exchange.dydx.abacus.utils.safeSet
import indexer.codegen.IndexerPerpetualMarketStatus
import indexer.codegen.IndexerPerpetualMarketType

@Suppress("UNCHECKED_CAST")
internal class MarketsProcessor(parser: ParserProtocol, calculateSparklines: Boolean) :
    BaseProcessor(parser) {
    private val marketProcessor = MarketProcessor(parser, calculateSparklines)

    internal var groupingMultiplier: Int
        get() = marketProcessor.groupingMultiplier
        set(value) {
            marketProcessor.groupingMultiplier = value
        }

    internal fun subscribed(
        existing: Map<String, Any>?,
        content: Map<String, Any>
    ): Map<String, Any>? {
        val payload = parser.asNativeMap(content["markets"])
        return if (payload != null) {
            received(existing, payload)
        } else {
            existing
        }
    }

    internal fun testSubscribed(content: Map<String, Any>): InternalStatePerpetualMarkets {
        val payload = parser.asNativeMap(content["markets"])
        return if (payload != null) {
            val markets = mutableMapOf<String, InternalStatePerpetualMarket>()
            for ((market, data) in payload) {
                val marketPayload = parser.asNativeMap(data)
                if (marketPayload != null) {
                    val clobPairId = parser.asInt(marketPayload["clobPairId"]) ?: error("clobPairId is null")
                    val ticker = parser.asString(marketPayload["ticker"]) ?: error("ticker is null")
                    val status = IndexerPerpetualMarketStatus.valueOf(parser.asString(marketPayload["status"]) ?: error("status is null"))
                    val oraclePrice = parser.asDouble(marketPayload["oraclePrice"]) ?: error("oraclePrice is null")
                    val priceChange24H = parser.asDouble(marketPayload["priceChange24H"]) ?: error("priceChange24H is null")
                    val volume24H = parser.asDouble(marketPayload["volume24H"]) ?: error("volume24H is null")
                    val trades24H = parser.asInt(marketPayload["trades24H"]) ?: error("trades24H is null")
                    val nextFundingRate = parser.asDouble(marketPayload["nextFundingRate"]) ?: error("nextFundingRate is null")
                    val initialMarginFraction = parser.asDouble(marketPayload["initialMarginFraction"]) ?: error("initialMarginFraction is null")
                    val maintenanceMarginFraction = parser.asDouble(marketPayload["maintenanceMarginFraction"]) ?: error("maintenanceMarginFraction is null")
                    val openInterest = parser.asDouble(marketPayload["openInterest"]) ?: error("openInterest is null")
                    val atomicResolution = parser.asInt(marketPayload["atomicResolution"]) ?: error("atomicResolution is null")
                    val quantumConversionExponent = parser.asInt(marketPayload["quantumConversionExponent"]) ?: error("quantumConversionExponent is null")
                    val tickSize = parser.asDouble(marketPayload["tickSize"]) ?: error("tickSize is null")
                    val stepSize = parser.asDouble(marketPayload["stepSize"]) ?: error("stepSize is null")
                    val stepBaseQuantums = parser.asInt(marketPayload["stepBaseQuantums"]) ?: error("stepBaseQuantums is null")
                    val subticksPerTick = parser.asInt(marketPayload["subticksPerTick"]) ?: error("subticksPerTick is null")
                    val marketType = IndexerPerpetualMarketType.valueOf(parser.asString(marketPayload["marketType"]) ?: error("marketType is null"))
                    val openInterestLowerCap = parser.asDouble(marketPayload["openInterestLowerCap"]) ?: error("openInterestLowerCap is null")
                    val openInterestUpperCap = parser.asDouble(marketPayload["openInterestUpperCap"]) ?: error("openInterestUpperCap is null")
                    val baseOpenInterest = parser.asDouble(marketPayload["baseOpenInterest"]) ?: error("baseOpenInterest is null")

                    val receivedMarket = InternalStatePerpetualMarket(
                        clobPairId = clobPairId,
                        ticker = ticker,
                        status = status,
                        oraclePrice = oraclePrice,
                        priceChange24H = priceChange24H,
                        volume24H = volume24H,
                        trades24H = trades24H,
                        nextFundingRate = nextFundingRate,
                        initialMarginFraction = initialMarginFraction,
                        maintenanceMarginFraction = maintenanceMarginFraction,
                        openInterest = openInterest,
                        atomicResolution = atomicResolution,
                        quantumConversionExponent = quantumConversionExponent,
                        tickSize = tickSize,
                        stepSize = stepSize,
                        stepBaseQuantums = stepBaseQuantums,
                        subticksPerTick = subticksPerTick,
                        marketType = marketType,
                        openInterestLowerCap = openInterestLowerCap,
                        openInterestUpperCap = openInterestUpperCap,
                        baseOpenInterest = baseOpenInterest,
                    )
                    markets[market] = receivedMarket
                }
            }
            InternalStatePerpetualMarkets(
                markets = markets
            )
        } else {
            InternalStatePerpetualMarkets()
        }
    }

    @Suppress("FunctionName")
    internal fun channel_data(
        existing: Map<String, Any>?,
        content: Map<String, Any>
    ): Map<String, Any> {
        return receivedChanges(existing, content)
    }

    @Suppress("FunctionName")
    internal fun channel_batch_data(
        existing: Map<String, Any>?,
        content: List<Any>
    ): Map<String, Any> {
        var data = existing ?: mapOf()
        for (partialPayload in content) {
            parser.asNativeMap(partialPayload)?.let {
                data = receivedChanges(data, it)
            }
        }
        return data
    }

    override fun received(
        existing: Map<String, Any>?,
        payload: Map<String, Any>
    ): Map<String, Any> {
        val markets = existing?.mutable() ?: mutableMapOf()
        for ((market, data) in payload) {
            val marketPayload = parser.asNativeMap(data)
            if (marketPayload != null) {
                val receivedMarket = marketProcessor.received(
                    parser.asNativeMap(existing?.get(market)),
                    marketPayload,
                )
                markets[market] = receivedMarket
            }
        }
        return markets
    }

    private fun receivedChanges(
        existing: Map<String, Any>?,
        payload: Map<String, Any>
    ): Map<String, Any> {
        val markets = existing?.mutable() ?: mutableMapOf<String, Any>()
        val narrowedPayload = narrow(payload)
        for ((market, data) in narrowedPayload) {
            val marketPayload = parser.asNativeMap(data)
            if (marketPayload != null) {
                val receivedMarket = marketProcessor.receivedDelta(
                    parser.asNativeMap(existing?.get(market)),
                    marketPayload,
                )
                markets[market] = receivedMarket
            }
        }
        return markets
    }

    private fun narrow(payload: Map<String, Any>): Map<String, Any> {
        return parser.asNativeMap(payload["trading"]) ?: parser.asNativeMap(payload["oraclePrices"])
            ?: parser.asNativeMap("markets") ?: payload
    }

    internal fun receivedConfigurations(
        existing: Map<String, Any>?,
        payload: Map<String, Any>
    ): Map<String, Any> {
        val markets = existing?.mutable() ?: mutableMapOf<String, Any>()
        for ((market, data) in payload) {
            val marketPayload = parser.asNativeMap(data)
            if (marketPayload == null) {
                Logger.d { "Market payload is null" }
            } else {
                val receivedMarket = marketProcessor.receivedConfigurations(
                    parser.asNativeMap(existing?.get(market)),
                    marketPayload,
                )
                markets[market] = receivedMarket
            }
        }
        return markets
    }

    internal fun receivedOrderbook(
        existing: Map<String, Any>?,
        market: String,
        payload: Map<String, Any>
    ): Map<String, Any> {
        val marketData = parser.asNativeMap(existing?.get(market)) ?: mutableMapOf()
        val markets = existing?.mutable() ?: mutableMapOf()
        markets[market] = marketProcessor.receivedOrderbook(marketData, payload)
        return markets
    }

    internal fun receivedBatchOrderbookChanges(
        existing: Map<String, Any>?,
        market: String,
        payload: List<Any>
    ): Map<String, Any>? {
        val marketData = parser.asNativeMap(existing?.get(market))
        return if (existing != null && marketData != null) {
            val markets = existing.mutable()
            markets[market] = marketProcessor.receivedBatchOrderbookChanges(marketData, payload)
            markets
        } else {
            existing
        }
    }

    internal fun receivedTrades(
        existing: Map<String, Any>?,
        market: String,
        payload: Map<String, Any>
    ): Map<String, Any>? {
        val marketData = parser.asNativeMap(existing?.get(market)) ?: mutableMapOf()
        val markets = existing?.mutable() ?: mutableMapOf()
        markets[market] = marketProcessor.receivedTrades(marketData, payload)
        return markets
    }

    internal fun receivedTradesChanges(
        existing: Map<String, Any>?,
        market: String,
        payload: Map<String, Any>
    ): Map<String, Any>? {
        val marketData = parser.asNativeMap(existing?.get(market))
        return if (existing != null && marketData != null) {
            val markets = existing.mutable()
            markets[market] = marketProcessor.receivedTradesChanges(marketData, payload)
            markets
        } else {
            existing
        }
    }

    internal fun receivedBatchedTradesChanges(
        existing: Map<String, Any>?,
        market: String,
        payload: List<Any>
    ): Map<String, Any>? {
        val marketData = parser.asNativeMap(existing?.get(market))
        return if (existing != null && marketData != null) {
            val markets = existing.mutable()
            markets[market] = marketProcessor.receivedBatchedTradesChanges(marketData, payload)
            markets
        } else {
            existing
        }
    }

    internal fun receivedCandles(
        existing: Map<String, Any>?,
        payload: Map<String, Any>
    ): Map<String, Any>? {
        payload["candles"]?.let { candles ->
            parser.asNativeMap(candles)?.let {
                val modified = existing?.mutable() ?: mutableMapOf<String, Any>()
                for ((key, itemData) in it) {
                    parser.asString(key)?.let { market ->
                        parser.asNativeMap(existing?.get(market))?.let { marketData ->
                            parser.asNativeList(itemData)?.let { list ->
                                modified[market] = marketProcessor.receivedCandles(marketData, list)
                            }
                        }
                    }
                }
                return modified
            }
            parser.asNativeList(candles)?.let { list ->
                parser.asNativeMap(list.firstOrNull())?.let { first ->
                    val market =
                        parser.asString(first["market"]) ?: parser.asString(first["ticker"])
                    if (market != null) {
                        val modified = existing?.mutable() ?: mutableMapOf<String, Any>()

                        parser.asNativeMap(existing?.get(market))?.let { marketData ->
                            modified[market] = marketProcessor.receivedCandles(marketData, list)
                        }
                        return modified
                    }
                }
            }
        }
        return existing
    }

    internal fun receivedSparklines(
        existing: Map<String, Any>?,
        payload: Map<String, Any>
    ): Map<String, Any> {
        val modified = existing?.mutable() ?: mutableMapOf<String, Any>()
        for ((key, itemData) in payload) {
            parser.asString(key)?.let { market ->
                parser.asNativeMap(existing?.get(market))?.let { marketData ->
                    parser.asNativeList(itemData)?.let { list ->
                        modified[market] = marketProcessor.receivedSparklines(marketData, list)
                    }
                }
            }
        }
        return modified
    }

    internal fun receivedCandles(
        existing: Map<String, Any>?,
        market: String,
        resolution: String,
        payload: Map<String, Any>
    ): Map<String, Any>? {
        val marketData = parser.asNativeMap(existing?.get(market))
        return if (existing != null && marketData != null) {
            val markets = existing.mutable()
            markets[market] = marketProcessor.receivedCandles(marketData, resolution, payload)
            markets
        } else {
            existing
        }
    }

    internal fun receivedCandlesChanges(
        existing: Map<String, Any>?,
        market: String,
        resolution: String,
        payload: Map<String, Any>
    ): Map<String, Any>? {
        val marketData = parser.asNativeMap(existing?.get(market))
        return if (existing != null && marketData != null) {
            val markets = existing.mutable()
            markets[market] =
                marketProcessor.receivedCandlesChanges(marketData, resolution, payload)
            markets
        } else {
            existing
        }
    }

    internal fun receivedBatchedCandlesChanges(
        existing: Map<String, Any>?,
        market: String,
        resolution: String,
        payload: List<Any>
    ): Map<String, Any>? {
        val marketData = parser.asNativeMap(existing?.get(market))
        return if (existing != null && marketData != null) {
            val markets = existing.mutable()
            markets[market] =
                marketProcessor.receivedBatchedCandlesChanges(marketData, resolution, payload)
            markets
        } else {
            existing
        }
    }

    internal fun receivedHistoricalFundings(
        existing: Map<String, Any>?,
        payload: Map<String, Any>
    ): Map<String, Any>? {
        val market = parser.asString(
            parser.value(payload, "historicalFunding.0.market") ?: parser.value(
                payload,
                "historicalFunding.0.ticker",
            ),
        )
        if (market != null) {
            val marketData = parser.asNativeMap(existing?.get(market))
            if (existing != null && marketData != null) {
                val markets = existing.mutable()
                markets[market] = marketProcessor.receivedHistoricalFundings(marketData, payload)
                return markets
            }
        }
        return existing
    }

    internal fun groupOrderbook(existing: Map<String, Any>?, market: String?): Map<String, Any>? {
        return if (existing != null) {
            val modified = existing.mutable()
            if (market != null) {
                val existingMarket = parser.asNativeMap(existing[market])
                modified.safeSet(market, marketProcessor.groupOrderbook(existingMarket))
            } else {
                for ((key, value) in existing) {
                    val existingMarket = parser.asNativeMap(value)
                    modified.safeSet(key, marketProcessor.groupOrderbook(existingMarket))
                }
            }
            modified
        } else {
            null
        }
    }
}
