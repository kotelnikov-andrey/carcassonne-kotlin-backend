package org.example.database.dao

import org.example.database.tables.TileFeaturesTable
import org.example.database.tables.TilesTable
import org.example.model.Tile
import org.example.model.TileSides
import org.example.model.Meeple
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
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
        // Find meeples on this tile
        val meeple = MeepleDao.find {
            (MeeplesTable.tileId eq id) and
            (MeeplesTable.returned eq false)
        }.firstOrNull()?.let {
            Meeple(
                playerId = it.playerId.value.toString(),
                position = it.position,
                featureId = it.featureId.value.toString()
            )
        }

        val effectiveSides = getEffectiveSides()

        Tile(
            tileId = id.value.toString(),
            tileType = tileType,
            x = x ?: 0,
            y = y ?: 0,
            rotation = rotation,
            sides = TileSides(
                north = effectiveSides[0],
                east = effectiveSides[1],
                south = effectiveSides[2],
                west = effectiveSides[3]
            ),
            features = features.map { it.toTileFeature() },
            meeple = meeple
        )
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
        // Clear existing features
        TileFeatureDao.find { TileFeaturesTable.tileId eq id }.forEach { it.delete() }

        // Create new features based on tile type
        when (tileType) {
            "monastery_road" -> {
                // Monastery in center
                TileFeatureDao.new {
                    this.tileId = this@TileDao.id
                    this.featureType = "monastery"
                    this.sides = "[]" // Monastery doesn't connect to sides
                    this.completed = false
                }
                // Road on south
                TileFeatureDao.new {
                    this.tileId = this@TileDao.id
                    this.featureType = "road"
                    this.sides = """["south"]"""
                    this.completed = false
                }
            }
            "monastery" -> {
                // Just a monastery
                TileFeatureDao.new {
                    this.tileId = this@TileDao.id
                    this.featureType = "monastery"
                    this.sides = "[]"
                    this.completed = false
                }
            }
            "city_cap" -> {
                // City on north
                TileFeatureDao.new {
                    this.tileId = this@TileDao.id
                    this.featureType = "city"
                    this.sides = """["north"]"""
                    this.completed = false
                }
                // Field on other sides
                TileFeatureDao.new {
                    this.tileId = this@TileDao.id
                    this.featureType = "field"
                    this.sides = """["east", "south", "west"]"""
                    this.completed = false
                }
            }
            // Add more tile types as needed
            else -> {
                // Default field on all sides
                TileFeatureDao.new {
                    this.tileId = this@TileDao.id
                    this.featureType = "field"
                    this.sides = """["north", "east", "south", "west"]"""
                    this.completed = false
                }
            }
        }
    }
}