package org.example.service

import org.example.database.dao.*
import org.example.database.tables.PlayersTable
import org.example.model.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.UUID
import java.util.Random
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

/**
 * Service for handling game-related operations
 */
class GameService {
    private val jwtSecret = System.getenv("JWT_SECRET") ?: "CHANGE_ME_SECRET"
    private val jwtIssuer = System.getenv("JWT_ISSUER") ?: "carcassonne-backend"
    private val jwtAudience = System.getenv("JWT_AUDIENCE") ?: "carcassonne-users"
    private val jwtExpirationMs = System.getenv("JWT_EXPIRATION_MS")?.toLongOrNull() ?: (24 * 60 * 60 * 1000) // 24 hours

    /**
     * Create a new game
     */
    fun createGame(request: CreateGameRequest): CreateGameResponse = transaction {
        // Generate a unique game ID and join code
        val gameId = UUID.randomUUID()
        val joinCode = generateJoinCode()

        // Create the game
        val game = GameDao.new(gameId) {
            status = "waiting"
            remainingCards = 72 // Default for base game
            currentPlayerId = null
            currentTileId = null
            this.joinCode = joinCode
            createdAt = LocalDateTime.now()
            updatedAt = LocalDateTime.now()
            expansions = "[]" // Default to base game only
        }

        // Create the host player
        val player = game.createGame(request.playerName, request.gameOptions).second

        // Generate JWT token for the player
        val token = generateToken(player.id.value.toString(), game.id.value.toString(), true)

        CreateGameResponse(
            gameId = game.id.value.toString(),
            joinCode = joinCode,
            token = token,
            gameState = game.toGameState()
        )
    }

    /**
     * Join an existing game
     */
    fun joinGame(gameId: String, request: JoinGameRequest): JoinGameResponse = transaction {
        // Find the game
        val game = GameDao.findById(UUID.fromString(gameId))
            ?: throw IllegalArgumentException("Game not found")

        // Verify join code
        if (game.joinCode != request.code) {
            throw IllegalArgumentException("Invalid join code")
        }

        // Check if game is in waiting state
        if (game.status != "waiting") {
            throw IllegalStateException("Game has already started")
        }

        // Add player to the game
        val player = game.addPlayer(request.playerName)

        // Generate JWT token for the player
        val token = generateToken(player.id.value.toString(), game.id.value.toString(), false)

        JoinGameResponse(
            token = token,
            gameState = game.toGameState()
        )
    }

    /**
     * Start a game
     */
    fun startGame(gameId: String, playerId: String): GameState = transaction {
        // Find the game
        val game = GameDao.findById(UUID.fromString(gameId))
            ?: throw IllegalArgumentException("Game not found")

        // Find the player
        val player = PlayerDao.find {
            (PlayersTable.id eq UUID.fromString(playerId)) and
            (PlayersTable.gameId eq game.id)
        }.firstOrNull() ?: throw IllegalArgumentException("Player not found")

        // Check if player is the host
        if (!player.isHost) {
            throw IllegalStateException("Only the host can start the game")
        }

        // Start the game
        game.startGame()
    }

    /**
     * Get game state
     */
    fun getGameState(gameId: String): GameState = transaction {
        // Find the game
        val game = GameDao.findById(UUID.fromString(gameId))
            ?: throw IllegalArgumentException("Game not found")

        game.toGameState()
    }

    /**
     * Take a card from the deck
     */
    fun takeCard(request: TakeCardRequest, playerId: String): TakeCardResponse = transaction {
        // Find the game
        val game = GameDao.findById(UUID.fromString(request.gameId))
            ?: throw IllegalArgumentException("Game not found")

        // Check if it's the player's turn
        if (game.currentPlayerId != playerId) {
            throw IllegalStateException("It's not your turn")
        }

        // Check if game is active
        if (game.status != "active") {
            throw IllegalStateException("Game is not active")
        }

        // Draw a tile
        val tile = game.drawTile()
        game.currentTileId = tile.id.value.toString()

        TakeCardResponse(
            tileId = tile.id.value.toString(),
            tileDetails = tile.toTile(),
            updatedGameState = game.toGameState()
        )
    }

    /**
     * Place a tile on the board
     */
    fun placeTile(gameId: String, request: PlaceTileRequest, playerId: String): GameState = transaction {
        // Find the game
        val game = GameDao.findById(UUID.fromString(gameId))
            ?: throw IllegalArgumentException("Game not found")

        // Check if it's the player's turn
        if (game.currentPlayerId != playerId) {
            throw IllegalStateException("It's not your turn")
        }

        // Check if game is active
        if (game.status != "active") {
            throw IllegalStateException("Game is not active")
        }

        // Check if the tile ID matches the current tile
        if (game.currentTileId != request.tileId) {
            throw IllegalArgumentException("Invalid tile ID")
        }

        // Place the tile
        game.placeTile(
            UUID.fromString(request.tileId),
            request.x,
            request.y,
            request.rotation,
            UUID.fromString(playerId)
        )

        game.toGameState()
    }

    /**
     * Place a meeple on a tile
     */
    fun placeMeeple(gameId: String, request: PlaceMeepleRequest, playerId: String): GameState = transaction {
        // Find the game
        val game = GameDao.findById(UUID.fromString(gameId))
            ?: throw IllegalArgumentException("Game not found")

        // Check if it's the player's turn
        if (game.currentPlayerId != playerId) {
            throw IllegalStateException("It's not your turn")
        }

        // Check if game is active
        if (game.status != "active") {
            throw IllegalStateException("Game is not active")
        }

        // Place the meeple
        game.placeMeeple(
            UUID.fromString(playerId),
            UUID.fromString(request.tileId),
            request.position,
            UUID.fromString(request.featureId)
        )

        game.toGameState()
    }

    /**
     * End the current player's turn
     */
    fun endTurn(gameId: String, playerId: String): ScoreUpdateResponse = transaction {
        // Find the game
        val game = GameDao.findById(UUID.fromString(gameId))
            ?: throw IllegalArgumentException("Game not found")

        // Check if it's the player's turn
        if (game.currentPlayerId != playerId) {
            throw IllegalStateException("It's not your turn")
        }

        // Check if game is active
        if (game.status != "active") {
            throw IllegalStateException("Game is not active")
        }

        // End the turn
        val (gameState, scoreUpdates) = game.endTurn()

        // Convert score updates to API model
        val scoreUpdateModels = scoreUpdates.map { update ->
            ScoreUpdate(
                playerId = update.playerId,
                points = update.points,
                feature = FeatureScoreInfo(
                    featureType = update.feature.featureType,
                    completed = update.feature.completed
                )
            )
        }

        ScoreUpdateResponse(
            scoreUpdates = scoreUpdateModels,
            updatedGameState = gameState
        )
    }

    /**
     * Generate a unique join code
     */
    private fun generateJoinCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val random = Random()
        val codeLength = 6
        
        val code = StringBuilder()
        repeat(codeLength) {
            code.append(chars[random.nextInt(chars.length)])
        }
        
        return code.toString()
    }

    /**
     * Generate a JWT token for a player
     */
    private fun generateToken(playerId: String, gameId: String, isHost: Boolean): String {
        val now = Date()
        val expiryDate = Date(now.time + jwtExpirationMs)
        
        return JWT.create()
            .withIssuer(jwtIssuer)
            .withAudience(jwtAudience)
            .withIssuedAt(now)
            .withExpiresAt(expiryDate)
            .withClaim("playerId", playerId)
            .withClaim("gameId", gameId)
            .withClaim("isHost", isHost)
            .sign(Algorithm.HMAC256(jwtSecret))
    }
}