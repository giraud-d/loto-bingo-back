package me.giraudet.lotobingo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Entity representing a Bingo Card.
 */
@Entity
@Table(name = "bingo_cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BingoCardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private GameEntity game;

    @Column(nullable = false, unique = true)
    private String uniqueIdentifier;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String gridData; // JSON representation of the grid

    @Builder.Default
    @Column(nullable = false)
    private Boolean hasWon = false;
}
