package in.virit.passkeyreauth;

/**
 * Thrown (as the cause of a failed future) or passed to the failure callback
 * when a re-authentication with a passkey does not succeed: the user cancelled,
 * has no passkeys, used a passkey of somebody else, the authenticator could not
 * verify the user, or the response did not pass the verification.
 * <p>
 * The message is meant for the user and can be localized, see
 * {@link ReauthenticationTexts}. Technical details are in the cause.
 */
public class ReauthenticationException extends RuntimeException {

    private final boolean cancelledByUser;

    public ReauthenticationException(String message) {
        this(message, null, false);
    }

    public ReauthenticationException(String message, Throwable cause) {
        this(message, cause, false);
    }

    private ReauthenticationException(String message, Throwable cause, boolean cancelledByUser) {
        super(message, cause);
        this.cancelledByUser = cancelledByUser;
    }

    static ReauthenticationException cancelledByUser() {
        return new ReauthenticationException(ReauthenticationTexts.CANCELLED.get(), null, true);
    }

    /**
     * @return true if the user cancelled in the {@link ReauthenticationDialog},
     * typically no need to show an error then. Note that cancelling the passkey
     * prompt of the browser can't be told apart from a failed verification.
     */
    public boolean isCancelledByUser() {
        return cancelledByUser;
    }
}
