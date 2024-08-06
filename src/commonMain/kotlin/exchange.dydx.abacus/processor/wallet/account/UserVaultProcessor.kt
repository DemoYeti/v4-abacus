package exchange.dydx.abacus.processor.wallet.account

import exchange.dydx.abacus.output.account.UnbondingDelegation
import exchange.dydx.abacus.processor.base.BaseProcessor
import exchange.dydx.abacus.protocols.ParserProtocol
import exchange.dydx.abacus.state.internalstate.InternalVaultAccount
import exchange.dydx.abacus.utils.IMutableList
import indexer.models.AccountVaultResponse
import indexer.models.chain.OnChainUnbondingResponse
import kollections.iMutableListOf

internal interface UserVaultProcessorProtocol {
    fun process(
        existing: InternalVaultAccount?,
        payload: AccountVaultResponse?,
    ): InternalVaultAccount?
}

internal class UserVaultProcessor(
    parser: ParserProtocol,
) : BaseProcessor(parser), UserVaultProcessorProtocol {

    override fun process(
        existing: InternalVaultAccount?,
        payload: AccountVaultResponse?,
    ): InternalVaultAccount? {
        if (payload == null) {
            return existing
        }
        // todo
        return existing
    }
}
