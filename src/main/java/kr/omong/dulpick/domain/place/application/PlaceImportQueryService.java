package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.application.exception.PlaceImportAccessDeniedException;
import kr.omong.dulpick.domain.place.application.exception.PlaceImportNotFoundException;
import kr.omong.dulpick.domain.place.domain.PlaceImport;
import kr.omong.dulpick.domain.place.domain.PlaceImportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlaceImportQueryService {

    private final PlaceImportRepository importRepository;
    private final PlaceImportViewMapper viewMapper;

    public PlaceImportQueryService(
            PlaceImportRepository importRepository,
            PlaceImportViewMapper viewMapper
    ) {
        this.importRepository = importRepository;
        this.viewMapper = viewMapper;
    }

    @Transactional(readOnly = true)
    public PlaceImportView get(Long memberId, Long importId) {
        PlaceImport placeImport = importRepository.findById(importId)
                .orElseThrow(PlaceImportNotFoundException::new);
        if (!placeImport.getMemberId().equals(memberId)) {
            throw new PlaceImportAccessDeniedException();
        }
        return viewMapper.toView(placeImport);
    }
}
