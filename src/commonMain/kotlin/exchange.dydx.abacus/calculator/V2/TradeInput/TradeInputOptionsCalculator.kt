package exchange.dydx.abacus.calculator.v2.TradeInput

import exchange.dydx.abacus.calculator.CalculationPeriod
import exchange.dydx.abacus.calculator.MarginCalculator
import exchange.dydx.abacus.output.input.MarginMode
import exchange.dydx.abacus.output.input.OrderType
import exchange.dydx.abacus.output.input.SelectionOption
import exchange.dydx.abacus.output.input.Tooltip
import exchange.dydx.abacus.output.input.TradeInputGoodUntil
import exchange.dydx.abacus.protocols.ParserProtocol
import exchange.dydx.abacus.state.internalstate.InternalAccountState
import exchange.dydx.abacus.state.internalstate.InternalMarketState
import exchange.dydx.abacus.state.internalstate.InternalPerpetualPosition
import exchange.dydx.abacus.state.internalstate.InternalSubaccountState
import exchange.dydx.abacus.state.internalstate.InternalTradeInputOptions
import exchange.dydx.abacus.state.internalstate.InternalTradeInputState
import exchange.dydx.abacus.state.internalstate.safeCreate

internal class TradeInputOptionsCalculator(
    private val parser: ParserProtocol,
) {
    fun calculate(
        trade: InternalTradeInputState,
        account: InternalAccountState,
        subaccount: InternalSubaccountState?,
        market: InternalMarketState?,
        position: InternalPerpetualPosition?,
    ): InternalTradeInputState {
        trade.options = calculatedOptions(
            account = account,
            subaccount = subaccount,
            trade = trade,
            position = position,
            market = market,
        )
        applyDefaultOptions(
            trade = trade,
            account = account,
            subaccount = subaccount,
            position = position,
            market = market,
        )

        return trade
    }

    private fun calculatedOptions(
        account: InternalAccountState,
        subaccount: InternalSubaccountState?,
        trade: InternalTradeInputState,
        position: InternalPerpetualPosition?,
        market: InternalMarketState?,
    ): InternalTradeInputOptions {
        val fields = requiredFields(
            trade = trade,
            account = account,
            subaccount = subaccount,
            market = market,
        )

        return calculatedOptionsFromFields(
            fields = fields,
            trade = trade,
            position = position,
            market = market,
        )
    }

    private fun requiredFields(
        trade: InternalTradeInputState,
        account: InternalAccountState,
        subaccount: InternalSubaccountState?,
        market: InternalMarketState?,
    ): List<Any>? {
        return when (trade.type) {
            OrderType.Market -> {
                return when (trade.marginMode) {
                    MarginMode.Isolated -> listOf(
                        sizeField(),
                        bracketsField(),
                        marginModeField(market, account, subaccount),
                        reduceOnlyField(),
                    ).filterNotNull()

                    else -> listOf(
                        sizeField(),
                        leverageField(),
                        bracketsField(),
                        marginModeField(market, account, subaccount),
                        reduceOnlyField(),
                    ).filterNotNull()
                }
            }

            OrderType.Limit -> {
                when (trade.timeInForce) {
                    "GTT" ->
                        listOf(
                            sizeField(),
                            limitPriceField(),
                            timeInForceField(),
                            goodTilField(),
                            postOnlyField(),
                            marginModeField(market, account, subaccount),
                        ).filterNotNull()

                    else ->
                        listOf(
                            sizeField(),
                            limitPriceField(),
                            timeInForceField(),
                            marginModeField(market, account, subaccount),
                            reduceOnlyField(),
                        ).filterNotNull()
                }
            }

            OrderType.StopLimit, OrderType.TakeProfitLimit -> {
                val execution = trade.execution
                listOf(
                    sizeField(),
                    limitPriceField(),
                    triggerPriceField(),
                    goodTilField(),
                    executionField(true),
                    marginModeField(market, account, subaccount),
                    when (execution) {
                        "IOC" -> reduceOnlyField()
                        else -> null
                    },
                ).filterNotNull()
            }

            OrderType.StopMarket, OrderType.TakeProfitMarket -> {
                listOf(
                    sizeField(),
                    triggerPriceField(),
                    goodTilField(),
                    executionField(false),
                    marginModeField(market, account, subaccount),
                    reduceOnlyField(),
                ).filterNotNull()
            }

            OrderType.TrailingStop -> {
                listOf(
                    sizeField(),
                    trailingPercentField(),
                    goodTilField(),
                    executionField(false),
                    marginModeField(market, account, subaccount),
                ).filterNotNull()
            }

            OrderType.Liquidated,
            OrderType.Liquidation,
            OrderType.Offsetting,
            OrderType.Deleveraged,
            OrderType.FinalSettlement,
            null -> null
        }
    }

    private fun calculatedOptionsFromFields(
        fields: List<Any>?,
        trade: InternalTradeInputState,
        position: InternalPerpetualPosition?,
        market: InternalMarketState?,
    ): InternalTradeInputOptions {
        if (fields == null) {
            return trade.options
        }
        val options = InternalTradeInputOptions()
        for (item in fields) {
            parser.asNativeMap(item)?.let { field ->
                when (parser.asString(field["field"])) {
                    "size.size" -> options.needsSize = true
                    "size.leverage" -> options.needsLeverage = true
                    "price.triggerPrice" -> options.needsTriggerPrice = true
                    "price.limitPrice" -> options.needsLimitPrice = true
                    "price.trailingPercent" -> options.needsTrailingPercent = true
                    "timeInForce" -> {
                        options.timeInForceOptions = field["options"] as? List<SelectionOption>
                        //      options.needsTimeInForce = true
                    }

                    "goodTil" -> {
                        options.goodTilUnitOptions = goodTilUnitField()["options"] as? List<SelectionOption>
                        options.needsGoodUntil = true
                    }

                    "execution" -> {
                        options.executionOptions = field["options"] as? List<SelectionOption>
                        //     options.needsExecution = true
                    }

                    "marginMode" -> {
                        options.marginModeOptions = field["options"] as? List<SelectionOption>
                        options.needsMarginMode = true
                    }

                    "reduceOnly" -> options.needsReduceOnly = true
                    "postOnly" -> options.needsPostOnly = true
                    "brackets" -> options.needsBrackets = true
                }
            }
        }

        if (options.needsLeverage) {
            options.maxLeverage = maxLeverageFromPosition(position, market)
        } else {
            options.maxLeverage = null
        }

        if (options.needsReduceOnly) {
            options.reduceOnlyTooltip = null
        } else {
            options.reduceOnlyTooltip = buildToolTip(reduceOnlyPromptFromTrade(trade.type))
        }

        if (options.needsPostOnly) {
            options.postOnlyTooltip = null
        } else {
            options.postOnlyTooltip = buildToolTip(postOnlyPromptFromTrade(trade.type))
        }

        options.needsTargetLeverage = trade.marginMode == MarginMode.Isolated

        return options
    }

    private fun applyDefaultOptions(
        trade: InternalTradeInputState,
        account: InternalAccountState,
        subaccount: InternalSubaccountState?,
        position: InternalPerpetualPosition?,
        market: InternalMarketState?,
    ): InternalTradeInputState {
        var options = calculatedOptions(
            account = account,
            subaccount = subaccount,
            trade = trade,
            position = position,
            market = market,
        )
        if (options.timeInForceOptions != null) {
            if (trade.timeInForce == null) {
                trade.timeInForce = options.timeInForceOptions?.firstOrNull()?.type
            }
        }

        options = calculatedOptions(
            account = account,
            subaccount = subaccount,
            trade = trade,
            position = position,
            market = market,
        )
        if (options.goodTilUnitOptions != null) {
            if (trade.goodTil?.unit == null) {
                val goodTil = TradeInputGoodUntil.safeCreate(trade.goodTil)
                trade.goodTil = goodTil.copy(unit = "D")
            }
        }

        options = calculatedOptions(
            account = account,
            subaccount = subaccount,
            trade = trade,
            position = position,
            market = market,
        )
        if (options.executionOptions != null) {
            if (trade.execution == null) {
                trade.execution = options.executionOptions?.firstOrNull()?.type
            }
        }

        options = calculatedOptions(
            account = account,
            subaccount = subaccount,
            trade = trade,
            position = position,
            market = market,
        )
        if (options.marginModeOptions != null) {
            if (trade.marginMode == null) {
                trade.marginMode = MarginMode.invoke(options.marginModeOptions?.firstOrNull()?.type)
            }
        }

        options = calculatedOptions(
            account = account,
            subaccount = subaccount,
            trade = trade,
            position = position,
            market = market,
        )
        if (options.needsGoodUntil) {
            if (trade.goodTil == null) {
                val goodTil = TradeInputGoodUntil.safeCreate(trade.goodTil)
                trade.goodTil = goodTil.copy(duration = 28.0)
            }
        }

        return trade
    }

    private fun buildToolTip(stringKey: String?): Tooltip? {
        return if (stringKey != null) {
            Tooltip(titleStringKey = "$stringKey.TITLE", bodyStringKey = "$stringKey.BODY")
        } else {
            null
        }
    }

    private fun reduceOnlyPromptFromTrade(
        orderType: OrderType?
    ): String? {
        return when (orderType) {
            OrderType.Limit -> "GENERAL.TRADE.REDUCE_ONLY_TIMEINFORCE_IOC"
            OrderType.StopLimit, OrderType.TakeProfitLimit -> "GENERAL.TRADE.REDUCE_ONLY_TIMEINFORCE_IOC"
            else -> return null
        }
    }

    private fun postOnlyPromptFromTrade(
        orderType: OrderType?
    ): String? {
        return when (orderType) {
            OrderType.Limit -> "GENERAL.TRADE.POST_ONLY_TIMEINFORCE_GTT"
            else -> return null
        }
    }

    private fun maxLeverageFromPosition(
        position: InternalPerpetualPosition?,
        market: InternalMarketState?,
    ): Double? {
        if (position != null) {
            return position.calculated[CalculationPeriod.current]?.maxLeverage
        } else {
            val initialMarginFraction =
                market?.perpetualMarket?.configs?.effectiveInitialMarginFraction
                    ?: return null
            return 1.0 / initialMarginFraction
        }
    }

    private fun marginModeField(
        market: InternalMarketState?,
        account: InternalAccountState,
        subaccount: InternalSubaccountState?,
    ): Map<String, Any>? {
        val selectableMarginMode = MarginCalculator.selectableMarginModes(
            account = account,
            market = market,
            subaccountNumber = subaccount?.subaccountNumber ?: 0,
        )
        return if (selectableMarginMode) {
            mapOf(
                "field" to "marginMode",
                "type" to "string",
                "options" to listOf(
                    marginModeCross,
                    marginModeIsolated,
                ),
            )
        } else {
            null
        }
    }

    private fun sizeField(): Map<String, Any> {
        return mapOf(
            "field" to "size.size",
            "type" to "double",
        )
    }

    private fun leverageField(): Map<String, Any> {
        return mapOf(
            "field" to "size.leverage",
            "type" to "double",
        )
    }

    private fun limitPriceField(): Map<String, Any> {
        return mapOf(
            "field" to "price.limitPrice",
            "type" to "double",
        )
    }

    private fun triggerPriceField(): Map<String, Any> {
        return mapOf(
            "field" to "price.triggerPrice",
            "type" to "double",
        )
    }

    private fun trailingPercentField(): Map<String, Any> {
        return mapOf("field" to "price.trailingPercent", "type" to "double")
    }

    private fun reduceOnlyField(): Map<String, Any>? {
        return mapOf(
            "field" to "reduceOnly",
            "type" to "bool",
            "default" to false,
        )
    }

    private fun postOnlyField(): Map<String, Any> {
        return mapOf(
            "field" to "postOnly",
            "type" to "bool",
            "default" to false,
        )
    }

    private fun bracketsField(): Map<String, Any> {
        return mapOf(
            "field" to "brackets",
            "type" to listOf(
                stopLossField(),
                takeProfitField(),
                goodTilField(),
                executionField(false),
            ),
        )
    }

    private fun stopLossField(): Map<String, Any> {
        return mapOf(
            "field" to "stopLoss",
            "type" to
                listOf(
                    priceField(),
                    reduceOnlyField(),
                ).filterNotNull(),
        )
    }

    private fun takeProfitField(): Map<String, Any> {
        return mapOf(
            "field" to "takeProfit",
            "type" to
                listOf(
                    priceField(),
                    reduceOnlyField(),
                ).filterNotNull(),
        )
    }

    private fun priceField(): Map<String, Any> {
        return mapOf(
            "field" to "price",
            "type" to "double",
        )
    }

    private fun timeInForceField(): Map<String, Any> {
        return mapOf(
            "field" to "timeInForce",
            "type" to "string",
            "options" to listOf(
                timeInForceOptionGTT,
                timeInForceOptionIOC,
            ),
        )
    }

    private fun goodTilField(): Map<String, Any> {
        return mapOf(
            "field" to "goodTil",
            "type" to listOf(
                goodTilDurationField(),
                goodTilUnitField(),
            ),
        )
    }

    private fun goodTilDurationField(): Map<String, Any> {
        return mapOf(
            "field" to "duration",
            "type" to "int",
        )
    }

    private fun goodTilUnitField(): Map<String, Any> {
        return mapOf(
            "field" to "unit",
            "type" to "string",
            "options" to listOf(
                goodTilUnitMinutes,
                goodTilUnitHours,
                goodTilUnitDays,
                goodTilUnitWeeks,
            ),
        )
    }

    private fun executionField(includesDefaultAndPostOnly: Boolean): Map<String, Any> {
        return mapOf(
            "field" to "execution",
            "type" to "string",
            "options" to
                if (includesDefaultAndPostOnly) {
                    listOf(
                        executionDefault,
                        executionIOC,
                        executionPostOnly,
                    )
                } else {
                    listOf(
                        executionIOC,
                    )
                },
        )
    }

    private val timeInForceOptionGTT: SelectionOption
        get() = SelectionOption(
            type = "GTT",
            stringKey = "APP.TRADE.GOOD_TIL_TIME",
            string = null,
            iconUrl = null,
        )

    private val timeInForceOptionIOC: SelectionOption
        get() = SelectionOption(
            type = "IOC",
            stringKey = "APP.TRADE.IMMEDIATE_OR_CANCEL",
            string = null,
            iconUrl = null,
        )

    private val goodTilUnitMinutes: SelectionOption
        get() = SelectionOption(
            type = "M",
            stringKey = "APP.GENERAL.TIME_STRINGS.MINUTES_SHORT",
            string = null,
            iconUrl = null,
        )

    private val goodTilUnitHours: SelectionOption
        get() = SelectionOption(
            type = "H",
            stringKey = "APP.GENERAL.TIME_STRINGS.HOURS",
            string = null,
            iconUrl = null,
        )

    private val goodTilUnitDays: SelectionOption
        get() = SelectionOption(
            type = "D",
            stringKey = "APP.GENERAL.TIME_STRINGS.DAYS",
            string = null,
            iconUrl = null,
        )

    private val goodTilUnitWeeks: SelectionOption
        get() = SelectionOption(
            type = "W",
            stringKey = "APP.GENERAL.TIME_STRINGS.WEEKS",
            string = null,
            iconUrl = null,
        )

    private val executionDefault: SelectionOption
        get() = SelectionOption(
            type = "DEFAULT",
            stringKey = "APP.TRADE.GOOD_TIL_DATE",
            string = null,
            iconUrl = null,
        )

    private val executionPostOnly: SelectionOption
        get() = SelectionOption(
            type = "POST_ONLY",
            stringKey = "APP.TRADE.POST_ONLY",
            string = null,
            iconUrl = null,
        )

    private val executionIOC: SelectionOption
        get() = SelectionOption(
            type = "IOC",
            stringKey = "APP.TRADE.IMMEDIATE_OR_CANCEL",
            string = null,
            iconUrl = null,
        )

    private val marginModeCross: SelectionOption
        get() = SelectionOption(
            type = "CROSS",
            stringKey = "APP.TRADE.CROSS_MARGIN",
            string = null,
            iconUrl = null,
        )

    private val marginModeIsolated: SelectionOption
        get() = SelectionOption(
            type = "ISOLATED",
            stringKey = "APP.TRADE.ISOLATED_MARGIN",
            string = null,
            iconUrl = null,
        )
}