package org.example

import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.auth.*
import io.ktor.http.*
import org.example.model.*
import org.example.service.GameService
import org.slf4j.LoggerFactory

/**
 * Routes for the Carcassonne game API
 */
fun Route.gameRoutes() {
    val logger = LoggerFactory.getLogger("GameRoutes")
    val gameService = GameService()

    // Public routes (no authentication required)
    
    // POST /game -> Create a new game
    post("/game") {
        try {
            val request = call.receive<CreateGameRequest>()
            val response = gameService.createGame(request)
            call.respond(response)
        } catch (e: Exception) {
            logger.error("Error creating game", e)
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    errorCode = "CREATE_GAME_ERROR",
                    errorMessage = e.message ?: "Could not create game"
                )
            )
        }
    }

    // POST /game/{gameId}/join -> Join an existing game
    post("/game/{gameId}/join") {
        try {
            val gameId = call.parameters["gameId"] ?: throw IllegalArgumentException("gameId is required")
            val request = call.receive<JoinGameRequest>()
            val response = gameService.joinGame(gameId, request)
            call.respond(response)
        } catch (e: Exception) {
            logger.error("Error joining game", e)
            val statusCode = when (e) {
                is IllegalArgumentException -> HttpStatusCode.BadRequest
                is IllegalStateException -> HttpStatusCode.Conflict
                else -> HttpStatusCode.InternalServerError
            }
            call.respond(
                statusCode,
                ErrorResponse(
                    errorCode = "JOIN_GAME_ERROR",
                    errorMessage = e.message ?: "Could not join game"
                )
            )
        }
    }

    // Authenticated routes
    authenticate("auth-jwt") {
        // GET /game/{gameId} -> Get game state
        get("/game/{gameId}") {
            try {
                val gameId = call.parameters["gameId"] ?: throw IllegalArgumentException("gameId is required")
                val gameState = gameService.getGameState(gameId)
                call.respond(gameState)
            } catch (e: Exception) {
                logger.error("Error getting game state", e)
                val statusCode = when (e) {
                    is IllegalArgumentException -> HttpStatusCode.NotFound
                    else -> HttpStatusCode.InternalServerError
                }
                call.respond(
                    statusCode,
                    ErrorResponse(
                        errorCode = "GET_GAME_ERROR",
                        errorMessage = e.message ?: "Could not get game state"
                    )
                )
            }
        }

        // POST /game/{gameId}/start -> Start game
        post("/game/{gameId}/start") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val playerIdFromToken = principal?.payload?.getClaim("playerId")?.asString()
                    ?: throw IllegalArgumentException("Invalid or missing token")

                val gameId = call.parameters["gameId"] ?: throw IllegalArgumentException("gameId is required")
                val gameState = gameService.startGame(gameId, playerIdFromToken)
                call.respond(gameState)
            } catch (e: Exception) {
                logger.error("Error starting game", e)
                val statusCode = when (e) {
                    is IllegalArgumentException -> HttpStatusCode.BadRequest
                    is IllegalStateException -> HttpStatusCode.Forbidden
                    else -> HttpStatusCode.InternalServerError
                }
                call.respond(
                    statusCode,
                    ErrorResponse(
                        errorCode = "GAME_START_ERROR",
                        errorMessage = e.message ?: "Could not start game"
                    )
                )
            }
        }

        // POST /game/takeCard -> Take a card from stack
        post("/game/takeCard") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val playerIdFromToken = principal?.payload?.getClaim("playerId")?.asString()
                    ?: throw IllegalArgumentException("Invalid or missing token")

                val request = call.receive<TakeCardRequest>()
                val response = gameService.takeCard(request, playerIdFromToken)
                call.respond(response)
            } catch (e: Exception) {
                logger.error("Error taking card", e)
                val statusCode = when (e) {
                    is IllegalArgumentException -> HttpStatusCode.BadRequest
                    is IllegalStateException -> HttpStatusCode.Forbidden
                    else -> HttpStatusCode.InternalServerError
                }
                call.respond(
                    statusCode,
                    ErrorResponse(
                        errorCode = "TAKE_CARD_ERROR",
                        errorMessage = e.message ?: "Could not take card"
                    )
                )
            }
        }

        // POST /game/{gameId}/placeTile -> Place a tile on the board
        post("/game/{gameId}/placeTile") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val playerIdFromToken = principal?.payload?.getClaim("playerId")?.asString()
                    ?: throw IllegalArgumentException("Invalid or missing token")

                val gameId = call.parameters["gameId"] ?: throw IllegalArgumentException("gameId is required")
                val request = call.receive<PlaceTileRequest>()
                val gameState = gameService.placeTile(gameId, request, playerIdFromToken)
                call.respond(gameState)
            } catch (e: Exception) {
                logger.error("Error placing tile", e)
                val statusCode = when (e) {
                    is IllegalArgumentException -> HttpStatusCode.BadRequest
                    is IllegalStateException -> HttpStatusCode.Forbidden
                    else -> HttpStatusCode.InternalServerError
                }
                call.respond(
                    statusCode,
                    ErrorResponse(
                        errorCode = "PLACE_TILE_ERROR",
                        errorMessage = e.message ?: "Could not place tile"
                    )
                )
            }
        }

        // POST /game/{gameId}/placeMeeple -> Place a meeple on a tile
        post("/game/{gameId}/placeMeeple") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val playerIdFromToken = principal?.payload?.getClaim("playerId")?.asString()
                    ?: throw IllegalArgumentException("Invalid or missing token")

                val gameId = call.parameters["gameId"] ?: throw IllegalArgumentException("gameId is required")
                val request = call.receive<PlaceMeepleRequest>()
                val gameState = gameService.placeMeeple(gameId, request, playerIdFromToken)
                call.respond(gameState)
            } catch (e: Exception) {
                logger.error("Error placing meeple", e)
                val statusCode = when (e) {
                    is IllegalArgumentException -> HttpStatusCode.BadRequest
                    is IllegalStateException -> HttpStatusCode.Forbidden
                    else -> HttpStatusCode.InternalServerError
                }
                call.respond(
                    statusCode,
                    ErrorResponse(
                        errorCode = "PLACE_MEEPLE_ERROR",
                        errorMessage = e.message ?: "Could not place meeple"
                    )
                )
            }
        }

        // POST /game/{gameId}/endTurn -> End the current player's turn
        post("/game/{gameId}/endTurn") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val playerIdFromToken = principal?.payload?.getClaim("playerId")?.asString()
                    ?: throw IllegalArgumentException("Invalid or missing token")

                val gameId = call.parameters["gameId"] ?: throw IllegalArgumentException("gameId is required")
                val response = gameService.endTurn(gameId, playerIdFromToken)
                call.respond(response)
            } catch (e: Exception) {
                logger.error("Error ending turn", e)
                val statusCode = when (e) {
                    is IllegalArgumentException -> HttpStatusCode.BadRequest
                    is IllegalStateException -> HttpStatusCode.Forbidden
                    else -> HttpStatusCode.InternalServerError
                }
                call.respond(
                    statusCode,
                    ErrorResponse(
                        errorCode = "END_TURN_ERROR",
                        errorMessage = e.message ?: "Could not end turn"
                    )
                )
            }
        }
    }
}
