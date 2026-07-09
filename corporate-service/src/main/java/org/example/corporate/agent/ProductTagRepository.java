package org.example.corporate.agent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductTagRepository extends JpaRepository<ProductTagRow, ProductTagId> {

    @Query("SELECT t.tag FROM ProductTagRow t WHERE t.productId = :productId ORDER BY t.tag")
    List<String> findTagsByProductId(@Param("productId") long productId);

    @Query("SELECT t.productId FROM ProductTagRow t WHERE t.tag IN :tags GROUP BY t.productId HAVING COUNT(DISTINCT t.tag) = :count")
    List<Long> findProductIdsWithAllTags(@Param("tags") List<String> tags, @Param("count") long count);
}
