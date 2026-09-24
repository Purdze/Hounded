package dev.marshall.hounded.command;

import static dev.marshall.hounded.testing.PluginFixture.messagesOf;
import static dev.marshall.hounded.testing.PluginFixture.run;
import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.marshall.hounded.config.ConfigLoadException;
import dev.marshall.hounded.config.MessageKey;
import dev.marshall.hounded.config.PlaceholderNames;
import dev.marshall.hounded.game.Role;
import dev.marshall.hounded.testing.PluginFixture;
import java.util.List;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class HoundedCommandTest {
    private PluginFixture fixture;
    private CommandReplies replies;
    private PlayerMock admin;
    private PlayerMock steve;

    @BeforeEach
    void setUp() throws ConfigLoadException {
        fixture = PluginFixture.start();
        replies = new CommandReplies(fixture.config());
        admin = fixture.addAdmin("Admin");
        steve = fixture.server().addPlayer("Steve");
    }

    @AfterEach
    void tearDown() {
        fixture.close();
    }

    private String roleMessage(MessageKey key, PlayerMock player, Role role) {
        return fixture.aboutPlayer(key, player, replies.roleNames(role));
    }

    private String roleMessage(MessageKey key, Role role, TagResolver... extra) {
        return fixture.chat(key, TagResolver.resolver(replies.roleNames(role), TagResolver.resolver(extra)));
    }

    @Test
    void helpIsShownToEveryone() {
        assertEquals(List.of(fixture.chat(MessageKey.HELP)), run(steve, "hounded"));
        assertEquals(List.of(fixture.chat(MessageKey.HELP)), run(steve, "hounded help"));
    }

    @Test
    void addAssignsTheRole() {
        assertEquals(
                List.of(roleMessage(MessageKey.ROLE_ASSIGNED, steve, Role.RUNNER)),
                run(admin, "hounded runner add Steve"));
    }

    @Test
    void listShowsPlayersInAssignmentOrder() {
        run(admin, "hounded runner add Steve");
        run(admin, "hounded runner add Admin");

        assertEquals(
                List.of(roleMessage(
                        MessageKey.ROLE_LIST,
                        Role.RUNNER,
                        Placeholder.unparsed(PlaceholderNames.PLAYERS, "Steve, Admin"))),
                run(admin, "hounded runner list"));
    }

    @Test
    void listWithNobodySaysSo() {
        assertEquals(List.of(roleMessage(MessageKey.ROLE_LIST_EMPTY, Role.HUNTER)), run(admin, "hounded hunter list"));
    }

    @Test
    void removeTakesAwayTheRole() {
        run(admin, "hounded runner add Steve");

        assertEquals(
                List.of(roleMessage(MessageKey.ROLE_REMOVED, steve, Role.RUNNER)),
                run(admin, "hounded runner remove Steve"));
        assertEquals(List.of(roleMessage(MessageKey.ROLE_LIST_EMPTY, Role.RUNNER)), run(admin, "hounded runner list"));
    }

    @Test
    void removingARoleThePlayerDoesNotHaveIsRefused() {
        run(admin, "hounded hunter add Steve");

        assertEquals(
                List.of(roleMessage(MessageKey.ROLE_NOT_ASSIGNED, steve, Role.RUNNER)),
                run(admin, "hounded runner remove Steve"));
    }

    @Test
    void clearRemovesEveryoneWithTheRole() {
        run(admin, "hounded hunter add Steve");
        run(admin, "hounded hunter add Admin");

        assertEquals(List.of(roleMessage(MessageKey.ROLE_CLEARED, Role.HUNTER)), run(admin, "hounded hunter clear"));
        assertEquals(List.of(roleMessage(MessageKey.ROLE_LIST_EMPTY, Role.HUNTER)), run(admin, "hounded hunter list"));
    }

    @Test
    void startNeedsARunnerAndAHunter() {
        assertEquals(List.of(fixture.chat(MessageKey.START_NO_RUNNERS)), run(admin, "hounded start 0"));
        run(admin, "hounded runner add Steve");
        assertEquals(List.of(fixture.chat(MessageKey.START_NO_HUNTERS)), run(admin, "hounded start 0"));
    }

    @Test
    void startWithoutHeadstartReleasesHuntersForEveryone() {
        fixture.assignRoles(admin, steve);
        messagesOf(steve);

        assertEquals(
                fixture.chat(MessageKey.START_RELEASED),
                run(admin, "hounded start 0").getFirst());
        assertEquals(List.of(fixture.chat(MessageKey.START_RELEASED)), messagesOf(steve));
    }

    @Test
    void startingTwiceIsRefused() {
        fixture.startRound(admin, 0, steve);

        assertEquals(List.of(fixture.chat(MessageKey.START_ALREADY_RUNNING)), run(admin, "hounded start 0"));
    }

    @Test
    void rolesAreLockedDuringARound() {
        fixture.startRound(admin, 0, steve);

        assertEquals(List.of(fixture.chat(MessageKey.ROLE_LOCKED)), run(admin, "hounded hunter add Steve"));
    }

    @Test
    void stopEndsTheRoundAndReturnsToTheLobby() {
        fixture.startRound(admin, 0, steve);

        assertEquals(List.of(fixture.chat(MessageKey.STOP_STOPPED)), run(admin, "hounded stop"));
        assertEquals(
                List.of(roleMessage(MessageKey.ROLE_ASSIGNED, steve, Role.HUNTER)),
                run(admin, "hounded hunter add Steve"));
    }

    @Test
    void stopWithoutARoundIsRefused() {
        assertEquals(List.of(fixture.chat(MessageKey.STOP_NOT_RUNNING)), run(admin, "hounded stop"));
    }

    @Test
    void reloadReportsSuccess() {
        assertEquals(List.of(fixture.chat(MessageKey.RELOAD_SUCCESS)), run(admin, "hounded reload"));
    }

    @Test
    void playersWithoutAdminCannotUseAdminCommands() {
        run(steve, "hounded runner add Steve");

        assertEquals(List.of(roleMessage(MessageKey.ROLE_LIST_EMPTY, Role.RUNNER)), run(admin, "hounded runner list"));
    }
}
