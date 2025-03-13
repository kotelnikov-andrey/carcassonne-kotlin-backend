package org.example

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.example.database.DatabaseConfig
import org.example.model.CreateGameRequest
import org.example.model.JoinGameRequest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AppTest {

    companion object {
        @Container
        val postgresContainer = PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
            withDatabaseName("carcassonne_test")
            withUsername("test")
            withPassword("test")
        }
    }

    private val objectMapper = ObjectMapper().apply {
        registerKotlinModule()
    }

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
    }

    @Test
    fun testRootEndpoint() = testApplication {
        application {
            configureStatusPages()
            configureSecurity()
            configureSerialization()
            configureRouting()
        }

        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Carcassonne Backend is running!", response.bodyAsText())
    }

    @Test
    fun testCreateGame() = testApplication {
        application {
            configureStatusPages()
            configureSecurity()
            configureSerialization()
            configureRouting()
        }

        val createGameRequest = CreateGameRequest(
            playerName = "Test Player"
        )

        val response = client.post("/game") {
            contentType(ContentType.Application.Json)
            setBody(objectMapper.writeValueAsString(createGameRequest))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("gameId"))
        assertTrue(responseBody.contains("joinCode"))
        assertTrue(responseBody.contains("token"))
    }

    @Test
    fun testJoinGame() = testApplication {
        application {
            configureStatusPages()
            configureSecurity()
            configureSerialization()
            configureRouting()
        }

        // First create a game
        val createGameRequest = CreateGameRequest(
            playerName = "Host Player"
        )

        val createResponse = client.post("/game") {
            contentType(ContentType.Application.Json)
            setBody(objectMapper.writeValueAsString(createGameRequest))
        }

        val createResponseBody = createResponse.bodyAsText()
        val gameId = objectMapper.readTree(createResponseBody).get("gameId").asText()
        val joinCode = objectMapper.readTree(createResponseBody).get("joinCode").asText()

        // Now join the game
        val joinGameRequest = JoinGameRequest(
            code = joinCode,
            playerName = "Second Player"
        )

        val joinResponse = client.post("/game/$gameId/join") {
            contentType(ContentType.Application.Json)
            setBody(objectMapper.writeValueAsString(joinGameRequest))
        }

        assertEquals(HttpStatusCode.OK, joinResponse.status)
        val joinResponseBody = joinResponse.bodyAsText()
        assertTrue(joinResponseBody.contains("token"))
        assertTrue(joinResponseBody.contains("gameState"))
        assertTrue(joinResponseBody.contains("Second Player"))
    }

    @Test
    fun testGetGameState() = testApplication {
        application {
            configureStatusPages()
            configureSecurity()
            configureSerialization()
            configureRouting()
        }

        // First create a game
        val createGameRequest = CreateGameRequest(
            playerName = "Host Player"
        )

        val createResponse = client.post("/game") {
            contentType(ContentType.Application.Json)
            setBody(objectMapper.writeValueAsString(createGameRequest))
        }

        val createResponseBody = createResponse.bodyAsText()
        val gameId = objectMapper.readTree(createResponseBody).get("gameId").asText()
        val token = objectMapper.readTree(createResponseBody).get("token").asText()

        // Now get the game state
        val getResponse = client.get("/game/$gameId") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, getResponse.status)
        val getResponseBody = getResponse.bodyAsText()
        assertTrue(getResponseBody.contains("gameId"))
        assertTrue(getResponseBody.contains("players"))
        assertTrue(getResponseBody.contains("Host Player"))
    }
}
