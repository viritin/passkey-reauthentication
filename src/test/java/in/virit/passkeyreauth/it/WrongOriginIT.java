package in.virit.passkeyreauth.it;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/**
 * The browser happily signs the assertion, but the server expects another
 * origin (e.g. a misconfiguration, or a response relayed from another site), so
 * the server side verification must reject it.
 */
@TestPropertySource(properties = "test.allowed-origin=https://app.example.com")
public class WrongOriginIT extends AbstractTestAppIT {

    @Test
    public void responseFromWrongOriginIsRejected() {
        VirtualAuthenticator.attachTo(page);
        var app = app();
        app.registerWithPasskey("frank");

        app.transfer();
        app.assertTransferCancelled("Passkey verification failed");
        app.assertTransferCount(0);
    }
}
