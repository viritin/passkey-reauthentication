package in.virit.passkeyreauth.it;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReauthenticationIT extends AbstractTestAppIT {

    @Test
    public void actionRunsAfterVerification() {
        VirtualAuthenticator.attachTo(page);
        var app = app();
        app.registerWithPasskey("alice");
        long counterBefore = app.signatureCounter();

        app.transfer();
        app.assertTransferCount(1);
        // The new signature counter was handed to the PasskeyStore
        assertTrue(app.signatureCounter() > counterBefore);

        app.transfer();
        app.assertTransferCount(2);
    }

    @Test
    public void actionRunsAfterVerificationWithoutDialog() {
        VirtualAuthenticator.attachTo(page);
        var app = app();
        app.registerWithPasskey("heidi");

        app.transferWithoutDialog();
        app.assertTransferCount(1);
    }

    @Test
    public void cancellingTheDialogDoesNotRunTheAction() {
        VirtualAuthenticator.attachTo(page);
        var app = app();
        app.registerWithPasskey("ivan");
        long counterBefore = app.signatureCounter();

        app.startTransfer().cancel();
        app.assertNoErrorNotification();
        app.assertTransferCount(0);
        // The passkey was not even asked for
        assertEquals(counterBefore, app.signatureCounter());
    }

    @Test
    public void loginWithPasskeyThenConfirmAction() {
        VirtualAuthenticator.attachTo(page);
        var app = app();
        app.registerWithPasskey("grace");
        app.logout();

        app.loginWithPasskey();
        app.assertLoggedInAs("grace");
        app.transfer();
        app.assertTransferCount(1);
    }

    @Test
    public void actionIsCancelledIfUserIsNotVerified() {
        var authenticator = VirtualAuthenticator.attachTo(page);
        var app = app();
        app.registerWithPasskey("bob");

        // Somebody else at the keyboard: the fingerprint/PIN check fails
        authenticator.setUserVerified(false);
        app.transfer();
        app.assertTransferCancelled("");
        app.assertTransferCount(0);

        // The real user again
        authenticator.setUserVerified(true);
        app.transfer();
        app.assertTransferCount(1);
    }

    @Test
    public void userWithoutPasskeysCannotConfirm() {
        var app = app();
        app.loginWithoutPasskey("carol");

        app.transfer();
        app.assertTransferCancelled("You have no passkeys");
        app.assertTransferCount(0);
    }

    @Test
    public void passkeyOfAnotherUserIsNotAccepted() {
        var authenticator = VirtualAuthenticator.attachTo(page);
        var app = app();
        app.registerWithPasskey("dave");
        String davesPasskey = authenticator.credentialIds().getFirst();
        app.logout();
        app.registerWithPasskey("erin");
        assertEquals(2, authenticator.credentialIds().size());
        app.logout();

        // Dave's own passkey is gone from the device, only Erin's is left
        authenticator.removeCredential(davesPasskey);
        app.loginWithoutPasskey("dave");
        app.transfer();
        app.assertTransferCancelled("");
        app.assertTransferCount(0);
    }
}
