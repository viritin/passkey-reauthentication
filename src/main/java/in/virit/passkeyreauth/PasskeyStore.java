package in.virit.passkeyreauth;

import java.util.List;

/**
 * The bridge between {@link PasskeyReauthentication} and the passkeys your
 * application stores. This is the only part you need to implement, typically
 * with a few lines on top of the repository you already have for passkeys.
 */
public interface PasskeyStore {

    /**
     * Returns the passkeys of the currently logged-in user. Only these are
     * accepted in the re-authentication, so somebody else can't confirm the
     * action with their own passkey.
     * <p>
     * Called in the UI thread, when {@link PasskeyReauthentication#verify()} is
     * called, so the security context of your framework is available.
     */
    List<StoredPasskey> passkeysOfCurrentUser();

    /**
     * Called after a successful re-authentication with the new signature counter
     * of the passkey. Store it like your login does, so that cloned authenticators
     * can be detected. Note that synced passkeys (e.g. iCloud Keychain, Google
     * Password Manager) typically always report 0.
     *
     * @param passkey             the passkey that was used
     * @param newSignatureCount   the counter reported by the authenticator
     */
    void signatureCountUpdated(StoredPasskey passkey, long newSignatureCount);
}
