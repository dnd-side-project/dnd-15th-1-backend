package kr.omong.dulpick.domain.place.application;

import org.springframework.http.MediaType;

public interface ContentThumbnailDownloader {

    DownloadedThumbnail download(String sourceUrl);

    record DownloadedThumbnail(
            byte[] bytes,
            MediaType contentType
    ) {
    }
}
