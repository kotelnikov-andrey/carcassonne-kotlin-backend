package org.example.model

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Game state model for API responses
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class GameState(
    val gameId: String,
    val status: String,
    val board: List<Tile>,
    val players: List<Player>,
    val currentPlayerId: String?,
    val remainingCards: Int,
    val currentTileId: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val expansions: List<String>? = null
)

/**
 * Tile model for API responses
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Tile(
    val tileId: String,
    val tileType: String,
    val x: Int,
    val y: Int,
    val rotation: Int,
    val sides: TileSides,
    val features: List<TileFeature>,
    val meeple: Meeple? = null
)

/**
 * Tile sides model
 */
data class TileSides(
    val north: String,
    val east: String,
    val south: String,
    val west: String
)

/**
 * Tile feature model
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class TileFeature(
    val featureId: String,
    val featureType: String,
    val sides: List<String>,
    val completed: Boolean,
    val points: Int? = null
)

/**
 * Player model for API responses
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Player(
    val playerId: String,
    val name: String,
    val color: String,
    val meeples: Int,
    val score: Int,
    val isHost: Boolean
)

/**
 * Meeple model for API responses
 */
data class Meeple(
    val playerId: String,
    val position: String,
    val featureId: String
)

/**
 * Error response model
 */
data class ErrorResponse(
    val errorCode: String,
    val errorMessage: String
)

/**
 * Game options model
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class GameOptions(
    val includeExpansions: List<String>? = null,
    val maxPlayers: Int? = null,
    val timerSeconds: Int? = null
)

/**
 * Feature score info model
 */
data class FeatureScoreInfo(
    val featureType: String,
    val completed: Boolean
)

// Import generated models to use in our code
typealias CreateGameRequest = org.example.model.generated.CreateGameRequest
typealias CreateGameResponse = org.example.model.generated.CreateGameResponse
typealias JoinGameRequest = org.example.model.generated.JoinGameRequest
typealias JoinGameResponse = org.example.model.generated.JoinGameResponse
typealias TakeCardRequest = org.example.model.generated.TakeCardRequest
typealias TakeCardResponse = org.example.model.generated.TakeCardResponse
typealias PlaceTileRequest = org.example.model.generated.PlaceTileRequest
typealias PlaceMeepleRequest = org.example.model.generated.PlaceMeepleRequest
typealias EndTurnRequest = org.example.model.generated.EndTurnRequest
typealias ScoreUpdateResponse = org.example.model.generated.ScoreUpdateResponse
typealias ScoreUpdate = org.example.model.generated.ScoreUpdateResponseScoreUpdatesInner