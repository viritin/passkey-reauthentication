package in.virit.passkeyreauth.it;

import in.virit.passkeyreauth.testapp.TestServer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.vaadin.addons.dramafinder.AbstractBasePlaywrightIT;

/**
 * Starts the test app ({@link TestServer}) on a random port and gives each test
 * a fresh Playwright page (headless by default, add -Dheadless=false to watch).
 * The relying party id and origin are deduced from the request, so a random
 * port works fine.
 */
@SpringBootTest(classes = TestServer.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractTestAppIT extends AbstractBasePlaywrightIT {

    @LocalServerPort
    int port;

    @Override
    public String getUrl() {
        return "http://localhost:" + port + "/";
    }

    protected TestAppPage app() {
        return new TestAppPage(page, getUrl());
    }
}
