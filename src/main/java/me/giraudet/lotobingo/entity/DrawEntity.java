package me.giraudet.lotobingo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing a drawn number/word in a game.
 */
@Entity
@Table(name = "draws")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrawEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private GameEntity game;

    @Column(nullable = false)
    private Integer number;

    @Column(nullable = false)
    private String word;

    @Column(nullable = false)
    private Integer drawOrder;

    @Column(nullable = false)
    private LocalDateTime drawnAt;

    @PrePersist
    protected void onCreate() {
        drawnAt = LocalDateTime.now();
    }
}
