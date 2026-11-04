package in.virit.passkeyreauth;

import com.vaadin.flow.i18n.I18NProvider;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReauthenticationTextsTest {

    private static final Locale FI = Locale.of("fi");

    /** Returns the given value for every key, like some providers do for missing keys */
    private static I18NProvider provider(Map<String, String> translations, String missing) {
        return new I18NProvider() {
            @Override
            public List<Locale> getProvidedLocales() {
                return List.of(FI);
            }

            @Override
            public String getTranslation(String key, Locale locale, Object... params) {
                return translations.getOrDefault(key, missing.replace("{key}", key));
            }
        };
    }

    @Test
    void translationFromProviderIsUsed() {
        var provider = provider(Map.of("passkey-reauthentication.dialog.confirm", "Vahvista"), "{key}");
        assertEquals("Vahvista", ReauthenticationTexts.DIALOG_CONFIRM.get(provider, FI));
    }

    @Test
    void defaultIsUsedWithoutProvider() {
        assertEquals("Confirm with passkey", ReauthenticationTexts.DIALOG_CONFIRM.get(null, FI));
    }

    @Test
    void defaultIsUsedWhenProviderReturnsTheKey() {
        var provider = provider(Map.of(), "{key}");
        assertEquals("Cancel", ReauthenticationTexts.DIALOG_CANCEL.get(provider, FI));
    }

    @Test
    void defaultIsUsedWhenProviderMarksTheKeyMissing() {
        // DefaultI18NProvider style
        assertEquals("Cancel", ReauthenticationTexts.DIALOG_CANCEL.get(provider(Map.of(), "!fi: {key}"), FI));
        // another common style
        assertEquals("Cancel", ReauthenticationTexts.DIALOG_CANCEL.get(provider(Map.of(), "!{{key}}!"), FI));
    }

    @Test
    void defaultIsUsedForBlankTranslation() {
        var provider = provider(Map.of("passkey-reauthentication.dialog.cancel", " "), "{key}");
        assertEquals("Cancel", ReauthenticationTexts.DIALOG_CANCEL.get(provider, FI));
    }

    @Test
    void keysHaveCommonPrefix() {
        for (var text : ReauthenticationTexts.values()) {
            assertEquals(true, text.getKey().startsWith(ReauthenticationTexts.KEY_PREFIX), text.getKey());
        }
    }
}
