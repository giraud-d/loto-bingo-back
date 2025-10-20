-- Create games table
CREATE TABLE games (
    id UUID PRIMARY KEY,
    number_of_cards INTEGER NOT NULL,
    words_per_card INTEGER NOT NULL,
    grid_rows INTEGER NOT NULL,
    grid_columns INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

-- Create word_mappings table
CREATE TABLE word_mappings (
    id BIGSERIAL PRIMARY KEY,
    game_id UUID NOT NULL,
    number INTEGER NOT NULL,
    word VARCHAR(500) NOT NULL,
    CONSTRAINT fk_word_mappings_game FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE CASCADE
);

-- Create draws table
CREATE TABLE draws (
    id BIGSERIAL PRIMARY KEY,
    game_id UUID NOT NULL,
    number INTEGER NOT NULL,
    word VARCHAR(255) NOT NULL,
    draw_order INTEGER NOT NULL,
    drawn_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_draws_game FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE CASCADE
);

-- Create bingo_cards table
CREATE TABLE bingo_cards (
    id UUID PRIMARY KEY,
    game_id UUID NOT NULL,
    unique_identifier VARCHAR(255) NOT NULL UNIQUE,
    grid_data TEXT NOT NULL,
    has_won BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_bingo_cards_game FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX idx_word_mappings_game_id ON word_mappings(game_id);
CREATE INDEX idx_word_mappings_number ON word_mappings(number);
CREATE INDEX idx_draws_game_id ON draws(game_id);
CREATE INDEX idx_draws_draw_order ON draws(draw_order);
CREATE INDEX idx_bingo_cards_game_id ON bingo_cards(game_id);
CREATE INDEX idx_bingo_cards_unique_identifier ON bingo_cards(unique_identifier);
