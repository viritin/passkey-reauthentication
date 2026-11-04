package in.virit.passkeyreauth.testapp;

import com.vaadin.flow.server.VaadinSession;
import in.virit.passkeyreauth.PasskeyStore;
import in.virit.passkeyreauth.StoredPasskey;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory users and their passkeys, and a "login" that just stores the
 * username in the Vaadin session. In a real app, this would be your security
 * framework and database.
 */
@Component
public class DemoUsers implements PasskeyStore {

    private static final String USERNAME = DemoUsers.class.getName() + ".username";

    private final Map<String, List<StoredPasskey>> passkeys = new ConcurrentHashMap<>();

    public void login(String username) {
        passkeys.putIfAbsent(username, new CopyOnWriteArrayList<>());
        VaadinSession.getCurrent().setAttribute(USERNAME, username);
    }

    public void logout() {
        VaadinSession.getCurrent().setAttribute(USERNAME, null);
    }

    public Optional<String> currentUser() {
        return Optional.ofNullable((String) VaadinSession.getCurrent().getAttribute(USERNAME));
    }

    public void addPasskey(String username, StoredPasskey passkey) {
        passkeys.computeIfAbsent(username, u -> new CopyOnWriteArrayList<>()).add(passkey);
    }

    public Optional<UserPasskey> findByCredentialId(byte[] credentialId) {
        return passkeys.entrySet().stream()
                .flatMap(e -> e.getValue().stream().map(p -> new UserPasskey(e.getKey(), p)))
                .filter(up -> Arrays.equals(up.passkey().credentialId(), credentialId))
                .findFirst();
    }

    public record UserPasskey(String username, StoredPasskey passkey) {
    }

    @Override
    public List<StoredPasskey> passkeysOfCurrentUser() {
        return currentUser().map(passkeys::get).map(List::copyOf).orElse(List.of());
    }

    @Override
    public void signatureCountUpdated(StoredPasskey passkey, long newSignatureCount) {
        passkeys.values().forEach(list -> list.replaceAll(p -> Arrays.equals(p.credentialId(), passkey.credentialId())
                ? new StoredPasskey(p.credentialId(), p.publicKeyCose(), newSignatureCount)
                : p));
    }
}
