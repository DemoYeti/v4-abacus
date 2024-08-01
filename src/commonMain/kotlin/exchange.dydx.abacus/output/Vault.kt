package exchange.dydx.abacus.output

import exchange.dydx.abacus.state.internalstate.InternalVaultState
import exchange.dydx.abacus.utils.IList
import kollections.JsExport
import kollections.iListOf
import kotlinx.serialization.Serializable

@JsExport
@Serializable
data class Vault(
    val totalValue: String,
    val thirtyDayReturnPercent: Double,
    val pnlPoints: IList<VaultHistoricalPNL>,
    val positions: IList<VaultPosition>,
) {
    companion object {
        internal fun create(
            internalState: InternalVaultState?,
        ): Vault? {
            if (internalState == null) {
                return null;
            }
            val lastPosition = internalState.pnlTicks.lastOrNull()
            return Vault(
                totalValue = lastPosition?.equity ?: "0.0",
                thirtyDayReturnPercent = 0.0,
                pnlPoints = iListOf(),
                positions = iListOf(),
            )
        }
    }
}

@JsExport
@Serializable
data class VaultHistoricalPNL(
    val equity: Double,
    val totalPnl: Double,
    val createdAtMilliseconds: Double,
)

@JsExport
@Serializable
data class VaultPosition(
    val marketID: String,
    val marginUsdc: Double,
    val currentLeverageMultiple: Double,
    val positionSizeAsset: Double,
    val positionSizeUsdc: Double,
    val thirtyDayReturnPercent: Double,
    val thirtyDayReturn: Double,
    val sparklinePoints: IList<Double>,
)
