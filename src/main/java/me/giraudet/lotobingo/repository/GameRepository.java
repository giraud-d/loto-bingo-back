package me.giraudet.lotobingo.repository;

import me.giraudet.lotobingo.entity.GameEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for Game entities.
 * Spring Data JPA will automatically implement this interface.
 */
@Repository
public interface GameRepository extends JpaRepository<GameEntity, UUID> {

    // You can add custom query methods here
    // Example: List<GameEntity> findByStatus(GameEntity.GameStatus status);
}
