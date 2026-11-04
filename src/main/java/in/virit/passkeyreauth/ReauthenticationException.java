package in.virit.passkeyreauth;

/**
 * Thrown (as the cause of a failed future) when a re-authentication with a
 * passkey does not succeed: the user cancelled, has no passkeys, used a passkey
 * of somebody else, the authenticator could not verify the user, or the
 * response did not pass the verification.
 */
public class ReauthenticationException extends RuntimeException {

    public ReauthenticationException(String message) {
        super(message);
    }

    public ReauthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
