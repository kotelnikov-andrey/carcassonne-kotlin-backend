package org.example.database.dao

import org.example.database.tables.MeeplesTable
import org.example.database.tables.TileFeaturesTable
import org.example.model.generated.TileFeature
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and
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

        val feature = TileFeature()
        feature.featureId = id.toString()
        feature.featureType = convertFeatureType(featureType)
        feature.sides = sidesList.map { convertSide(it) }.toMutableList()
        feature.completed = completed
        feature.points = points
        
        feature
    }
    
    /**
     * Convert feature type string to enum
     */
    private fun convertFeatureType(type: String): TileFeature.FeatureType {
        return when (type) {
            "city" -> TileFeature.FeatureType.CITY
            "road" -> TileFeature.FeatureType.ROAD
            "monastery" -> TileFeature.FeatureType.MONASTERY
            "field" -> TileFeature.FeatureType.FIELD
            else -> TileFeature.FeatureType.FIELD
        }
    }
    
    /**
     * Convert side string to enum
     */
    private fun convertSide(side: String): TileFeature.Sides {
        return when (side) {
            "north" -> TileFeature.Sides.NORTH
            "east" -> TileFeature.Sides.EAST
            "south" -> TileFeature.Sides.SOUTH
            "west" -> TileFeature.Sides.WEST
            else -> TileFeature.Sides.NORTH
        }
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
        // Simplified implementation for now
        false
    }

    /**
     * Get all meeples on this feature
     */
    fun getMeeples(): List<MeepleDao> = transaction {
        // Simplified implementation for now
        emptyList()
    }

    /**
     * Return all meeples from this feature to their owners
     */
    fun returnMeeples() = transaction {
        // Simplified implementation for now
        // This will be implemented properly later
    }
}