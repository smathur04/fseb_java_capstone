package assembly.general.api.exception;

public class ReservationBusinessException extends RuntimeException {

    private final String error;
    private final String detailName;
    private final Object detailValue;

    public ReservationBusinessException(
            String error,
            String message,
            String detailName,
            Object detailValue
    ) {
        super(message);
        this.error = error;
        this.detailName = detailName;
        this.detailValue = detailValue;
    }

    public String getError() {
        return error;
    }

    public String getDetailName() {
        return detailName;
    }

    public Object getDetailValue() {
        return detailValue;
    }
}