package exchange.dydx.abacus.state.internalState

internal data class InternalState(
    val transfer: InternalTransferInputState = InternalTransferInputState(),
    val markets: InternalStatePerpetualMarkets = InternalStatePerpetualMarkets(),
) {
    fun setMarkets(markets: InternalStatePerpetualMarkets): InternalState {
        return this.copy(markets = markets)
    }
}
