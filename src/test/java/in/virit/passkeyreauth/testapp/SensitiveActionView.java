package in.virit.passkeyreauth.testapp;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import in.virit.passkeyreauth.PasskeyReauthentication;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * The library in use: the transfer only happens after the user has confirmed it
 * with one of their passkeys.
 */
@Route("sensitive-action")
public class SensitiveActionView extends VerticalLayout implements BeforeEnterObserver {

    private final DemoUsers users;
    private final PasskeyReauthentication reauthentication;
    private final Passkeys passkeys = new Passkeys();
    private final TransferLog transferLog = new TransferLog();

    public SensitiveActionView(DemoUsers users, PasskeyReauthentication reauthentication) {
        this.users = users;
        this.reauthentication = reauthentication;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        users.currentUser().ifPresentOrElse(username -> {
            removeAll();
            add(new H2("Logged in as " + username));
            add(passkeys);
            add(new HorizontalLayout(
                    new Button("Transfer 100 €", e -> transfer()) {{
                        addThemeVariants(ButtonVariant.PRIMARY);
                    }},
                    new Button("Log out", e -> {
                        users.logout();
                        UI.getCurrent().navigate(RegistrationView.class);
                    })));
            add(transferLog);
            passkeys.refresh();
        }, () -> event.forwardTo(RegistrationView.class));
    }

    private void transfer() {
        reauthentication.runAfterVerification(
                () -> {
                    transferLog.logTransfer();
                    passkeys.refresh(); // shows the updated signature counter
                },
                e -> Notification.show("Transfer cancelled: " + e.getMessage())
                        .addThemeVariants(NotificationVariant.ERROR));
    }

    class Passkeys extends VerticalLayout {
        Passkeys() {
            setPadding(false);
            setSpacing(false);
        }

        void refresh() {
            removeAll();
            var list = users.passkeysOfCurrentUser();
            if (list.isEmpty()) {
                add(new Paragraph("No passkeys"));
            }
            list.forEach(passkey -> add(new Paragraph("Passkey signature counter: " + passkey.signatureCount())));
        }
    }

    static class TransferLog extends VerticalLayout {
        private final AtomicInteger transfers = new AtomicInteger();

        TransferLog() {
            setPadding(false);
            setSpacing(false);
        }

        void logTransfer() {
            add(new Paragraph("Transfer " + transfers.incrementAndGet() + " done"));
        }
    }
}
