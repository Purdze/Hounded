package dev.marshall.hounded;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.marshall.hounded.config.ConfigLoadException;
import dev.marshall.hounded.testing.PluginFixture;
import java.io.File;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HoundedPluginTest {
    private PluginFixture fixture;

    @BeforeEach
    void setUp() throws ConfigLoadException {
        fixture = PluginFixture.start();
    }

    @AfterEach
    void tearDown() {
        fixture.close();
    }

    @Test
    void enablesAndWritesDefaultFiles() {
        assertTrue(fixture.plugin().isEnabled());
        assertTrue(new File(fixture.plugin().getDataFolder(), "config.yml").isFile());
        assertTrue(new File(fixture.plugin().getDataFolder(), "messages.yml").isFile());
    }

    @Test
    void disablingCancelsAllTasks() {
        fixture.startRound(fixture.addAdmin("Admin"), 30, fixture.server().addPlayer("Runner"));
        assertTrue(fixture.scheduledTaskCount() > 0);

        fixture.server().getPluginManager().disablePlugin(fixture.plugin());

        assertEquals(0, fixture.scheduledTaskCount());
    }
}
