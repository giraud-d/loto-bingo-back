package me.giraudet.lotobingo.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing the mapping between a number (1-90) and a word.
 */
@Entity
@Table(name = "word_mappings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WordMappingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private GameEntity game;

    @Column(nullable = false)
    private Integer number;

    @Column(nullable = false, length = 500)
    private String word;
}
