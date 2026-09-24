package dev.marshall.hounded.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.marshall.hounded.config.ConfigLoadException;
import dev.marshall.hounded.config.MessageKey;
import dev.marshall.hounded.game.GameSession;
import dev.marshall.hounded.testing.MutableClock;
import dev.marshall.hounded.testing.PluginFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HoundedExpansionTest {
    private PluginFixture fixture;
    private HoundedExpansion expansion;

    @BeforeEach
    void setUp() throws ConfigLoadException {
        fixture = PluginFixture.start();
        GameSession session = new GameSession(new MutableClock());
        expansion = new HoundedExpansion(fixture.plugin().getPluginMeta(), fixture.placeholderResolverFor(session));
    }

    @AfterEach
    void tearDown() {
        fixture.close();
    }

    @Test
    void identifiesAsHoundedWithThePluginsVersion() {
        assertEquals("hounded", expansion.getIdentifier());
        assertEquals(fixture.plugin().getPluginMeta().getVersion(), expansion.getVersion());
    }

    @Test
    void survivesPlaceholderApiReloads() {
        assertTrue(expansion.persist());
    }

    @Test
    void answersKnownPlaceholdersAndLeavesOthersToPlaceholderApi() {
        assertEquals(fixture.text(MessageKey.STATE_LOBBY), expansion.onRequest(null, "state"));
        assertNull(expansion.onRequest(null, "nonsense"));
    }
}
