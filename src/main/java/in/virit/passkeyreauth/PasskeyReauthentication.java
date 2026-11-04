package in.virit.passkeyreauth;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinRequest;
import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.credential.CredentialRecord;
import com.webauthn4j.credential.CredentialRecordImpl;
import com.webauthn4j.data.AuthenticationData;
import com.webauthn4j.data.AuthenticationParameters;
import com.webauthn4j.data.attestation.authenticator.AAGUID;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Re-authentication ("step-up", "sudo mode") with a passkey for Vaadin
 * applications: before a sensitive action, ask the logged-in user to confirm
 * their presence with one of their passkeys. User verification is required, so
 * on today's devices this means biometrics (or the device PIN), proving that the
 * same person is still at the keyboard, not e.g. a colleague using a laptop left
 * unlocked.
 * <pre>{@code
 * reauthentication.runAfterVerification(
 *         () -> bankService.transfer(amount),
 *         e -> Notification.show("Not confirmed: " + e.getMessage()));
 * }</pre>
 * or, if you prefer composing futures, {@link #verify()}.
 * The whole ceremony happens over Vaadin's own communication: the challenge is
 * kept in server memory, the browser side is one {@code executeJs} call, and the
 * response is verified with WebAuthn4J against the passkeys given by your
 * {@link PasskeyStore}. No HTTP endpoints, cookies or changes to the login
 * session are involved, so this works the same with any security framework
 * (e.g. Spring Security or Quarkus) as long as you can list the passkeys of the
 * current user.
 * <p>
 * The callbacks of the returned future run in the UI thread (with the session
 * locked), so you can update the UI directly. Reuse one instance, e.g. as an
 * application scoped bean; configure it before use.
 */
public class PasskeyReauthentication {

    private static final WebAuthnManager WEB_AUTHN_MANAGER = WebAuthnManager.createNonStrictWebAuthnManager();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder BASE64URL = Base64.getUrlEncoder().withoutPadding();

    private final PasskeyStore passkeyStore;
    private String relyingPartyId;
    private Set<String> allowedOrigins;
    private Duration timeout = Duration.ofMinutes(1);

    public PasskeyReauthentication(PasskeyStore passkeyStore) {
        this.passkeyStore = Objects.requireNonNull(passkeyStore);
    }

    /**
     * Sets the relying party id (~ domain name) your passkeys are registered for,
     * the same value you have configured for your security framework. If not set,
     * the host name of the current request is used.
     */
    public PasskeyReauthentication withRelyingPartyId(String relyingPartyId) {
        this.relyingPartyId = relyingPartyId;
        return this;
    }

    /**
     * Sets the origins (e.g. {@code https://app.example.com}) accepted in the
     * response, the same values you have configured for your security framework.
     * If not set, the origin is deduced from the current request, which may be
     * wrong behind a TLS terminating proxy, so configure this in production.
     */
    public PasskeyReauthentication withAllowedOrigins(String... origins) {
        this.allowedOrigins = Set.of(origins);
        return this;
    }

    /**
     * How long the browser waits for the user (a hint for the browser). Defaults
     * to one minute.
     */
    public PasskeyReauthentication withTimeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    /**
     * Asks the user of the current UI to confirm their presence with one of their
     * passkeys and runs the action if that succeeds. Both callbacks run in the UI
     * thread. Exceptions thrown by the action go to Vaadin's error handler, like
     * from any other listener.
     *
     * @param action    the sensitive action
     * @param onFailure called instead of the action if the user could not be verified
     */
    public void runAfterVerification(Runnable action, Consumer<ReauthenticationException> onFailure) {
        start(UI.getCurrent(), action, onFailure);
    }

    /**
     * Asks the user of the current UI to confirm their presence with one of their
     * passkeys.
     *
     * @return a future that completes when the user has been verified, or fails
     * with a {@link ReauthenticationException}
     */
    public CompletableFuture<Void> verify() {
        return verify(UI.getCurrent());
    }

    /**
     * Asks the user of the given UI to confirm their presence with one of their
     * passkeys. Call this in the UI thread, so that {@link PasskeyStore} can find
     * the current user.
     *
     * @return a future that completes when the user has been verified, or fails
     * with a {@link ReauthenticationException}
     */
    public CompletableFuture<Void> verify(UI ui) {
        var result = new CompletableFuture<Void>();
        start(ui, () -> result.complete(null), result::completeExceptionally);
        return result;
    }

    private void start(UI ui, Runnable onSuccess, Consumer<ReauthenticationException> onFailure) {
        List<StoredPasskey> passkeys = passkeyStore.passkeysOfCurrentUser();
        if (passkeys.isEmpty()) {
            onFailure.accept(new ReauthenticationException("You have no passkeys to confirm the action with"));
            return;
        }
        String rpId = relyingPartyId != null ? relyingPartyId : requestHost();
        Set<Origin> origins = (allowedOrigins != null ? allowedOrigins : Set.of(requestOrigin())).stream()
                .map(Origin::new)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        byte[] challenge = new byte[32];
        RANDOM.nextBytes(challenge);
        String allowCredentials = passkeys.stream()
                .map(StoredPasskey::credentialIdBase64Url)
                .collect(Collectors.joining(","));

        ui.getPage().executeJs(BrowserScript.GET_ASSERTION,
                        BASE64URL.encodeToString(challenge), rpId, timeout.toMillis(), allowCredentials)
                .then(String.class, responseJson -> {
                    try {
                        verifyResponse(responseJson, passkeys, new ServerProperty(origins, rpId,
                                new DefaultChallenge(challenge)));
                    } catch (ReauthenticationException e) {
                        onFailure.accept(e);
                        return;
                    } catch (RuntimeException e) {
                        onFailure.accept(new ReauthenticationException("Passkey verification failed", e));
                        return;
                    }
                    onSuccess.run();
                }, browserError -> onFailure.accept(
                        // Typically NotAllowedError: the user cancelled or the authenticator
                        // could not verify the user
                        new ReauthenticationException("No passkey confirmation from the browser: " + browserError)));
    }

    private void verifyResponse(String responseJson, List<StoredPasskey> passkeys, ServerProperty serverProperty) {
        AuthenticationData response = WEB_AUTHN_MANAGER.parseAuthenticationResponseJSON(responseJson);
        StoredPasskey passkey = passkeys.stream()
                .filter(p -> Arrays.equals(p.credentialId(), response.getCredentialId()))
                .findFirst()
                .orElseThrow(() -> new ReauthenticationException("The passkey does not belong to the current user"));

        CredentialRecord credentialRecord = new CredentialRecordImpl(null, null, null, null,
                passkey.signatureCount(),
                new AttestedCredentialData(AAGUID.ZERO, passkey.credentialId(), passkey.coseKey()),
                null, null, null, null);
        var parameters = new AuthenticationParameters(serverProperty, credentialRecord,
                passkeys.stream().map(StoredPasskey::credentialId).toList(),
                true, // user verification (biometrics/PIN) required, this is the whole point
                true);
        // Throws VerificationException if the signature, challenge, origin, rpId,
        // user verification or the counter is not valid
        AuthenticationData verified = WEB_AUTHN_MANAGER.verify(response, parameters);
        passkeyStore.signatureCountUpdated(passkey, verified.getAuthenticatorData().getSignCount());
    }

    private static String requestHost() {
        String host = currentRequest().getHeader("Host");
        // strip the port, also from IPv6 addresses like [::1]:8080
        int portSeparator = host.lastIndexOf(':');
        return portSeparator > host.lastIndexOf(']') ? host.substring(0, portSeparator) : host;
    }

    private static String requestOrigin() {
        VaadinRequest request = currentRequest();
        return (request.isSecure() ? "https://" : "http://") + request.getHeader("Host");
    }

    private static VaadinRequest currentRequest() {
        VaadinRequest request = VaadinRequest.getCurrent();
        if (request == null) {
            throw new IllegalStateException("No current request to deduce the relying party id or origin from. "
                    + "Configure them with withRelyingPartyId and withAllowedOrigins.");
        }
        return request;
    }
}
