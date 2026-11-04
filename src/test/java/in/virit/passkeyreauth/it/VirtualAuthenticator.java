package in.virit.passkeyreauth.it;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.microsoft.playwright.CDPSession;
import com.microsoft.playwright.Page;

import java.util.List;
import java.util.stream.StreamSupport;

/**
 * A software authenticator ("security key" with biometrics) for Chromium, via
 * the WebAuthn domain of the Chrome DevTools Protocol. Once attached,
 * navigator.credentials.create/get calls on the page are answered by this
 * authenticator without any user interaction.
 */
public class VirtualAuthenticator {

    private final CDPSession cdp;
    private final String authenticatorId;

    private VirtualAuthenticator(Page page) {
        cdp = page.context().newCDPSession(page);
        cdp.send("WebAuthn.enable");

        var options = new JsonObject();
        options.addProperty("protocol", "ctap2");
        options.addProperty("transport", "internal");
        options.addProperty("hasResidentKey", true);
        options.addProperty("hasUserVerification", true);
        options.addProperty("isUserVerified", true);
        options.addProperty("automaticPresenceSimulation", true);
        var params = new JsonObject();
        params.add("options", options);

        authenticatorId = cdp.send("WebAuthn.addVirtualAuthenticator", params)
                .get("authenticatorId").getAsString();
    }

    public static VirtualAuthenticator attachTo(Page page) {
        return new VirtualAuthenticator(page);
    }

    /**
     * Simulates whether the user passes the verification (fingerprint, face or
     * device PIN) of the authenticator.
     */
    public void setUserVerified(boolean userVerified) {
        var params = params();
        params.addProperty("isUserVerified", userVerified);
        cdp.send("WebAuthn.setUserVerified", params);
    }

    /**
     * @return the ids of the credentials (passkeys) stored in this authenticator
     */
    public List<String> credentialIds() {
        return StreamSupport.stream(cdp.send("WebAuthn.getCredentials", params())
                        .getAsJsonArray("credentials").spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .map(credential -> credential.get("credentialId").getAsString())
                .toList();
    }

    /**
     * Removes a passkey from this authenticator (but not from the server).
     */
    public void removeCredential(String credentialId) {
        var params = params();
        params.addProperty("credentialId", credentialId);
        cdp.send("WebAuthn.removeCredential", params);
    }

    private JsonObject params() {
        var params = new JsonObject();
        params.addProperty("authenticatorId", authenticatorId);
        return params;
    }
}
