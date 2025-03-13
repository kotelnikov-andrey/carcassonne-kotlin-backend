package org.example.database.dao

import org.example.database.tables.PlayersTable
import org.example.model.generated.Player
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.time.LocalDateTime
import java.util.UUID

/**
 * Data Access Object for Player entity
 */
class PlayerDao(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<PlayerDao>(PlayersTable)

    var gameId by PlayersTable.gameId
    var name by PlayersTable.name
    var color by PlayersTable.color
    var meeples by PlayersTable.meeples
    var score by PlayersTable.score
    var isHost by PlayersTable.isHost
    var createdAt by PlayersTable.createdAt
    var updatedAt by PlayersTable.updatedAt

    /**
     * Convert DAO to API model
     */
    fun toPlayer(): Player {
        val player = Player()
        player.playerId = id.value.toString()
        player.name = name
        player.color = color
        player.meeples = meeples
        player.score = score
        player.isHost = isHost
        
        return player
    }

    /**
     * Add points to player's score
     */
    fun addPoints(points: Int) {
        score += points
        updatedAt = LocalDateTime.now()
    }
}