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
 * @param side
 * @param size
 * @param price
 * @param type
 * @param createdAt
 * @param createdAtHeight
 */
@Serializable
data class IndexerTradeResponseObject(

    val id: kotlin.String,
    val side: IndexerOrderSide,
    val size: kotlin.String,
    val price: kotlin.String,
    val type: IndexerTradeType,
    val createdAt: IndexerIsoString,
    val createdAtHeight: kotlin.String
)