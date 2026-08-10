package kr.omong.dulpick.domain.place.application.exception;

public class PlaceImportClaimLostException extends RuntimeException {

    public PlaceImportClaimLostException() {
        super("Place import claim ownership was lost");
    }
}
