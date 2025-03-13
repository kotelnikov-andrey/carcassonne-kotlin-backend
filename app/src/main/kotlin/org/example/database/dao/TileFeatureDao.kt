package org.example.database.dao

import org.example.database.tables.MeeplesTable
import org.example.database.tables.TileFeaturesTable
import org.example.model.TileFeature
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.UUID
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

/**
 * Data Access Object for TileFeature entity
 */
class TileFeatureDao(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<TileFeatureDao>(TileFeaturesTable)

    var tileId by TileFeaturesTable.tileId
    var featureType by TileFeaturesTable.featureType
    var sides by TileFeaturesTable.sides
    var completed by TileFeaturesTable.completed
    var points by TileFeaturesTable.points
    var completedAt by TileFeaturesTable.completedAt

    /**
     * Convert DAO to API model
     */
    fun toTileFeature(): TileFeature = transaction {
        val mapper = ObjectMapper()
        val sidesList = mapper.readValue<List<String>>(sides)

        TileFeature(
            featureId = id.value.toString(),
            featureType = featureType,
            sides = sidesList,
            completed = completed,
            points = points
        )
    }

    /**
     * Mark feature as completed
     */
    fun markCompleted(pointsValue: Int) {
        completed = true
        points = pointsValue
        completedAt = LocalDateTime.now()
    }

    /**
     * Check if this feature has a meeple
     */
    fun hasMeeple(): Boolean = transaction {
        MeepleDao.find {
            (MeeplesTable.featureId eq id) and
            (MeeplesTable.returned eq false)
        }.any()
    }

    /**
     * Get all meeples on this feature
     */
    fun getMeeples(): List<MeepleDao> = transaction {
        MeepleDao.find {
            (MeeplesTable.featureId eq id) and
            (MeeplesTable.returned eq false)
        }.toList()
    }

    /**
     * Return all meeples from this feature to their owners
     */
    fun returnMeeples() = transaction {
        val now = LocalDateTime.now()
        
        MeepleDao.find {
            (MeeplesTable.featureId eq id) and
            (MeeplesTable.returned eq false)
        }.forEach { meeple ->
            meeple.returned = true
            meeple.returnedAt = now
            
            // Return meeple to player
            val player = PlayerDao.findById(meeple.playerId.value)
            player?.let { p ->
                p.meeples++
                p.updatedAt = now
            }
        }
    }
}