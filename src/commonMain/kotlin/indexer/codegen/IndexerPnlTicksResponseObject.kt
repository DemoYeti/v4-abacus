/**
 * Indexer API
 * No description provided (generated by Swagger Codegen https://github.com/swagger-api/swagger-codegen)
 *
 * OpenAPI spec version: v1.0.0
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */
package indexer.codegen

import kotlinx.serialization.Serializable

/**
 *
 * @param id
 * @param subaccountId
 * @param equity
 * @param totalPnl
 * @param netTransfers
 * @param createdAt
 * @param blockHeight
 * @param blockTime
 */
@Serializable
data class IndexerPnlTicksResponseObject(

    val id: kotlin.String,
    val subaccountId: kotlin.String,
    val equity: kotlin.String,
    val totalPnl: kotlin.String,
    val netTransfers: kotlin.String,
    val createdAt: kotlin.String,
    val blockHeight: kotlin.String,
    val blockTime: IndexerIsoString
)