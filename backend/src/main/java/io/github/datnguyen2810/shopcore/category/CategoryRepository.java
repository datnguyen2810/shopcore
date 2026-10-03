package io.github.datnguyen2810.shopcore.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    List<Category> findAllByActiveTrueOrderByNameAsc();

    Optional<Category> findByIdAndActiveTrue(UUID id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Category c
            SET c.active = false,
                c.updatedAt = :updatedAt
            WHERE c.id = :id
                AND c.active = true
        """)
    int deactivateById(@Param("id") UUID id,
                       @Param("updatedAt") Instant updatedAt);
}
