package me.giraudet.lotobingo.repository;

import me.giraudet.lotobingo.entity.DrawEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Draw entities.
 */
@Repository
public interface DrawRepository extends JpaRepository<DrawEntity, Long> {

    List<DrawEntity> findByGameIdOrderByDrawOrderAsc(UUID gameId);

    Optional<DrawEntity> findByGameIdAndNumber(UUID gameId, Integer number);

    boolean existsByGameIdAndNumber(UUID gameId, Integer number);

    long countByGameId(UUID gameId);
}
