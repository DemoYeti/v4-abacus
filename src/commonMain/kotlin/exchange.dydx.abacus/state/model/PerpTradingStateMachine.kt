package exchange.dydx.abacus.state.model

import exchange.dydx.abacus.protocols.LocalizerProtocol
import exchange.dydx.abacus.state.app.helper.Formatter
import exchange.dydx.abacus.state.manager.V4Environment

class PerpTradingStateMachine(
    environment: V4Environment?,
    localizer: LocalizerProtocol?,
    formatter: Formatter?,
    maxSubaccountNumber: Int,
    useParentSubaccount: Boolean,
    useSkipProcessor: Boolean,
) :
    TradingStateMachine(environment, localizer, formatter, maxSubaccountNumber, useParentSubaccount, useSkipProcessor=useSkipProcessor) {
    /*
    Placeholder for now. Eventually, the code specifically for Perpetual will be in this class
     */
}
