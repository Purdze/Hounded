package dev.marshall.hounded.onboarding;

import static dev.marshall.hounded.testing.PluginFixture.messagesOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import dev.marshall.hounded.config.ConfigKey;
import dev.marshall.hounded.config.ConfigLoadException;
import dev.marshall.hounded.config.MessageKey;
import dev.marshall.hounded.testing.PluginFixture;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/** The guide through real joins on a fresh install. */
class QuickStartGuideTest {
    private PluginFixture fixture;

    @BeforeEach
    void setUp() throws ConfigLoadException {
        fixture = PluginFixture.start();
    }

    @AfterEach
    void tearDown() {
        fixture.close();
    }

    /**
     * An op joining the server. MockBukkit can only grant op after a player is added, so the join
     * that matters is a reconnect with op already set.
     */
    private PlayerMock adminJoins(String name) {
        PlayerMock admin = fixture.addAdmin(name);
        admin.disconnect();
        admin.reconnect();
        return admin;
    }

    private void waitForTheGuide() {
        fixture.server().getScheduler().performTicks(QuickStartGuide.DELAY_TICKS);
    }

    private String guide() {
        return fixture.chat(MessageKey.QUICK_START_GUIDE);
    }

    @Test
    void adminIsShownTheGuideShortlyAfterJoining() {
        PlayerMock admin = adminJoins("Admin");

        fixture.server().getScheduler().performTicks(QuickStartGuide.DELAY_TICKS - 1);
        assertFalse(messagesOf(admin).contains(guide()));

        fixture.server().getScheduler().performTicks(1);
        assertEquals(List.of(guide()), messagesOf(admin));
    }

    @Test
    void playersWithoutAdminAreNotShownTheGuide() {
        PlayerMock player = fixture.server().addPlayer("Player");

        waitForTheGuide();

        assertEquals(List.of(), messagesOf(player));
    }

    @Test
    void guideStopsOnceARoundHasStarted() {
        PlayerMock admin = fixture.addAdmin("Admin");
        fixture.startRound(admin, 0, fixture.server().addPlayer("Runner"));

        PlayerMock laterAdmin = adminJoins("LaterAdmin");
        waitForTheGuide();

        assertFalse(messagesOf(laterAdmin).contains(guide()));
    }

    @Test
    void guideCanBeTurnedOff() throws IOException {
        PlayerMock admin = fixture.addAdmin("Admin");
        fixture.reloadWith(admin, ConfigKey.QUICK_START_GUIDE, false);
        admin.disconnect();

        admin.reconnect();
        waitForTheGuide();

        assertFalse(messagesOf(admin).contains(guide()));
    }

    @Test
    void adminWhoLeavesBeforeTheDelayIsNotSentAnything() {
        PlayerMock admin = adminJoins("Admin");
        admin.disconnect();

        waitForTheGuide();

        assertEquals(List.of(), messagesOf(admin));
    }
}
