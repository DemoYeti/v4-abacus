package exchange.dydx.abacus.state.internalState

internal data class InternalState(
    val transfer: InternalTransferInputState = InternalTransferInputState(),
    val perpetualMarkets: InternalStatePerpetualMarkets = InternalStatePerpetualMarkets(),
)
