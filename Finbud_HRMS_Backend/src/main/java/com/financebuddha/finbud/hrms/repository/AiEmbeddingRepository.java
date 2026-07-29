package com.financebuddha.finbud.hrms.repository;

import com.financebuddha.finbud.hrms.entity.AiEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiEmbeddingRepository extends JpaRepository<AiEmbedding, Long> {

    List<AiEmbedding> findByEntityType(String entityType);

    Optional<AiEmbedding> findByEntityTypeAndEntityId(String entityType, Long entityId);

    @Query(value = "SELECT * FROM ai_embeddings ORDER BY embedding <-> CAST(:embedding AS vector) LIMIT :limit", nativeQuery = true)
    List<AiEmbedding> findNearestNeighbors(@Param("embedding") String embedding, @Param("limit") int limit);

    @Query(value = "SELECT * FROM ai_embeddings WHERE entity_type = :entityType ORDER BY embedding <-> CAST(:embedding AS vector) LIMIT :limit", nativeQuery = true)
    List<AiEmbedding> findNearestNeighborsByEntityType(@Param("embedding") String embedding,
                                                         @Param("entityType") String entityType,
                                                         @Param("limit") int limit);

    void deleteByEntityTypeAndEntityId(String entityType, Long entityId);
}
