package za.ac.up.cos326.prac5;

/** Raised for invalid user input (blank / too long) or an account name that is not in the database. */
public class InvalidAccountException extends RuntimeException {
    public InvalidAccountException(String message) {
        super(message);
    }
}
