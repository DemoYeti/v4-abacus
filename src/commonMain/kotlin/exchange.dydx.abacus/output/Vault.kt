package exchange.dydx.abacus.output

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import exchange.dydx.abacus.responses.ParsingError
import exchange.dydx.abacus.utils.IList
import kollections.JsExport
import kotlinx.serialization.Serializable

@JsExport
@Serializable
data class Vault(
    val totalValue: String,
    val thirtyDayReturnPercent: Double,
    val pnlPoints: IList<VaultHistoricalPNL>,
    val positions: IList<VaultPosition>,
)

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
