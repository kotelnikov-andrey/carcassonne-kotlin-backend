package org.example.service

import io.ktor.server.testing.*
import org.example.database.DatabaseConfig
import org.example.model.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.*

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GameServiceTest {

    companion object {
        @Container
        val postgresContainer = PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
            withDatabaseName("carcassonne_test")
            withUsername("test")
            withPassword("test")
        }
    }

    private lateinit var gameService: GameService

    @BeforeAll
    fun setupDatabase() {
        // Start the container
        postgresContainer.start()

        // Connect to the test database
        val jdbcUrl = postgresContainer.jdbcUrl
        val username = postgresContainer.username
        val password = postgresContainer.password

        // Set environment variables for DatabaseConfig
        System.setProperty("DATABASE_URL", jdbcUrl)
        System.setProperty("DATABASE_USER", username)
        System.setProperty("DATABASE_PASSWORD", password)

        // Initialize the database
        DatabaseConfig.init()

        // Create the GameService
        gameService = GameService()
    }

    @AfterEach
    fun cleanup() {
        // Clean up the database after each test
        transaction {
            exec("TRUNCATE TABLE games CASCADE")
            exec("TRUNCATE TABLE players CASCADE")
            exec("TRUNCATE TABLE tiles CASCADE")
            exec("TRUNCATE TABLE tile_features CASCADE")
            exec("TRUNCATE TABLE meeples CASCADE")
        }
    }

    @Test
    fun `test create game`() {
        // Create a game
        val request = CreateGameRequest(
            playerName = "Player 1",
            gameOptions = GameOptions(
                includeExpansions = listOf("base"),
                maxPlayers = 4,
                timerSeconds = 0
            )
        )

        val response = gameService.createGame(request)

        // Verify the response
        assertNotNull(response.gameId)
        assertNotNull(response.joinCode)
        assertNotNull(response.token)
        assertEquals("Player 1", response.gameState.players[0].name)
        assertEquals("waiting", response.gameState.status)
        assertTrue(response.gameState.players[0].isHost)
    }

    @Test
    fun `test join game`() {
        // Create a game
        val createRequest = CreateGameRequest(
            playerName = "Player 1"
        )
        val createResponse = gameService.createGame(createRequest)

        // Join the game
        val joinRequest = JoinGameRequest(
            code = createResponse.joinCode,
            playerName = "Player 2"
        )
        val joinResponse = gameService.joinGame(createResponse.gameId, joinRequest)

        // Verify the response
        assertNotNull(joinResponse.token)
        assertEquals(2, joinResponse.gameState.players.size)
        assertEquals("Player 1", joinResponse.gameState.players[0].name)
        assertEquals("Player 2", joinResponse.gameState.players[1].name)
        assertTrue(joinResponse.gameState.players[0].isHost)
        assertFalse(joinResponse.gameState.players[1].isHost)
    }

    @Test
    fun `test start game`() {
        // Create a game
        val createRequest = CreateGameRequest(
            playerName = "Player 1"
        )
        val createResponse = gameService.createGame(createRequest)

        // Join the game
        val joinRequest = JoinGameRequest(
            code = createResponse.joinCode,
            playerName = "Player 2"
        )
        val joinResponse = gameService.joinGame(createResponse.gameId, joinRequest)

        // Start the game
        val gameState = gameService.startGame(
            createResponse.gameId,
            createResponse.gameState.players[0].playerId
        )

        // Verify the response
        assertEquals("active", gameState.status)
        assertNotNull(gameState.currentPlayerId)
        assertEquals(1, gameState.board.size) // Initial tile
        assertTrue(gameState.remainingCards > 0)
    }

    @Test
    fun `test take card`() {
        // Create and start a game
        val createRequest = CreateGameRequest(
            playerName = "Player 1"
        )
        val createResponse = gameService.createGame(createRequest)

        // Join the game
        val joinRequest = JoinGameRequest(
            code = createResponse.joinCode,
            playerName = "Player 2"
        )
        gameService.joinGame(createResponse.gameId, joinRequest)

        // Start the game
        val gameState = gameService.startGame(
            createResponse.gameId,
            createResponse.gameState.players[0].playerId
        )

        // Take a card
        val takeCardRequest = TakeCardRequest(
            gameId = createResponse.gameId
        )
        val takeCardResponse = gameService.takeCard(
            takeCardRequest,
            gameState.currentPlayerId!!
        )

        // Verify the response
        assertNotNull(takeCardResponse.tileId)
        assertNotNull(takeCardResponse.tileDetails)
        assertEquals(gameState.remainingCards - 1, takeCardResponse.updatedGameState.remainingCards)
    }

    @Test
    fun `test place tile`() {
        // Create and start a game
        val createRequest = CreateGameRequest(
            playerName = "Player 1"
        )
        val createResponse = gameService.createGame(createRequest)

        // Join the game
        val joinRequest = JoinGameRequest(
            code = createResponse.joinCode,
            playerName = "Player 2"
        )
        gameService.joinGame(createResponse.gameId, joinRequest)

        // Start the game
        val gameState = gameService.startGame(
            createResponse.gameId,
            createResponse.gameState.players[0].playerId
        )

        // Take a card
        val takeCardRequest = TakeCardRequest(
            gameId = createResponse.gameId
        )
        val takeCardResponse = gameService.takeCard(
            takeCardRequest,
            gameState.currentPlayerId!!
        )

        // Place the tile
        val placeTileRequest = PlaceTileRequest(
            tileId = takeCardResponse.tileId,
            x = 1,
            y = 0,
            rotation = 0
        )
        val updatedGameState = gameService.placeTile(
            createResponse.gameId,
            placeTileRequest,
            gameState.currentPlayerId!!
        )

        // Verify the response
        assertEquals(2, updatedGameState.board.size) // Initial tile + placed tile
        val placedTile = updatedGameState.board.find { it.x == 1 && it.y == 0 }
        assertNotNull(placedTile)
        assertEquals(takeCardResponse.tileId, placedTile?.tileId)
    }

    @Test
    fun `test end turn`() {
        // Create and start a game
        val createRequest = CreateGameRequest(
            playerName = "Player 1"
        )
        val createResponse = gameService.createGame(createRequest)

        // Join the game
        val joinRequest = JoinGameRequest(
            code = createResponse.joinCode,
            playerName = "Player 2"
        )
        gameService.joinGame(createResponse.gameId, joinRequest)

        // Start the game
        val gameState = gameService.startGame(
            createResponse.gameId,
            createResponse.gameState.players[0].playerId
        )

        // Take a card
        val takeCardRequest = TakeCardRequest(
            gameId = createResponse.gameId
        )
        val takeCardResponse = gameService.takeCard(
            takeCardRequest,
            gameState.currentPlayerId!!
        )

        // Place the tile
        val placeTileRequest = PlaceTileRequest(
            tileId = takeCardResponse.tileId,
            x = 1,
            y = 0,
            rotation = 0
        )
        gameService.placeTile(
            createResponse.gameId,
            placeTileRequest,
            gameState.currentPlayerId!!
        )

        // End the turn
        val scoreUpdateResponse = gameService.endTurn(
            createResponse.gameId,
            gameState.currentPlayerId!!
        )

        // Verify the response
        assertNotEquals(gameState.currentPlayerId, scoreUpdateResponse.updatedGameState.currentPlayerId)
        assertNotNull(scoreUpdateResponse.updatedGameState.currentTileId)
    }
}