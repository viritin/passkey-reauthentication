package in.virit.passkeyreauth.testapp;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

/**
 * Sign up with a passkey (or without, to test that case too), or log in with a
 * passkey registered earlier. In this test app, signing up also logs you in.
 */
@Route("")
public class RegistrationView extends VerticalLayout {

    private final DemoUsers users;
    private final TextField username = new TextField("Username") {{
        setRequired(true);
    }};

    public RegistrationView(DemoUsers users) {
        this.users = users;
        add(new H2("Passkey re-authentication test"));
        add(username);
        add(new HorizontalLayout(
                new Button("Register with a passkey", e -> register()) {{
                    addThemeVariants(ButtonVariant.PRIMARY);
                }},
                new Button("Log in without a passkey", e -> loginAndContinue(username.getValue()))));
        add(new H3("Already registered?"));
        add(new Button("Log in with a passkey", e -> loginWithPasskey()));
    }

    private void register() {
        TestAppWebAuthn.register(username.getValue(),
                passkey -> {
                    users.addPasskey(username.getValue(), passkey);
                    loginAndContinue(username.getValue());
                },
                error -> Notification.show("Registration failed: " + error)
                        .addThemeVariants(NotificationVariant.ERROR));
    }

    private void loginWithPasskey() {
        TestAppWebAuthn.login(users, this::loginAndContinue,
                error -> Notification.show("Login failed: " + error)
                        .addThemeVariants(NotificationVariant.ERROR));
    }

    private void loginAndContinue(String username) {
        users.login(username);
        UI.getCurrent().navigate(SensitiveActionView.class);
    }
}
