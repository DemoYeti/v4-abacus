package exchange.dydx.abacus.validator.trade

import exchange.dydx.abacus.calculator.MarginModeCalculator
import exchange.dydx.abacus.protocols.LocalizerProtocol
import exchange.dydx.abacus.protocols.ParserProtocol
import exchange.dydx.abacus.state.app.helper.Formatter
import exchange.dydx.abacus.state.manager.V4Environment
import exchange.dydx.abacus.validator.BaseInputValidator
import exchange.dydx.abacus.validator.PositionChange
import exchange.dydx.abacus.validator.TradeValidatorProtocol

internal class TradeIsolatedMarginValidator(
    localizer: LocalizerProtocol?,
    formatter: Formatter?,
    parser: ParserProtocol,
) : BaseInputValidator(localizer, formatter, parser), TradeValidatorProtocol {
    override fun validateTrade(
        account: Map<String, Any>?,
        subaccount: Map<String, Any>?,
        market: Map<String, Any>?,
        configs: Map<String, Any>?,
        trade: Map<String, Any>,
        change: PositionChange,
        restricted: Boolean,
        environment: V4Environment?
    ): List<Any>? {
        val childSubaccountNumber = MarginModeCalculator.getChildSubaccountNumberForIsolatedMarginTrade(
            parser,
            account,
            parser.asInt(subaccount?.get("subaccountNumber")) ?: 0,
            parser.asString(trade["marketId"])
        )
        val subaccount = parser.asMap(parser.value(account,"subaccounts.$childSubaccountNumber"))
        if (subaccount != null) {

            val isolatedMargin = parser.asDouble(parser.value(trade, "isolatedMargin"))
            if (isolatedMargin == null || isolatedMargin < 0) {
            }
        }
        return null
    }
}