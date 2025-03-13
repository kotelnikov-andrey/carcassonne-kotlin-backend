# Carcassonne Frontend-Backend Integration Guide

This guide explains how to integrate the existing Node.js frontend with the new Kotlin backend for the Carcassonne game.

## Overview

The Kotlin backend provides a RESTful API that the frontend can use to:
- Create and join games
- Start games
- Draw tiles
- Place tiles on the board
- Place meeples
- End turns
- Calculate scores

## API Endpoints

All API endpoints are documented in the OpenAPI specification at `/swagger` when the server is running.

### Authentication

The backend uses JWT tokens for authentication. When a player creates or joins a game, they receive a JWT token that must be included in the `Authorization` header for all subsequent requests:

```
Authorization: Bearer <token>
```

### Key Endpoints

| Endpoint | Method | Description | Authentication Required |
|----------|--------|-------------|------------------------|
| `/game` | POST | Create a new game | No |
| `/game/{gameId}/join` | POST | Join an existing game | No |
| `/game/{gameId}` | GET | Get the current game state | Yes |
| `/game/{gameId}/start` | POST | Start a game | Yes (host only) |
| `/game/takeCard` | POST | Draw a card from the deck | Yes |
| `/game/{gameId}/placeTile` | POST | Place a tile on the board | Yes |
| `/game/{gameId}/placeMeeple` | POST | Place a meeple on a tile | Yes |
| `/game/{gameId}/endTurn` | POST | End the current player's turn | Yes |

## Frontend Integration Steps

### 1. Update API Client

Update the frontend API client to use the new backend endpoints. Here's an example of how to update the API calls in the frontend:

```javascript
// Example: Creating a game
async function createGame(playerName) {
  const response = await fetch('http://localhost:8080/game', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      playerName,
      gameOptions: {
        includeExpansions: ['base'],
        maxPlayers: 4,
      },
    }),
  });
  
  const data = await response.json();
  
  // Store the token for future requests
  localStorage.setItem('jwt', data.token);
  
  return data;
}

// Example: Getting game state
async function getGameState(gameId) {
  const token = localStorage.getItem('jwt');
  
  const response = await fetch(`http://localhost:8080/game/${gameId}`, {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  });
  
  return await response.json();
}
```

### 2. Update Authentication Logic

The frontend needs to store and use the JWT token for authenticated requests:

```javascript
// Add this function to your API client
function getAuthHeader() {
  const token = localStorage.getItem('jwt');
  return token ? { 'Authorization': `Bearer ${token}` } : {};
}

// Use it in your API calls
async function placeTile(gameId, tileId, x, y, rotation) {
  const response = await fetch(`http://localhost:8080/game/${gameId}/placeTile`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...getAuthHeader(),
    },
    body: JSON.stringify({
      tileId,
      x,
      y,
      rotation,
    }),
  });
  
  return await response.json();
}
```

### 3. Update Game Flow

The game flow in the frontend should follow these steps:

1. Create a game (host) or join a game (other players)
2. Start the game (host only)
3. For each player's turn:
   - Draw a card
   - Place the tile
   - Optionally place a meeple
   - End the turn
4. Game ends when all tiles are placed
5. Final scoring is calculated automatically

### 4. Update Data Models

The frontend needs to adapt to the data models provided by the backend. Here are the key models:

#### Game State

```javascript
{
  gameId: "string",
  status: "waiting" | "active" | "finished",
  board: [Tile],
  players: [Player],
  currentPlayerId: "string",
  remainingCards: number,
  currentTileId: "string" | null
}
```

#### Tile

```javascript
{
  tileId: "string",
  tileType: "string",
  x: number,
  y: number,
  rotation: number,
  sides: {
    north: "city" | "road" | "field",
    east: "city" | "road" | "field",
    south: "city" | "road" | "field",
    west: "city" | "road" | "field"
  },
  features: [TileFeature],
  meeple: Meeple | null
}
```

#### Player

```javascript
{
  playerId: "string",
  name: "string",
  color: "string",
  meeples: number,
  score: number,
  isHost: boolean
}
```

### 5. Update Rendering Logic

The frontend needs to update its rendering logic to match the new data models:

```javascript
// Example: Rendering a tile
function renderTile(tile) {
  // Create a tile element
  const tileElement = document.createElement('div');
  tileElement.className = 'tile';
  tileElement.style.transform = `rotate(${tile.rotation}deg)`;
  
  // Set the background image based on tile type
  tileElement.style.backgroundImage = `url('/images/tiles/${tile.tileType}.png')`;
  
  // Position the tile on the board
  tileElement.style.gridColumn = tile.x + 1;
  tileElement.style.gridRow = tile.y + 1;
  
  // Render meeple if present
  if (tile.meeple) {
    const meepleElement = document.createElement('div');
    meepleElement.className = 'meeple';
    
    // Get player color
    const player = gameState.players.find(p => p.playerId === tile.meeple.playerId);
    meepleElement.style.backgroundColor = player.color;
    
    // Position meeple based on position
    positionMeeple(meepleElement, tile.meeple.position);
    
    tileElement.appendChild(meepleElement);
  }
  
  return tileElement;
}
```

## Testing the Integration

1. Start the backend server:
   ```
   cd Carcassone-Backend
   ./gradlew run
   ```

2. Start the frontend server:
   ```
   cd carcassonne-frontend
   npm start
   ```

3. Open the frontend in your browser and test the integration.

4. You can also use the provided test script to test the API directly:
   ```
   cd Carcassone-Backend
   ./test-api.sh
   ```

## Troubleshooting

### CORS Issues

If you encounter CORS issues, make sure the backend CORS configuration matches your frontend's origin. The backend is already configured to allow requests from any origin, but you may need to adjust this for production.

### Authentication Issues

If you encounter authentication issues:
- Check that the JWT token is being stored correctly
- Ensure the token is included in the Authorization header
- Verify that the token hasn't expired

### Data Model Mismatches

If the frontend and backend data models don't match:
- Check the OpenAPI specification for the latest model definitions
- Update the frontend models to match the backend
- Consider creating adapter functions to transform data between formats

## Next Steps

After completing the basic integration, consider implementing these additional features:

1. Real-time updates using WebSockets
2. Offline support with local storage
3. Enhanced error handling and user feedback
4. Animations for tile placement and scoring
5. Support for expansions (Inns & Cathedrals, Traders & Builders, etc.)