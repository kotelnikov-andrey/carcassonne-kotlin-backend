#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}Carcassonne Backend PR Creation Script${NC}"
echo "========================================"
echo ""

# Check if Git is installed
if ! command -v git &> /dev/null; then
    echo -e "${RED}Git is not installed. Please install Git first.${NC}"
    exit 1
fi

# Initialize Git repository if not already initialized
if [ ! -d ".git" ]; then
    echo -e "${BLUE}Initializing Git repository...${NC}"
    git init
    echo -e "${GREEN}Git repository initialized.${NC}"
else
    echo -e "${GREEN}Git repository already initialized.${NC}"
fi

# Create a new branch
BRANCH_NAME="feature/kotlin-backend-$(date +%Y%m%d%H%M%S)"
echo -e "${BLUE}Creating new branch: ${BRANCH_NAME}...${NC}"
git checkout -b $BRANCH_NAME
echo -e "${GREEN}Branch created.${NC}"

# Add all files to Git
echo -e "${BLUE}Adding files to Git...${NC}"
git add .
echo -e "${GREEN}Files added.${NC}"

# Commit the changes with a descriptive message
echo -e "${BLUE}Committing changes...${NC}"
git commit -m "Implement Carcassonne backend with Kotlin and PostgreSQL

This commit includes:
- Complete OpenAPI specification for all game endpoints
- PostgreSQL database integration for game state persistence
- Game board rendering functionality
- Tile placement logic with validation
- Card drawing mechanism
- Game registration system with join codes
- Game initialization and turn management
- Meeple placement with validation
- Scoring calculation system
- Docker Compose setup for PostgreSQL
- Comprehensive test suite"

echo -e "${GREEN}Changes committed.${NC}"

# Ask for remote repository URL
echo ""
echo -e "${BLUE}To create a pull request, you need to push to a remote repository.${NC}"
read -p "Enter the remote repository URL (e.g., https://github.com/yourusername/carcassonne.git): " REMOTE_URL

if [ -z "$REMOTE_URL" ]; then
    echo -e "${RED}No remote URL provided. Skipping push.${NC}"
    echo -e "${GREEN}Local Git repository has been set up with all changes committed.${NC}"
    echo -e "You can manually push to a remote repository later with:"
    echo -e "  git remote add origin YOUR_REMOTE_URL"
    echo -e "  git push -u origin $BRANCH_NAME"
    exit 0
fi

# Add remote repository
echo -e "${BLUE}Adding remote repository...${NC}"
git remote add origin $REMOTE_URL
echo -e "${GREEN}Remote repository added.${NC}"

# Push to remote repository
echo -e "${BLUE}Pushing to remote repository...${NC}"
git push -u origin $BRANCH_NAME

if [ $? -eq 0 ]; then
    echo -e "${GREEN}Push successful!${NC}"
    echo -e "${BLUE}Now you can create a pull request on your Git hosting service (GitHub, GitLab, etc.).${NC}"
    echo -e "${BLUE}Include the following description in your PR:${NC}"
    echo ""
    echo "# Carcassonne Backend Implementation"
    echo ""
    echo "This PR implements a Kotlin backend for the Carcassonne board game with PostgreSQL database integration."
    echo ""
    echo "## Features"
    echo ""
    echo "- Complete OpenAPI specification for all game endpoints"
    echo "- PostgreSQL database integration for game state persistence"
    echo "- Game board rendering functionality"
    echo "- Tile placement logic with validation"
    echo "- Card drawing mechanism"
    echo "- Game registration system with join codes"
    echo "- Game initialization and turn management"
    echo "- Meeple placement with validation"
    echo "- Scoring calculation system"
    echo ""
    echo "## Technical Details"
    echo ""
    echo "- Built with Kotlin and Ktor framework"
    echo "- Uses Exposed ORM for database access"
    echo "- JWT authentication for secure API access"
    echo "- Docker Compose setup for PostgreSQL"
    echo "- Comprehensive test suite"
    echo ""
    echo "## How to Test"
    echo ""
    echo "1. Start the PostgreSQL database: \`docker-compose up -d\`"
    echo "2. Run the application: \`./gradlew run\`"
    echo "3. Use the test script: \`./test-api.sh\`"
else
    echo -e "${RED}Push failed. Please check your remote URL and try again manually.${NC}"
fi