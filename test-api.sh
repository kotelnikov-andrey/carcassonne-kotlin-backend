#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Base URL
BASE_URL="http://localhost:8080"

# Store tokens and IDs
GAME_ID=""
HOST_TOKEN=""
PLAYER2_TOKEN=""
JOIN_CODE=""
TILE_ID=""

echo -e "${BLUE}Carcassonne API Test Script${NC}"
echo "=============================="
echo ""

# Check if the server is running
echo -e "${BLUE}Checking if the server is running...${NC}"
SERVER_STATUS=$(curl -s -o /dev/null -w "%{http_code}" $BASE_URL)
if [ $SERVER_STATUS -ne 200 ]; then
    echo -e "${RED}Server is not running. Please start the server first.${NC}"
    exit 1
fi
echo -e "${GREEN}Server is running!${NC}"
echo ""

# Create a new game
echo -e "${BLUE}Creating a new game...${NC}"
CREATE_GAME_RESPONSE=$(curl -s -X POST "$BASE_URL/game" \
    -H "Content-Type: application/json" \
    -d '{"playerName": "Host Player", "gameOptions": {"includeExpansions": ["base"], "maxPlayers": 4}}')

GAME_ID=$(echo $CREATE_GAME_RESPONSE | grep -o '"gameId":"[^"]*' | cut -d'"' -f4)
HOST_TOKEN=$(echo $CREATE_GAME_RESPONSE | grep -o '"token":"[^"]*' | cut -d'"' -f4)
JOIN_CODE=$(echo $CREATE_GAME_RESPONSE | grep -o '"joinCode":"[^"]*' | cut -d'"' -f4)

if [ -z "$GAME_ID" ] || [ -z "$HOST_TOKEN" ] || [ -z "$JOIN_CODE" ]; then
    echo -e "${RED}Failed to create game.${NC}"
    echo $CREATE_GAME_RESPONSE
    exit 1
fi

echo -e "${GREEN}Game created successfully!${NC}"
echo "Game ID: $GAME_ID"
echo "Join Code: $JOIN_CODE"
echo ""

# Join the game as a second player
echo -e "${BLUE}Joining the game as a second player...${NC}"
JOIN_GAME_RESPONSE=$(curl -s -X POST "$BASE_URL/game/$GAME_ID/join" \
    -H "Content-Type: application/json" \
    -d "{\"code\": \"$JOIN_CODE\", \"playerName\": \"Player 2\"}")

PLAYER2_TOKEN=$(echo $JOIN_GAME_RESPONSE | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$PLAYER2_TOKEN" ]; then
    echo -e "${RED}Failed to join game.${NC}"
    echo $JOIN_GAME_RESPONSE
    exit 1
fi

echo -e "${GREEN}Player 2 joined the game successfully!${NC}"
echo ""

# Get game state
echo -e "${BLUE}Getting game state...${NC}"
GAME_STATE_RESPONSE=$(curl -s -X GET "$BASE_URL/game/$GAME_ID" \
    -H "Authorization: Bearer $HOST_TOKEN")

echo -e "${GREEN}Game state retrieved successfully!${NC}"
echo ""

# Start the game
echo -e "${BLUE}Starting the game...${NC}"
START_GAME_RESPONSE=$(curl -s -X POST "$BASE_URL/game/$GAME_ID/start" \
    -H "Authorization: Bearer $HOST_TOKEN")

if [[ $START_GAME_RESPONSE == *"active"* ]]; then
    echo -e "${GREEN}Game started successfully!${NC}"
else
    echo -e "${RED}Failed to start game.${NC}"
    echo $START_GAME_RESPONSE
    exit 1
fi
echo ""

# Take a card
echo -e "${BLUE}Taking a card...${NC}"
TAKE_CARD_RESPONSE=$(curl -s -X POST "$BASE_URL/game/takeCard" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $HOST_TOKEN" \
    -d "{\"gameId\": \"$GAME_ID\"}")

TILE_ID=$(echo $TAKE_CARD_RESPONSE | grep -o '"tileId":"[^"]*' | cut -d'"' -f4)

if [ -z "$TILE_ID" ]; then
    echo -e "${RED}Failed to take a card.${NC}"
    echo $TAKE_CARD_RESPONSE
    exit 1
fi

echo -e "${GREEN}Card taken successfully!${NC}"
echo "Tile ID: $TILE_ID"
echo ""

# Place a tile
echo -e "${BLUE}Placing a tile...${NC}"
PLACE_TILE_RESPONSE=$(curl -s -X POST "$BASE_URL/game/$GAME_ID/placeTile" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $HOST_TOKEN" \
    -d "{\"tileId\": \"$TILE_ID\", \"x\": 1, \"y\": 0, \"rotation\": 0}")

if [[ $PLACE_TILE_RESPONSE == *"board"* ]]; then
    echo -e "${GREEN}Tile placed successfully!${NC}"
else
    echo -e "${RED}Failed to place tile.${NC}"
    echo $PLACE_TILE_RESPONSE
    exit 1
fi
echo ""

# End turn
echo -e "${BLUE}Ending turn...${NC}"
END_TURN_RESPONSE=$(curl -s -X POST "$BASE_URL/game/$GAME_ID/endTurn" \
    -H "Authorization: Bearer $HOST_TOKEN")

if [[ $END_TURN_RESPONSE == *"updatedGameState"* ]]; then
    echo -e "${GREEN}Turn ended successfully!${NC}"
else
    echo -e "${RED}Failed to end turn.${NC}"
    echo $END_TURN_RESPONSE
    exit 1
fi
echo ""

echo -e "${GREEN}All API tests completed successfully!${NC}"
echo ""
echo "You can continue playing the game by:"
echo "1. Taking a card with Player 2"
echo "2. Placing the tile"
echo "3. Ending the turn"
echo "4. Repeating with the next player"
echo ""
echo "Example commands:"
echo "curl -X POST \"$BASE_URL/game/takeCard\" -H \"Content-Type: application/json\" -H \"Authorization: Bearer \$PLAYER2_TOKEN\" -d '{\"gameId\": \"$GAME_ID\"}'"
echo "curl -X POST \"$BASE_URL/game/$GAME_ID/placeTile\" -H \"Content-Type: application/json\" -H \"Authorization: Bearer \$PLAYER2_TOKEN\" -d '{\"tileId\": \"\$TILE_ID\", \"x\": 0, \"y\": 1, \"rotation\": 0}'"
echo "curl -X POST \"$BASE_URL/game/$GAME_ID/endTurn\" -H \"Authorization: Bearer \$PLAYER2_TOKEN\""