package org.example.database.dao

import org.example.database.tables.MeeplesTable
import org.example.model.Meeple
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.UUID

/**
 * Data Access Object for Meeple entity
 */
class MeepleDao(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<MeepleDao>(MeeplesTable)

    var gameId by MeeplesTable.gameId
    var playerId by MeeplesTable.playerId
    var tileId by MeeplesTable.tileId
    var featureId by MeeplesTable.featureId
    var position by MeeplesTable.position
    var placedAt by MeeplesTable.placedAt
    var returned by MeeplesTable.returned
    var returnedAt by MeeplesTable.returnedAt

    /**
     * Convert DAO to API model
     */
    fun toMeeple(): Meeple {
        return Meeple(
            playerId = playerId.value.toString(),
            position = position,
            featureId = featureId.value.toString()
        )
    }

    /**
     * Return meeple to player
     */
    fun returnToPlayer() = transaction {
        if (!returned) {
            returned = true
            returnedAt = LocalDateTime.now()
            
            // Return meeple to player's supply
            val player = PlayerDao.findById(playerId.value)
            player?.let {
                it.meeples++
                it.updatedAt = LocalDateTime.now()
            }
        }
    }
}