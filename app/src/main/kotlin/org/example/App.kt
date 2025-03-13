package org.example

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.plugins.openapi.*
import io.ktor.server.plugins.cors.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.resources.*
import io.ktor.server.plugins.statuspages.*
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.example.database.DatabaseConfig
import org.example.model.ErrorResponse
import org.slf4j.LoggerFactory
import java.time.Duration

fun main() {
    // Initialize database
    DatabaseConfig.init()
    
    val logger = LoggerFactory.getLogger("Application")
    logger.info("Starting Carcassonne Backend")
    
    embeddedServer(Netty, port = 8080) {
        // Install features
        install(Resources)
        install(CORS) {
            anyHost()
            allowHeader(HttpHeaders.ContentType)
            allowHeader(HttpHeaders.Authorization)
            allowMethod(HttpMethod.Options)
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Put)
            allowMethod(HttpMethod.Delete)
        }
        install(Compression)
        install(DefaultHeaders)
        
        // Configure OpenAPI and Swagger UI
        routing {
            swaggerUI(path = "swagger", swaggerFile = "openapi.yaml")
            openAPI(path = "openapi", swaggerFile = "openapi.yaml")
        }
        
        configureStatusPages()
        configureSecurity()
        configureSerialization()
        configureRouting()
        
        logger.info("Carcassonne Backend started on port 8080")
    }.start(wait = true)
}

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            val logger = LoggerFactory.getLogger("StatusPages")
            logger.error("Unhandled exception", cause)
            
            val statusCode = when (cause) {
                is IllegalArgumentException -> HttpStatusCode.BadRequest
                is IllegalStateException -> HttpStatusCode.Conflict
                else -> HttpStatusCode.InternalServerError
            }
            
            call.respond(
                statusCode,
                ErrorResponse(
                    errorCode = "INTERNAL_ERROR",
                    errorMessage = cause.message ?: "An internal error occurred"
                )
            )
        }
    }
}

fun Application.configureSecurity() {
    val jwtSecret = System.getenv("JWT_SECRET") ?: "CHANGE_ME_SECRET"
    val jwtAudience = System.getenv("JWT_AUDIENCE") ?: "carcassonne-users"
    
    install(Authentication) {
        jwt("auth-jwt") {
            realm = "carcassonne-backend"
            verifier(
                com.auth0.jwt.JWT
                    .require(com.auth0.jwt.algorithms.Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .build()
            )
            validate { credential ->
                if (credential.payload.audience.contains(jwtAudience)) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse(
                        errorCode = "INVALID_TOKEN",
                        errorMessage = "Invalid or expired token"
                    )
                )
            }
        }
    }
}

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            registerModule(JavaTimeModule())
            registerKotlinModule()
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
    }
}

fun Application.configureRouting() {
    routing {
        // Public (no auth) endpoints
        get("/") {
            call.respondText("Carcassonne Backend is running!", ContentType.Text.Plain)
        }

        // Game routes (mix of public and secure endpoints)
        gameRoutes()
    }
}
