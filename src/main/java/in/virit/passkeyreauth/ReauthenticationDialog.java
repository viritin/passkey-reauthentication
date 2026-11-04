package in.virit.passkeyreauth;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;

import java.util.function.Consumer;

/**
 * Tells the user what they are about to confirm, before the passkey prompt of
 * the browser/operating system opens. That prompt can't be customized (it
 * typically talks about "signing in"), so without this the user might not
 * understand why they are asked for their passkey.
 * <p>
 * Usually opened via
 * {@link PasskeyReauthentication#runAfterVerification(String, Runnable, Consumer)}.
 * The texts are localized with your Vaadin I18NProvider, see
 * {@link ReauthenticationTexts}. To change them for a single dialog, create it
 * yourself:
 * <pre>{@code
 * var dialog = new ReauthenticationDialog(reauthentication, "Transfer 100 € to Bob", action, onFailure);
 * dialog.getHint().setText("This transfer needs your fingerprint, face or device PIN.");
 * dialog.getConfirmButton().setText("Confirm transfer");
 * dialog.open();
 * }</pre>
 * The dialog is placed near the top of the page (5vh), so that it stays
 * visible while the passkey prompt is open in the middle. Change it with
 * {@link #setTop(String)}.
 */
public class ReauthenticationDialog extends Dialog {

    private final PasskeyReauthentication reauthentication;
    private final Runnable action;
    private final Consumer<ReauthenticationException> onFailure;

    private final Paragraph hint = new Paragraph(ReauthenticationTexts.DIALOG_HINT.get());
    private final Button confirmButton = new Button(ReauthenticationTexts.DIALOG_CONFIRM.get(), e -> confirm()) {{
        addThemeVariants(ButtonVariant.PRIMARY);
    }};
    private final Button cancelButton = new Button(ReauthenticationTexts.DIALOG_CANCEL.get(), e -> cancel());

    /**
     * @param reauthentication the configured re-authentication
     * @param description      what the user is about to confirm, e.g. "Transfer 100 € to Bob"
     * @param action           run after a successful verification
     * @param onFailure        called if the user cancels or could not be verified,
     *                         see {@link ReauthenticationException#isCancelledByUser()}
     */
    public ReauthenticationDialog(PasskeyReauthentication reauthentication, String description, Runnable action,
                                  Consumer<ReauthenticationException> onFailure) {
        this.reauthentication = reauthentication;
        this.action = action;
        this.onFailure = onFailure;
        setHeaderTitle(description);
        add(hint);
        getFooter().add(cancelButton, confirmButton);
        setCloseOnOutsideClick(false);
        // Near the top instead of the middle of the page, so that this explanation
        // stays visible below the passkey prompt the browser/OS opens in the middle
        setTop("5vh");
        // Esc
        addDialogCloseActionListener(e -> cancel());
    }

    private void confirm() {
        confirmButton.setEnabled(false);
        cancelButton.setEnabled(false);
        reauthentication.runAfterVerification(
                () -> {
                    close();
                    action.run();
                },
                e -> {
                    close();
                    onFailure.accept(e);
                });
    }

    private void cancel() {
        close();
        onFailure.accept(ReauthenticationException.cancelledByUser());
    }

    /**
     * @return the paragraph explaining what happens next, e.g. to change its text
     */
    public Paragraph getHint() {
        return hint;
    }

    public Button getConfirmButton() {
        return confirmButton;
    }

    public Button getCancelButton() {
        return cancelButton;
    }
}
