package org.example.database.dao

import org.example.database.tables.PlayersTable
import org.example.model.Player
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
        return Player(
            playerId = id.value.toString(),
            name = name,
            color = color,
            meeples = meeples,
            score = score,
            isHost = isHost
        )
    }

    /**
     * Add points to player's score
     */
    fun addPoints(points: Int) {
        score += points
        updatedAt = LocalDateTime.now()
    }
}