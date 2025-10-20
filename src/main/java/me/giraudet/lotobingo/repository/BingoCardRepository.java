package me.giraudet.lotobingo.repository;

import me.giraudet.lotobingo.entity.BingoCardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for BingoCard entities.
 */
@Repository
public interface BingoCardRepository extends JpaRepository<BingoCardEntity, UUID> {

    List<BingoCardEntity> findByGameId(UUID gameId);

    Optional<BingoCardEntity> findByUniqueIdentifier(String uniqueIdentifier);
}
