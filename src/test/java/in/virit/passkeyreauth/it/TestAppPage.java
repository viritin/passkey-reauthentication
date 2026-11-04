package in.virit.passkeyreauth.it;

import com.microsoft.playwright.Page;
import org.vaadin.addons.dramafinder.element.ButtonElement;
import org.vaadin.addons.dramafinder.element.DialogElement;
import org.vaadin.addons.dramafinder.element.NotificationElement;
import org.vaadin.addons.dramafinder.element.TextFieldElement;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Page object for the test app (RegistrationView and SensitiveActionView).
 */
public class TestAppPage {

    private static final Pattern COUNTER = Pattern.compile("Passkey signature counter: (\\d+)");

    private final Page page;
    private final String baseUrl;

    public TestAppPage(Page page, String baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    public TestAppPage open() {
        page.navigate(baseUrl);
        return this;
    }

    public void registerWithPasskey(String username) {
        TextFieldElement.getByLabel(page, "Username").setValue(username);
        ButtonElement.getByText(page, "Register with a passkey").click();
        assertLoggedInAs(username);
    }

    public void loginWithoutPasskey(String username) {
        TextFieldElement.getByLabel(page, "Username").setValue(username);
        ButtonElement.getByText(page, "Log in without a passkey").click();
        assertLoggedInAs(username);
    }

    public void loginWithPasskey() {
        ButtonElement.getByText(page, "Log in with a passkey").click();
    }

    public void logout() {
        ButtonElement.getByText(page, "Log out").click();
        assertThat(page.getByText("Passkey re-authentication test")).isVisible();
    }

    public void assertLoggedInAs(String username) {
        assertThat(page.getByText("Logged in as " + username)).isVisible();
    }

    /**
     * Clicks the transfer button and confirms in the dialog.
     */
    public void transfer() {
        startTransfer().confirm();
    }

    /**
     * Clicks the transfer button, which opens the confirmation dialog.
     */
    public ConfirmationDialog startTransfer() {
        ButtonElement.getByText(page, "Transfer 100 €").click();
        return new ConfirmationDialog();
    }

    public void transferWithoutDialog() {
        ButtonElement.getByText(page, "Transfer without confirmation dialog").click();
    }

    public void assertNoErrorNotification() {
        assertThat(page.locator("vaadin-notification-card[slot]")).hasCount(0);
    }

    public class ConfirmationDialog {
        private final DialogElement dialog = new DialogElement(page);

        ConfirmationDialog() {
            dialog.assertOpen();
            assertThat(dialog.getLocator().getByText("Transfer 100 €")).isVisible();
        }

        public void confirm() {
            ButtonElement.getByText(dialog.getLocator(), "Confirm with passkey").click();
            dialog.assertClosed();
        }

        public void cancel() {
            ButtonElement.getByText(dialog.getLocator(), "Cancel").click();
            dialog.assertClosed();
        }
    }

    public void assertTransferCount(int expected) {
        if (expected > 0) {
            assertThat(page.getByText("Transfer " + expected + " done")).isVisible();
        }
        assertThat(page.getByText(Pattern.compile("Transfer \\d+ done"))).hasCount(expected);
    }

    public void assertTransferCancelled(String reason) {
        NotificationElement.getByText(page, "Transfer cancelled: " + reason).assertOpen();
    }

    public long signatureCounter() {
        var text = page.getByText(COUNTER).textContent();
        var matcher = COUNTER.matcher(text);
        if (!matcher.find()) {
            throw new AssertionError("No signature counter in: " + text);
        }
        return Long.parseLong(matcher.group(1));
    }
}
