package kr.omong.dulpick.domain.place.domain;

public enum ContentSourceType {
    INSTAGRAM_REEL,
    INSTAGRAM_POST,
    NAVER_MAP,
    NAVER_BLOG,
    NAVER_SHORT_LINK,
    TISTORY

    ;

    public boolean storesPublicContent() {
        return this == INSTAGRAM_REEL || this == INSTAGRAM_POST;
    }
}
