package in.virit.passkeyreauth.testapp;

import in.virit.passkeyreauth.PasskeyReauthentication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Optional;

/**
 * A minimal demo app for development and the browser tests: register a user
 * with a passkey ({@link RegistrationView}), then use the library in
 * {@link SensitiveActionView}. There is no security framework at all, the
 * "login" is just the username in the Vaadin session (see {@link DemoUsers}),
 * which shows the library only depends on knowing the current user's passkeys.
 * <p>
 * Run the main method and open http://localhost:9998 (WebAuthn needs a secure
 * context, which localhost is).
 */
@SpringBootApplication
public class TestServer {

    public static void main(String... args) {
        SpringApplication.run(TestServer.class, args);
    }

    /**
     * The relying party id and origin are deduced from the request by default.
     * The property is only for testing that a wrong origin is rejected.
     */
    @Bean
    PasskeyReauthentication passkeyReauthentication(DemoUsers users,
            @Value("${test.allowed-origin:#{null}}") Optional<String> allowedOrigin) {
        var reauthentication = new PasskeyReauthentication(users);
        allowedOrigin.ifPresent(reauthentication::withAllowedOrigins);
        return reauthentication;
    }
}
