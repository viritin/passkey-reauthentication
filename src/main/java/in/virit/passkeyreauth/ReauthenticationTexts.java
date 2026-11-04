package in.virit.passkeyreauth;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.server.VaadinService;

import java.util.Locale;

/**
 * The user visible texts of the library: the {@link ReauthenticationDialog} and
 * the messages of {@link ReauthenticationException}. The English defaults are
 * used unless your Vaadin {@link I18NProvider} has a translation for the key,
 * so you can localize or override them the standard Vaadin way, e.g. in
 * {@code src/main/resources/vaadin-i18n/translations_fi.properties}:
 * <pre>
 * passkey-reauthentication.dialog.hint=Vahvista passkeylläsi. Laitteesi kysyy sormenjälkeä, kasvoja tai PIN-koodia.
 * passkey-reauthentication.dialog.confirm=Vahvista passkeyllä
 * passkey-reauthentication.dialog.cancel=Peruuta
 * passkey-reauthentication.error.no-passkeys=Sinulla ei ole passkeytä, jolla vahvistaa toiminto
 * passkey-reauthentication.error.not-confirmed=Passkeytä ei vahvistettu
 * passkey-reauthentication.error.verification-failed=Passkeyn tarkistus epäonnistui
 * passkey-reauthentication.error.foreign-passkey=Passkey ei ole sinun
 * passkey-reauthentication.error.cancelled=Peruutettu
 * </pre>
 * The dialog texts can also be changed per dialog, see {@link ReauthenticationDialog}.
 */
public enum ReauthenticationTexts {

    DIALOG_HINT("dialog.hint",
            "Confirm this with your passkey. Your device will ask for your fingerprint, face or PIN."),
    DIALOG_CONFIRM("dialog.confirm", "Confirm with passkey"),
    DIALOG_CANCEL("dialog.cancel", "Cancel"),
    NO_PASSKEYS("error.no-passkeys", "You have no passkeys to confirm the action with"),
    /** The browser did not return a passkey: the user cancelled, or could not be verified. */
    NOT_CONFIRMED("error.not-confirmed", "The action was not confirmed with a passkey"),
    VERIFICATION_FAILED("error.verification-failed", "Passkey verification failed"),
    FOREIGN_PASSKEY("error.foreign-passkey", "The passkey does not belong to the current user"),
    CANCELLED("error.cancelled", "Cancelled");

    /** The prefix of all translation keys */
    public static final String KEY_PREFIX = "passkey-reauthentication.";

    private final String key;
    private final String defaultText;

    ReauthenticationTexts(String key, String defaultText) {
        this.key = KEY_PREFIX + key;
        this.defaultText = defaultText;
    }

    /**
     * @return the translation key, e.g. {@code passkey-reauthentication.dialog.confirm}
     */
    public String getKey() {
        return key;
    }

    public String getDefaultText() {
        return defaultText;
    }

    /**
     * @return the translation for the locale of the current UI from the
     * {@link I18NProvider} of the application, or the English default if there
     * is none
     */
    public String get() {
        VaadinService service = VaadinService.getCurrent();
        I18NProvider provider = service == null ? null : service.getInstantiator().getI18NProvider();
        UI ui = UI.getCurrent();
        return get(provider, ui != null ? ui.getLocale() : Locale.getDefault());
    }

    String get(I18NProvider provider, Locale locale) {
        if (provider != null) {
            String translation = provider.getTranslation(key, locale);
            // Providers report missing keys differently, e.g. the key itself or "!fi: key"
            if (translation != null && !translation.isBlank() && !translation.contains(key)) {
                return translation;
            }
        }
        return defaultText;
    }
}
