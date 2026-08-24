package kr.omong.dulpick.domain.place.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PlaceImageRepository extends JpaRepository<PlaceImage, Long> {

    @Modifying
    @Query("DELETE FROM PlaceImage image WHERE image.placeId = :placeId")
    void deleteAllByPlaceId(@Param("placeId") Long placeId);

    Optional<PlaceImage> findByStorageKey(String storageKey);
}
