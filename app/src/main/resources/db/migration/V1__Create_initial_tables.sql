-- Create games table
CREATE TABLE IF NOT EXISTS games (
    id UUID PRIMARY KEY,
    status VARCHAR(20) NOT NULL,
    remaining_cards INTEGER NOT NULL,
    current_player_id VARCHAR(36),
    current_tile_id VARCHAR(36),
    join_code VARCHAR(10) UNIQUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expansions TEXT NOT NULL DEFAULT '[]'
);

-- Create players table
CREATE TABLE IF NOT EXISTS players (
    id UUID PRIMARY KEY,
    game_id UUID NOT NULL REFERENCES games(id) ON DELETE CASCADE,
    name VARCHAR(50) NOT NULL,
    color VARCHAR(20) NOT NULL,
    meeples INTEGER NOT NULL,
    score INTEGER NOT NULL DEFAULT 0,
    is_host BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Create tiles table
CREATE TABLE IF NOT EXISTS tiles (
    id UUID PRIMARY KEY,
    game_id UUID NOT NULL REFERENCES games(id) ON DELETE CASCADE,
    tile_type VARCHAR(50) NOT NULL,
    x INTEGER,
    y INTEGER,
    rotation INTEGER NOT NULL DEFAULT 0,
    north_side VARCHAR(20) NOT NULL,
    east_side VARCHAR(20) NOT NULL,
    south_side VARCHAR(20) NOT NULL,
    west_side VARCHAR(20) NOT NULL,
    is_placed BOOLEAN NOT NULL DEFAULT FALSE,
    placed_at TIMESTAMP WITH TIME ZONE,
    placed_by_player_id UUID REFERENCES players(id) ON DELETE SET NULL
);

-- Create tile_features table
CREATE TABLE IF NOT EXISTS tile_features (
    id UUID PRIMARY KEY,
    tile_id UUID NOT NULL REFERENCES tiles(id) ON DELETE CASCADE,
    feature_type VARCHAR(20) NOT NULL,
    sides TEXT NOT NULL, -- JSON array of sides this feature connects to
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    points INTEGER,
    completed_at TIMESTAMP WITH TIME ZONE
);

-- Create meeples table
CREATE TABLE IF NOT EXISTS meeples (
    id UUID PRIMARY KEY,
    game_id UUID NOT NULL REFERENCES games(id) ON DELETE CASCADE,
    player_id UUID NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    tile_id UUID NOT NULL REFERENCES tiles(id) ON DELETE CASCADE,
    feature_id UUID NOT NULL REFERENCES tile_features(id) ON DELETE CASCADE,
    position VARCHAR(20) NOT NULL,
    placed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    returned BOOLEAN NOT NULL DEFAULT FALSE,
    returned_at TIMESTAMP WITH TIME ZONE
);

-- Create indexes for performance
CREATE INDEX idx_games_join_code ON games(join_code);
CREATE INDEX idx_players_game_id ON players(game_id);
CREATE INDEX idx_tiles_game_id ON tiles(game_id);
CREATE INDEX idx_tiles_coordinates ON tiles(x, y) WHERE x IS NOT NULL AND y IS NOT NULL;
CREATE INDEX idx_tile_features_tile_id ON tile_features(tile_id);
CREATE INDEX idx_meeples_game_id ON meeples(game_id);
CREATE INDEX idx_meeples_player_id ON meeples(player_id);
CREATE INDEX idx_meeples_tile_id ON meeples(tile_id);