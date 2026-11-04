package in.virit.passkeyreauth.it;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Test;
import org.vaadin.addons.dramafinder.element.ButtonElement;
import org.vaadin.addons.dramafinder.element.DialogElement;

import java.nio.file.Path;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The texts of the library come from the I18NProvider of the app when it has
 * them (here src/test/resources/vaadin-i18n/translations_fi.properties), and
 * the dialog is placed near the top of the page.
 */
public class LocalizationAndLayoutIT extends AbstractTestAppIT {

    @Test
    public void dialogIsLocalizedAndNearTheTop() {
        // A browser preferring Finnish
        page.close();
        page = getBrowser().newContext(new Browser.NewContextOptions()
                .setLocale("fi-FI")
                .setViewportSize(1280, 800)).newPage();
        VirtualAuthenticator.attachTo(page);
        var app = app().open();
        app.registerWithPasskey("pekka");

        ButtonElement.getByText(page, "Transfer 100 €").click();
        var dialog = new DialogElement(page);
        dialog.assertOpen();
        assertThat(dialog.getLocator().getByText("Laitteesi kysyy sormenjälkeä")).isVisible();
        var confirm = ButtonElement.getByText(dialog.getLocator(), "Vahvista passkeyllä");
        assertThat(ButtonElement.getByText(dialog.getLocator(), "Peruuta").getLocator()).isVisible();

        // Near the top, horizontally centered, so that the passkey prompt of the
        // browser/OS in the middle of the page does not hide it
        var bounds = page.locator("vaadin-dialog [part='overlay']").boundingBox();
        assertTrue(bounds.y < 800 * 0.1, "Dialog should be near the top, but is at y=" + bounds.y);
        double centerX = bounds.x + bounds.width / 2;
        assertTrue(Math.abs(centerX - 640) < 20, "Dialog should be centered, but its center is at x=" + centerX);
        page.waitForTimeout(500); // the opening animation, for a nicer screenshot
        page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("target/reauthentication-dialog.png")));

        confirm.click();
        dialog.assertClosed();
        app.assertTransferCount(1);
    }

    @Test
    public void englishDefaultsWithoutTranslations() {
        VirtualAuthenticator.attachTo(page);
        var app = app().open();
        app.registerWithPasskey("john");

        ButtonElement.getByText(page, "Transfer 100 €").click();
        var dialog = new DialogElement(page);
        assertThat(dialog.getLocator().getByText("Your device will ask for your fingerprint")).isVisible();
        assertThat(ButtonElement.getByText(dialog.getLocator(), "Cancel").getLocator()).isVisible();
        ButtonElement.getByText(dialog.getLocator(), "Confirm with passkey").click();
        app.assertTransferCount(1);
    }

    @Test
    public void textsCanBeOverriddenPerDialog() {
        VirtualAuthenticator.attachTo(page);
        var app = app().open();
        app.registerWithPasskey("kate");

        ButtonElement.getByText(page, "Transfer with custom dialog texts").click();
        var dialog = new DialogElement(page);
        dialog.assertOpen();
        assertThat(dialog.getLocator().getByText("Transfer 100 € (custom)")).isVisible();
        assertThat(dialog.getLocator().getByText("Custom hint: confirm the transfer")).isVisible();
        ButtonElement.getByText(dialog.getLocator(), "Not now").click();
        dialog.assertClosed();
        app.assertNoErrorNotification();

        ButtonElement.getByText(page, "Transfer with custom dialog texts").click();
        ButtonElement.getByText(new DialogElement(page).getLocator(), "Approve transfer").click();
        app.assertTransferCount(1);
    }

    @Test
    public void errorMessagesAreLocalized() {
        page.close();
        page = getBrowser().newContext(new Browser.NewContextOptions().setLocale("fi-FI")).newPage();
        var app = app().open();
        app.loginWithoutPasskey("liisa");

        ButtonElement.getByText(page, "Transfer 100 €").click();
        ButtonElement.getByText(new DialogElement(page).getLocator(), "Vahvista passkeyllä").click();
        app.assertTransferCancelled("Sinulla ei ole passkeytä");
    }
}
