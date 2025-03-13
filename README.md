# Carcassonne Backend

A Kotlin-based backend for the Carcassonne board game, providing a RESTful API for game management, tile placement, and scoring.

## Features

- Complete Carcassonne game logic implementation
- RESTful API with OpenAPI specification
- JWT-based authentication
- PostgreSQL database for game state persistence
- Docker Compose setup for easy development

## Prerequisites

- JDK 17 or higher
- Docker and Docker Compose
- Gradle

## Getting Started

### 1. Start the PostgreSQL Database

```bash
docker-compose up -d
```

This will start a PostgreSQL database on port 5432 and pgAdmin on port 5050.

### 2. Build the Application

```bash
./gradlew build
```

### 3. Run the Application

```bash
./gradlew run
```

The application will start on port 8080.

### 4. Access the API Documentation

Open your browser and navigate to:

```
http://localhost:8080/swagger
```

## API Overview

The API provides the following endpoints:

- **POST /game**: Create a new game
- **POST /game/{gameId}/join**: Join an existing game
- **POST /game/{gameId}/start**: Start a game
- **GET /game/{gameId}**: Get the current game state
- **POST /game/takeCard**: Draw a card from the deck
- **POST /game/{gameId}/placeTile**: Place a tile on the board
- **POST /game/{gameId}/placeMeeple**: Place a meeple on a tile
- **POST /game/{gameId}/endTurn**: End the current player's turn

## Game Flow

1. Create a game using `POST /game`
2. Share the game ID and join code with other players
3. Other players join using `POST /game/{gameId}/join`
4. The host starts the game using `POST /game/{gameId}/start`
5. Players take turns:
   - Draw a card using `POST /game/takeCard`
   - Place the tile using `POST /game/{gameId}/placeTile`
   - Optionally place a meeple using `POST /game/{gameId}/placeMeeple`
   - End the turn using `POST /game/{gameId}/endTurn`
6. The game continues until all tiles are placed
7. Final scoring is calculated automatically

## Environment Variables

The application can be configured using the following environment variables:

- `DATABASE_URL`: JDBC URL for the PostgreSQL database (default: `jdbc:postgresql://localhost:5432/carcassonne`)
- `DATABASE_USER`: Database username (default: `postgres`)
- `DATABASE_PASSWORD`: Database password (default: `postgres`)
- `JWT_SECRET`: Secret key for JWT token signing (default: `CHANGE_ME_SECRET`)
- `JWT_ISSUER`: Issuer for JWT tokens (default: `carcassonne-backend`)
- `JWT_AUDIENCE`: Audience for JWT tokens (default: `carcassonne-users`)
- `JWT_EXPIRATION_MS`: JWT token expiration time in milliseconds (default: 24 hours)

## Development

### Running Tests

```bash
./gradlew test
```

### Generating OpenAPI Models

The OpenAPI specification is located at `src/main/resources/openapi.yaml`. To generate the Kotlin models from this specification, run:

```bash
./gradlew openApiGenerate
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.