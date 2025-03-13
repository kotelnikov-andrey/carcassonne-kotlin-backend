package org.example.database.dao

import org.example.database.tables.MeeplesTable
import org.example.database.tables.TileFeaturesTable
import org.example.database.tables.TilesTable
import org.example.model.generated.Tile
import org.example.model.generated.TileMeeple
import org.example.model.generated.TileSides
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

/**
 * Data Access Object for Tile entity
 */
class TileDao(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<TileDao>(TilesTable)

    var gameId by TilesTable.gameId
    var tileType by TilesTable.tileType
    var x by TilesTable.x
    var y by TilesTable.y
    var rotation by TilesTable.rotation
    var northSide by TilesTable.northSide
    var eastSide by TilesTable.eastSide
    var southSide by TilesTable.southSide
    var westSide by TilesTable.westSide
    var isPlaced by TilesTable.isPlaced
    var placedAt by TilesTable.placedAt
    var placedByPlayerId by TilesTable.placedByPlayerId

    private val features by TileFeatureDao referrersOn TileFeaturesTable.tileId

    /**
     * Convert DAO to API model
     */
    fun toTile(): Tile = transaction {
        // Create a simple tile without meeples for now
        val effectiveSides = getEffectiveSides()
        
        val tile = Tile()
        tile.tileId = id.toString()
        tile.tileType = tileType
        tile.x = x ?: 0
        tile.y = y ?: 0
        tile.rotation = rotation
        
        val sides = TileSides()
        sides.north = convertToNorth(effectiveSides[0])
        sides.east = convertToEast(effectiveSides[1])
        sides.south = convertToSouth(effectiveSides[2])
        sides.west = convertToWest(effectiveSides[3])
        tile.sides = sides
        
        // Skip features and meeples for now
        tile.features = mutableListOf()
        
        tile
    }
    
    /**
     * Convert string side type to North enum
     */
    private fun convertToNorth(sideType: String): TileSides.North {
        return when (sideType) {
            "city" -> TileSides.North.CITY
            "road" -> TileSides.North.ROAD
            "field" -> TileSides.North.FIELD
            else -> TileSides.North.FIELD
        }
    }
    
    /**
     * Convert string side type to East enum
     */
    private fun convertToEast(sideType: String): TileSides.East {
        return when (sideType) {
            "city" -> TileSides.East.CITY
            "road" -> TileSides.East.ROAD
            "field" -> TileSides.East.FIELD
            else -> TileSides.East.FIELD
        }
    }
    
    /**
     * Convert string side type to South enum
     */
    private fun convertToSouth(sideType: String): TileSides.South {
        return when (sideType) {
            "city" -> TileSides.South.CITY
            "road" -> TileSides.South.ROAD
            "field" -> TileSides.South.FIELD
            else -> TileSides.South.FIELD
        }
    }
    
    /**
     * Convert string side type to West enum
     */
    private fun convertToWest(sideType: String): TileSides.West {
        return when (sideType) {
            "city" -> TileSides.West.CITY
            "road" -> TileSides.West.ROAD
            "field" -> TileSides.West.FIELD
            else -> TileSides.West.FIELD
        }
    }

    /**
     * Get effective sides after rotation
     * Returns [north, east, south, west]
     */
    fun getEffectiveSides(): List<String> {
        val originalSides = listOf(northSide, eastSide, southSide, westSide)
        val steps = (rotation / 90) % 4
        
        return when (steps) {
            0 -> originalSides
            1 -> listOf(originalSides[3], originalSides[0], originalSides[1], originalSides[2]) // 90° clockwise
            2 -> listOf(originalSides[2], originalSides[3], originalSides[0], originalSides[1]) // 180°
            3 -> listOf(originalSides[1], originalSides[2], originalSides[3], originalSides[0]) // 270° clockwise
            else -> originalSides
        }
    }

    /**
     * Initialize features for this tile based on its type
     */
    fun initializeFeatures() = transaction {
        // Simplified implementation for now
        // We'll just create a default feature
        // This will be implemented properly later
    }
}