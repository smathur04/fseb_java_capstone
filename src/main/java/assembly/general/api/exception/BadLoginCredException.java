package assembly.general.api.exception;

public class BadLoginCredException extends RuntimeException{
    public BadLoginCredException(String message) {
        super(message);
    }
}
