package dev.marshall.hounded.onboarding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.marshall.hounded.config.ConfigLoadException;
import dev.marshall.hounded.testing.PluginFixture;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FirstRoundMarkerTest {
    private PluginFixture fixture;
    private File dataFile;

    @BeforeEach
    void setUp() throws ConfigLoadException {
        fixture = PluginFixture.start();
        dataFile = new File(fixture.plugin().getDataFolder(), FirstRoundMarker.DATA_FILE);
    }

    @AfterEach
    void tearDown() {
        fixture.close();
    }

    private void markAndWaitForTheSave(FirstRoundMarker marker) {
        marker.markRoundPlayed();
        fixture.server().getScheduler().waitAsyncTasksFinished();
    }

    @Test
    void freshInstallHasNotPlayedARound() {
        assertFalse(new FirstRoundMarker(fixture.plugin()).hasPlayedARound());
    }

    @Test
    void playedRoundIsRememberedAfterARestart() {
        markAndWaitForTheSave(new FirstRoundMarker(fixture.plugin()));

        assertTrue(new FirstRoundMarker(fixture.plugin()).hasPlayedARound());
    }

    @Test
    void markingAgainDoesNotSaveAgain() {
        FirstRoundMarker marker = new FirstRoundMarker(fixture.plugin());
        markAndWaitForTheSave(marker);
        assertTrue(dataFile.delete());

        markAndWaitForTheSave(marker);

        assertFalse(dataFile.exists());
    }

    @Test
    void unreadableFileShowsTheGuideAgainAndSaysWhy() throws IOException {
        Files.writeString(dataFile.toPath(), "first-round-played: [not closed");
        List<LogRecord> warnings = new ArrayList<>();
        Handler capture = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record.getLevel() == Level.WARNING) {
                    warnings.add(record);
                }
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        };
        fixture.plugin().getLogger().addHandler(capture);

        FirstRoundMarker marker = new FirstRoundMarker(fixture.plugin());

        fixture.plugin().getLogger().removeHandler(capture);
        assertFalse(marker.hasPlayedARound());
        assertEquals(1, warnings.size());
        assertTrue(warnings.getFirst().getMessage().contains(dataFile.getPath()));
    }
}
