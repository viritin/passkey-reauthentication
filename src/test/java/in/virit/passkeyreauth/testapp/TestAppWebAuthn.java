package in.virit.passkeyreauth.testapp;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinRequest;
import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.credential.CredentialRecordImpl;
import com.webauthn4j.data.AuthenticationData;
import com.webauthn4j.data.AuthenticationParameters;
import com.webauthn4j.data.PublicKeyCredentialParameters;
import com.webauthn4j.data.PublicKeyCredentialType;
import com.webauthn4j.data.RegistrationData;
import com.webauthn4j.data.RegistrationParameters;
import com.webauthn4j.data.attestation.authenticator.AAGUID;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.authenticator.COSEKey;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import in.virit.passkeyreauth.StoredPasskey;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;

/**
 * Passkey registration and login done directly with WebAuthn4J, for the test app
 * only. In a real app, your security framework (e.g. Spring Security or Quarkus)
 * does these.
 */
class TestAppWebAuthn {

    private static final WebAuthnManager WEB_AUTHN_MANAGER = WebAuthnManager.createNonStrictWebAuthnManager();
    private static final ObjectConverter OBJECT_CONVERTER = new ObjectConverter();
    private static final Base64.Encoder BASE64URL = Base64.getUrlEncoder().withoutPadding();
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String HELPERS = """
            const fromB64u = s => Uint8Array.from(
                atob(s.replace(/-/g, '+').replace(/_/g, '/').padEnd(Math.ceil(s.length / 4) * 4, '=')),
                c => c.charCodeAt(0));
            const toB64u = buffer => btoa(String.fromCharCode(...new Uint8Array(buffer)))
                .replace(/\\+/g, '-').replace(/\\//g, '_').replace(/=+$/, '');
            """;

    private static final String CREATE_SCRIPT = HELPERS + """
            return navigator.credentials.create({
                publicKey: {
                    challenge: fromB64u($0),
                    rp: {id: $1, name: 'Passkey re-authentication test'},
                    user: {id: fromB64u($2), name: $3, displayName: $3},
                    pubKeyCredParams: [{type: 'public-key', alg: -7}, {type: 'public-key', alg: -257}],
                    authenticatorSelection: {residentKey: 'required', userVerification: 'preferred'},
                    attestation: 'none'
                }
            }).then(credential => JSON.stringify({
                id: credential.id,
                rawId: toB64u(credential.rawId),
                type: credential.type,
                clientExtensionResults: credential.getClientExtensionResults(),
                response: {
                    clientDataJSON: toB64u(credential.response.clientDataJSON),
                    attestationObject: toB64u(credential.response.attestationObject),
                    transports: credential.response.getTransports()
                }
            }));
            """;

    // No allowCredentials: the browser offers any passkey of this site (they are
    // discoverable), and the server finds the user by the credential id
    private static final String GET_SCRIPT = HELPERS + """
            return navigator.credentials.get({
                publicKey: {challenge: fromB64u($0), rpId: $1, userVerification: 'preferred'}
            }).then(credential => JSON.stringify({
                id: credential.id,
                rawId: toB64u(credential.rawId),
                type: credential.type,
                clientExtensionResults: credential.getClientExtensionResults(),
                response: {
                    clientDataJSON: toB64u(credential.response.clientDataJSON),
                    authenticatorData: toB64u(credential.response.authenticatorData),
                    signature: toB64u(credential.response.signature),
                    userHandle: credential.response.userHandle ? toB64u(credential.response.userHandle) : null
                }
            }));
            """;

    private TestAppWebAuthn() {
    }

    static void register(String username, Consumer<StoredPasskey> onSuccess, Consumer<String> onError) {
        String host = VaadinRequest.getCurrent().getHeader("Host");
        String rpId = rpId(host);
        byte[] challenge = randomBytes(32);

        UI.getCurrent().getPage().executeJs(CREATE_SCRIPT,
                BASE64URL.encodeToString(challenge), rpId, BASE64URL.encodeToString(randomBytes(16)), username)
                .then(String.class, json -> {
                    RegistrationData registration = WEB_AUTHN_MANAGER.verifyRegistrationResponseJSON(json,
                            new RegistrationParameters(serverProperty(host, challenge),
                                    List.of(new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY,
                                                    COSEAlgorithmIdentifier.ES256),
                                            new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY,
                                                    COSEAlgorithmIdentifier.RS256)),
                                    false, true));
                    var authenticatorData = registration.getAttestationObject().getAuthenticatorData();
                    var credentialData = authenticatorData.getAttestedCredentialData();
                    onSuccess.accept(new StoredPasskey(credentialData.getCredentialId(),
                            OBJECT_CONVERTER.getCborConverter().writeValueAsBytes(credentialData.getCOSEKey()),
                            authenticatorData.getSignCount()));
                }, onError::accept);
    }

    /**
     * Logs in with any passkey known by {@link DemoUsers}.
     *
     * @param onSuccess called with the username
     */
    static void login(DemoUsers users, Consumer<String> onSuccess, Consumer<String> onError) {
        String host = VaadinRequest.getCurrent().getHeader("Host");
        byte[] challenge = randomBytes(32);

        UI.getCurrent().getPage().executeJs(GET_SCRIPT, BASE64URL.encodeToString(challenge), rpId(host))
                .then(String.class, json -> {
                    AuthenticationData response = WEB_AUTHN_MANAGER.parseAuthenticationResponseJSON(json);
                    var userPasskey = users.findByCredentialId(response.getCredentialId()).orElse(null);
                    if (userPasskey == null) {
                        onError.accept("Unknown passkey");
                        return;
                    }
                    var passkey = userPasskey.passkey();
                    var credentialRecord = new CredentialRecordImpl(null, null, null, null,
                            passkey.signatureCount(),
                            new AttestedCredentialData(AAGUID.ZERO, passkey.credentialId(),
                                    OBJECT_CONVERTER.getCborConverter().readValue(passkey.publicKeyCose(), COSEKey.class)),
                            null, null, null, null);
                    AuthenticationData verified = WEB_AUTHN_MANAGER.verify(response, new AuthenticationParameters(
                            serverProperty(host, challenge), credentialRecord, null, false, true));
                    users.signatureCountUpdated(passkey, verified.getAuthenticatorData().getSignCount());
                    onSuccess.accept(userPasskey.username());
                }, onError::accept);
    }

    private static String rpId(String host) {
        return host.replaceAll(":\\d+$", "");
    }

    private static ServerProperty serverProperty(String host, byte[] challenge) {
        return new ServerProperty(new Origin("http://" + host), rpId(host), new DefaultChallenge(challenge));
    }

    private static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return bytes;
    }
}
