package org.example.database.dao

import org.example.database.tables.*
import org.example.model.*
import org.example.model.generated.GameState
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

/**
 * Data Access Object for Game entity
 */
class GameDao(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<GameDao>(GamesTable)

    var status by GamesTable.status
    var remainingCards by GamesTable.remainingCards
    var currentPlayerId by GamesTable.currentPlayerId
    var currentTileId by GamesTable.currentTileId
    var joinCode by GamesTable.joinCode
    var createdAt by GamesTable.createdAt
    var updatedAt by GamesTable.updatedAt
    var expansions by GamesTable.expansions

    private val players by PlayerDao referrersOn PlayersTable.gameId
    private val tiles by TileDao referrersOn TilesTable.gameId
    private val meeples by MeepleDao referrersOn MeeplesTable.gameId

    /**
     * Convert DAO to API model
     */
    fun toGameState(): GameState {
        val mapper = ObjectMapper()
        val expansionsList = mapper.readValue<List<String>>(expansions)
        
        val gameState = GameState()
        gameState.gameId = id.value.toString()
        gameState.status = when (status) {
            "waiting" -> GameState.Status.WAITING
            "active" -> GameState.Status.ACTIVE
            "finished" -> GameState.Status.FINISHED
            else -> GameState.Status.WAITING
        }
        gameState.board = tiles.filter { it.isPlaced }.map { it.toTile() }.toMutableList()
        gameState.players = players.map { it.toPlayer() }.toMutableList()
        gameState.currentPlayerId = currentPlayerId
        gameState.remainingCards = remainingCards
        gameState.currentTileId = currentTileId
        gameState.createdAt = createdAt?.toOffsetDateTime()
        gameState.updatedAt = updatedAt?.toOffsetDateTime()
        
        return gameState
    }
    
    /**
     * Convert LocalDateTime to OffsetDateTime
     */
    private fun LocalDateTime.toOffsetDateTime(): OffsetDateTime {
        return this.atOffset(ZoneOffset.UTC)
    }

    /**
     * Create a new game
     */
    fun createGame(hostName: String, gameOptions: GameOptions?): Pair<GameDao, PlayerDao> = transaction {
        val hostPlayer = PlayerDao.new {
            gameId = this@GameDao.id
            name = hostName
            color = "red" // Default color, will be randomized on game start
            meeples = 7
            score = 0
            isHost = true
            createdAt = LocalDateTime.now()
            updatedAt = LocalDateTime.now()
        }

        // Set default expansions if none provided
        val expansionsJson = if (gameOptions?.includeExpansions != null) {
            ObjectMapper().writeValueAsString(gameOptions.includeExpansions)
        } else {
            "[]" // Default to base game only
        }

        this@GameDao.apply {
            status = "waiting"
            remainingCards = 72 // Default for base game
            currentPlayerId = null
            currentTileId = null
            expansions = expansionsJson
            createdAt = LocalDateTime.now()
            updatedAt = LocalDateTime.now()
        }

        Pair(this@GameDao, hostPlayer)
    }

    /**
     * Add a player to the game
     */
    fun addPlayer(playerName: String): PlayerDao = transaction {
        if (status != "waiting") {
            throw IllegalStateException("Cannot join a game that has already started")
        }

        val player = PlayerDao.new {
            gameId = this@GameDao.id
            name = playerName
            color = "blue" // Temporary, will be assigned on game start
            meeples = 7
            score = 0
            isHost = false
            createdAt = LocalDateTime.now()
            updatedAt = LocalDateTime.now()
        }

        this@GameDao.apply {
            updatedAt = LocalDateTime.now()
        }

        player
    }

    /**
     * Start the game
     */
    fun startGame(): GameState = transaction {
        if (status != "waiting") {
            throw IllegalStateException("Game has already started")
        }

        if (players.count() < 2) {
            throw IllegalStateException("At least 2 players are required to start the game")
        }

        // Assign random colors to players
        val colors = listOf("red", "blue", "green", "yellow", "black", "purple")
            .shuffled()
            .take(players.count().toInt())

        players.forEachIndexed { index, player ->
            player.color = colors[index]
            player.updatedAt = LocalDateTime.now()
        }

        // Initialize the tile deck
        initializeTileDeck()

        // Set the first player
        val firstPlayer = players.first()
        currentPlayerId = firstPlayer.id.value.toString()

        // Draw the first tile and place it in the center
        val startingTile = drawTile()
        placeTile(startingTile.id.value, 0, 0, 0, null)

        // Update game status
        status = "active"
        updatedAt = LocalDateTime.now()

        // Draw a tile for the first player
        val firstPlayerTile = drawTile()
        currentTileId = firstPlayerTile.id.value.toString()

        toGameState()
    }

    /**
     * Initialize the tile deck based on the game's expansions
     */
    private fun initializeTileDeck() {
        val mapper = ObjectMapper()
        val expansionsList = mapper.readValue<List<String>>(expansions)
        
        // Define tile types and their quantities for the base game
        val baseTiles = mapOf(
            "monastery_road" to 2,
            "monastery" to 4,
            "city_cap" to 5,
            "city_road" to 3,
            "city_road_bend" to 3,
            "city_three_sides" to 3,
            "city_diagonal" to 2,
            "city_full" to 1,
            "road_straight" to 8,
            "road_bend" to 9,
            "road_t" to 4,
            "road_crossing" to 1,
            "city_two_sides" to 2,
            "city_two_sides_road" to 3
        )
        
        // Create tiles and add them to the deck
        val allTiles = mutableListOf<TileDao>()
        
        baseTiles.forEach { (tileType, count) ->
            repeat(count) {
                val tile = createTile(tileType)
                allTiles.add(tile)
            }
        }
        
        // Add expansion tiles if needed
        if (expansionsList.contains("inns_and_cathedrals")) {
            // Add Inn & Cathedral tiles
            // This is just an example, actual implementation would include all expansion tiles
            val innTiles = mapOf(
                "city_road_inn" to 2,
                "city_cathedral" to 2
            )
            
            innTiles.forEach { (tileType, count) ->
                repeat(count) {
                    val tile = createTile(tileType)
                    allTiles.add(tile)
                }
            }
        }
        
        // Shuffle the deck
        allTiles.shuffle()
        
        // Update remaining cards count
        remainingCards = allTiles.size
    }
    
    /**
     * Create a tile with the specified type
     */
    private fun createTile(tileType: String): TileDao {
        // Define tile sides based on type
        val (north, east, south, west) = when (tileType) {
            "monastery_road" -> listOf("field", "field", "road", "field")
            "monastery" -> listOf("field", "field", "field", "field")
            "city_cap" -> listOf("city", "field", "field", "field")
            "city_road" -> listOf("city", "field", "road", "field")
            "city_road_bend" -> listOf("city", "road", "field", "road")
            "city_three_sides" -> listOf("city", "city", "field", "city")
            "city_diagonal" -> listOf("city", "field", "field", "city")
            "city_full" -> listOf("city", "city", "city", "city")
            "road_straight" -> listOf("field", "road", "field", "road")
            "road_bend" -> listOf("field", "road", "road", "field")
            "road_t" -> listOf("field", "road", "road", "road")
            "road_crossing" -> listOf("road", "road", "road", "road")
            "city_two_sides" -> listOf("city", "city", "field", "field")
            "city_two_sides_road" -> listOf("city", "city", "road", "field")
            "city_road_inn" -> listOf("city", "road", "field", "road") // Inn & Cathedral expansion
            "city_cathedral" -> listOf("city", "city", "city", "city") // Inn & Cathedral expansion
            else -> listOf("field", "field", "field", "field") // Default
        }
        
        return TileDao.new {
            this.gameId = this@GameDao.id
            this.tileType = tileType
            this.northSide = north
            this.eastSide = east
            this.southSide = south
            this.westSide = west
            this.isPlaced = false
            this.x = null
            this.y = null
            this.rotation = 0
            this.placedAt = null
            this.placedByPlayerId = null
        }
    }
    
    /**
     * Draw a tile from the deck
     */
    fun drawTile(): TileDao = transaction {
        if (status != "active") {
            throw IllegalStateException("Game is not active")
        }
        
        if (remainingCards <= 0) {
            throw IllegalStateException("No more tiles in the deck")
        }
        
        // Find an unplaced tile
        val tile = tiles.find { !it.isPlaced }
            ?: throw IllegalStateException("No more tiles available")
        
        // Update game state
        remainingCards--
        updatedAt = LocalDateTime.now()
        
        tile
    }
    
    /**
     * Place a tile on the board
     */
    fun placeTile(tileId: UUID, x: Int, y: Int, rotation: Int, playerId: UUID?): TileDao = transaction {
        if (status != "active") {
            throw IllegalStateException("Game is not active")
        }
        
        val tile = TileDao.findById(tileId)
            ?: throw IllegalArgumentException("Tile not found")
        
        if (tile.gameId.toString() != id.toString()) {
            throw IllegalArgumentException("Tile does not belong to this game")
        }
        
        if (tile.isPlaced) {
            throw IllegalArgumentException("Tile is already placed")
        }
        
        // Check if the position is valid
        validateTilePlacement(tile, x, y, rotation)
        
        // Place the tile
        tile.apply {
            this.x = x
            this.y = y
            this.rotation = rotation
            this.isPlaced = true
            this.placedAt = LocalDateTime.now()
            this.placedByPlayerId = playerId?.let { EntityID(it, PlayersTable) }
        }
        
        // Update game state
        updatedAt = LocalDateTime.now()
        
        tile
    }
    
    /**
     * Validate tile placement
     */
    private fun validateTilePlacement(tile: TileDao, x: Int, y: Int, rotation: Int) {
        // Check if the position is already occupied
        val existingTile = tiles.find { it.isPlaced && it.x == x && it.y == y }
        if (existingTile != null) {
            throw IllegalArgumentException("Position ($x, $y) is already occupied")
        }
        
        // Check if there are adjacent tiles
        val hasAdjacentTiles = tiles.any {
            it.isPlaced && (
                (it.x == x-1 && it.y == y) || // Left
                (it.x == x+1 && it.y == y) || // Right
                (it.x == x && it.y == y-1) || // Top
                (it.x == x && it.y == y+1)    // Bottom
            )
        }
        
        if (!hasAdjacentTiles && !(x == 0 && y == 0)) { // Allow (0,0) for the first tile
            throw IllegalArgumentException("Tile must be placed adjacent to an existing tile")
        }
        
        // Check if the tile matches adjacent tiles
        val effectiveSides = getEffectiveSides(tile, rotation)
        
        // Check left neighbor
        val leftTile = tiles.find { it.isPlaced && it.x == x-1 && it.y == y }
        if (leftTile != null) {
            val leftTileSides = getEffectiveSides(leftTile, leftTile.rotation)
            if (effectiveSides[3] != leftTileSides[1]) { // west of current tile must match east of left tile
                throw IllegalArgumentException("Tile does not match the left neighbor")
            }
        }
        
        // Check right neighbor
        val rightTile = tiles.find { it.isPlaced && it.x == x+1 && it.y == y }
        if (rightTile != null) {
            val rightTileSides = getEffectiveSides(rightTile, rightTile.rotation)
            if (effectiveSides[1] != rightTileSides[3]) { // east of current tile must match west of right tile
                throw IllegalArgumentException("Tile does not match the right neighbor")
            }
        }
        
        // Check top neighbor
        val topTile = tiles.find { it.isPlaced && it.x == x && it.y == y-1 }
        if (topTile != null) {
            val topTileSides = getEffectiveSides(topTile, topTile.rotation)
            if (effectiveSides[0] != topTileSides[2]) { // north of current tile must match south of top tile
                throw IllegalArgumentException("Tile does not match the top neighbor")
            }
        }
        
        // Check bottom neighbor
        val bottomTile = tiles.find { it.isPlaced && it.x == x && it.y == y+1 }
        if (bottomTile != null) {
            val bottomTileSides = getEffectiveSides(bottomTile, bottomTile.rotation)
            if (effectiveSides[2] != bottomTileSides[0]) { // south of current tile must match north of bottom tile
                throw IllegalArgumentException("Tile does not match the bottom neighbor")
            }
        }
    }
    
    /**
     * Get effective sides of a tile after rotation
     * Returns [north, east, south, west]
     */
    private fun getEffectiveSides(tile: TileDao, rotation: Int): List<String> {
        val originalSides = listOf(tile.northSide, tile.eastSide, tile.southSide, tile.westSide)
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
     * Place a meeple on a tile
     */
    fun placeMeeple(playerId: UUID, tileId: UUID, position: String, featureId: UUID): MeepleDao = transaction {
        if (status != "active") {
            throw IllegalStateException("Game is not active")
        }
        
        val player = PlayerDao.findById(playerId)
            ?: throw IllegalArgumentException("Player not found")
        
        if (player.gameId.toString() != id.toString()) {
            throw IllegalArgumentException("Player does not belong to this game")
        }
        
        if (player.meeples <= 0) {
            throw IllegalStateException("Player has no meeples left")
        }
        
        val tile = TileDao.findById(tileId)
            ?: throw IllegalArgumentException("Tile not found")
        
        if (tile.gameId.toString() != id.toString()) {
            throw IllegalArgumentException("Tile does not belong to this game")
        }
        
        if (!tile.isPlaced) {
            throw IllegalArgumentException("Tile is not placed on the board")
        }
        
        val feature = TileFeatureDao.findById(featureId)
            ?: throw IllegalArgumentException("Feature not found")
        
        if (feature.tileId.value != tileId) {
            throw IllegalArgumentException("Feature does not belong to this tile")
        }
        
        // Check if the feature already has a meeple
        val existingMeeple = meeples.find {
            !it.returned && it.featureId.value == featureId
        }
        
        if (existingMeeple != null) {
            throw IllegalStateException("Feature already has a meeple")
        }
        
        // Place the meeple
        val meeple = MeepleDao.new {
            this.gameId = this@GameDao.id
            this.playerId = player.id
            this.tileId = tile.id
            this.featureId = feature.id
            this.position = position
            this.placedAt = LocalDateTime.now()
            this.returned = false
            this.returnedAt = null
        }
        
        // Update player's meeple count
        player.meeples--
        player.updatedAt = LocalDateTime.now()
        
        // Update game state
        updatedAt = LocalDateTime.now()
        
        meeple
    }
    
    /**
     * End the current player's turn
     */
    fun endTurn(): Pair<GameState, MutableList<org.example.model.generated.ScoreUpdateResponseScoreUpdatesInner>> = transaction {
        if (status != "active") {
            throw IllegalStateException("Game is not active")
        }
        
        if (currentPlayerId == null) {
            throw IllegalStateException("No current player")
        }
        
        // Check for completed features and score them
        val scoreUpdates = scoreCompletedFeatures()
        
        // Move to the next player
        val currentPlayerIndex = players.indexOfFirst { it.id.value.toString() == currentPlayerId }
        val nextPlayerIndex = (currentPlayerIndex + 1) % players.count().toInt()
        val nextPlayer = players.elementAt(nextPlayerIndex)
        
        currentPlayerId = nextPlayer.id.value.toString()
        
        // Draw a tile for the next player if there are tiles left
        if (remainingCards > 0) {
            val nextTile = drawTile()
            currentTileId = nextTile.id.value.toString()
        } else {
            currentTileId = null
            
            // Check if this was the last tile
            if (tiles.none { !it.isPlaced }) {
                // Game is over, do final scoring
                val finalScoreUpdates = scoreFinalFeatures()
                scoreUpdates.addAll(finalScoreUpdates)
                
                // Update game status
                status = "finished"
            }
        }
        
        // Update game state
        updatedAt = LocalDateTime.now()
        
        Pair(toGameState(), scoreUpdates)
    }
    
    /**
     * Score completed features
     */
    private fun scoreCompletedFeatures(): MutableList<org.example.model.generated.ScoreUpdateResponseScoreUpdatesInner> {
        val scoreUpdates = mutableListOf<org.example.model.generated.ScoreUpdateResponseScoreUpdatesInner>()
        
        // Find all features that might be completed by the last placed tile
        val lastPlacedTile = tiles.filter { it.isPlaced }.maxByOrNull { it.placedAt ?: LocalDateTime.MIN }
            ?: return scoreUpdates
        
        // Get all features on this tile
        val tileFeatures = TileFeatureDao.find { TileFeaturesTable.tileId eq lastPlacedTile.id }.toList()
        
        for (feature in tileFeatures) {
            when (feature.featureType) {
                "city" -> scoreCompletedCity(feature, scoreUpdates)
                "road" -> scoreCompletedRoad(feature, scoreUpdates)
                "monastery" -> scoreCompletedMonastery(feature, scoreUpdates)
                // Fields are only scored at the end of the game
            }
        }
        
        return scoreUpdates
    }
    
    /**
     * Score a completed city
     */
    private fun scoreCompletedCity(feature: TileFeatureDao, scoreUpdates: MutableList<org.example.model.generated.ScoreUpdateResponseScoreUpdatesInner>) {
        // Implementation of city scoring logic
        // This would involve checking if the city is completed, calculating points,
        // and updating player scores
    }
    
    /**
     * Score a completed road
     */
    private fun scoreCompletedRoad(feature: TileFeatureDao, scoreUpdates: MutableList<org.example.model.generated.ScoreUpdateResponseScoreUpdatesInner>) {
        // Implementation of road scoring logic
    }
    
    /**
     * Score a completed monastery
     */
    private fun scoreCompletedMonastery(feature: TileFeatureDao, scoreUpdates: MutableList<org.example.model.generated.ScoreUpdateResponseScoreUpdatesInner>) {
        // Implementation of monastery scoring logic
    }
    
    /**
     * Score final features at the end of the game
     */
    private fun scoreFinalFeatures(): List<org.example.model.generated.ScoreUpdateResponseScoreUpdatesInner> {
        // Implementation of final scoring logic
        return emptyList()
    }
}