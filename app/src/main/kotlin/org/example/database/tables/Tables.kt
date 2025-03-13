package org.example.database.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

/**
 * Table for storing game information
 */
object GamesTable : UUIDTable("games") {
    val status = varchar("status", 20)
    val remainingCards = integer("remaining_cards")
    val currentPlayerId = varchar("current_player_id", 36).nullable()
    val currentTileId = varchar("current_tile_id", 36).nullable()
    val joinCode = varchar("join_code", 10).uniqueIndex()
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
    val expansions = text("expansions").default("[]") // JSON array of included expansions
}

/**
 * Table for storing player information
 */
object PlayersTable : UUIDTable("players") {
    val gameId = reference("game_id", GamesTable)
    val name = varchar("name", 50)
    val color = varchar("color", 20)
    val meeples = integer("meeples")
    val score = integer("score").default(0)
    val isHost = bool("is_host").default(false)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

/**
 * Table for storing tile information
 */
object TilesTable : UUIDTable("tiles") {
    val gameId = reference("game_id", GamesTable)
    val tileType = varchar("tile_type", 50)
    val x = integer("x").nullable()
    val y = integer("y").nullable()
    val rotation = integer("rotation").default(0)
    val northSide = varchar("north_side", 20)
    val eastSide = varchar("east_side", 20)
    val southSide = varchar("south_side", 20)
    val westSide = varchar("west_side", 20)
    val isPlaced = bool("is_placed").default(false)
    val placedAt = datetime("placed_at").nullable()
    val placedByPlayerId = reference("placed_by_player_id", PlayersTable).nullable()
}

/**
 * Table for storing tile features (cities, roads, monasteries, fields)
 */
object TileFeaturesTable : UUIDTable("tile_features") {
    val tileId = reference("tile_id", TilesTable)
    val featureType = varchar("feature_type", 20)
    val sides = text("sides") // JSON array of sides this feature connects to
    val completed = bool("completed").default(false)
    val points = integer("points").nullable()
    val completedAt = datetime("completed_at").nullable()
}

/**
 * Table for storing meeple placements
 */
object MeeplesTable : UUIDTable("meeples") {
    val gameId = reference("game_id", GamesTable)
    val playerId = reference("player_id", PlayersTable)
    val tileId = reference("tile_id", TilesTable)
    val featureId = reference("feature_id", TileFeaturesTable)
    val position = varchar("position", 20)
    val placedAt = datetime("placed_at")
    val returned = bool("returned").default(false)
    val returnedAt = datetime("returned_at").nullable()
}