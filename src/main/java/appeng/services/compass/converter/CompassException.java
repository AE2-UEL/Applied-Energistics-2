package appeng.services.compass.converter;

public class CompassException extends RuntimeException {

    private static final long serialVersionUID = 8825268683203860877L;

    private final Throwable inner;

    public CompassException(final Throwable t) {
        this.inner = t;
    }
}