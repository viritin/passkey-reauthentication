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
import in.virit.passkeyreauth.ReauthenticationDialog;
import in.virit.passkeyreauth.ReauthenticationException;

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
                    new Button("Transfer without confirmation dialog", e -> transferWithoutDialog()),
                    new Button("Transfer with custom dialog texts", e -> transferWithCustomTexts()),
                    new Button("Log out", e -> {
                        users.logout();
                        UI.getCurrent().navigate(RegistrationView.class);
                    })));
            add(transferLog);
            passkeys.refresh();
        }, () -> event.forwardTo(RegistrationView.class));
    }

    private void transfer() {
        // Shows a dialog telling what is being confirmed before the passkey prompt
        reauthentication.runAfterVerification("Transfer 100 €", this::doTransfer, this::showFailure);
    }

    private void transferWithCustomTexts() {
        // Texts of a single dialog overridden (all can be localized via I18NProvider)
        var dialog = new ReauthenticationDialog(reauthentication, "Transfer 100 € (custom)",
                this::doTransfer, this::showFailure);
        dialog.getHint().setText("Custom hint: confirm the transfer with your passkey.");
        dialog.getConfirmButton().setText("Approve transfer");
        dialog.getCancelButton().setText("Not now");
        dialog.open();
    }

    private void transferWithoutDialog() {
        // Asks for the passkey right away
        reauthentication.runAfterVerification(this::doTransfer, this::showFailure);
    }

    private void doTransfer() {
        transferLog.logTransfer();
        passkeys.refresh(); // shows the updated signature counter
    }

    private void showFailure(ReauthenticationException e) {
        if (!e.isCancelledByUser()) {
            Notification.show("Transfer cancelled: " + e.getMessage())
                    .addThemeVariants(NotificationVariant.ERROR);
        }
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
