package kr.omong.dulpick.domain.place.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegionTagRepository extends JpaRepository<RegionTag, Long> {

    List<RegionTag> findAllByActiveTrueOrderByDisplayOrderAscIdAsc();

    Optional<RegionTag> findByIdAndActiveTrue(Long id);
}
