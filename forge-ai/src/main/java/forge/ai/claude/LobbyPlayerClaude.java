package forge.ai.claude;

import java.util.Set;

import forge.ai.AIOption;
import forge.ai.LobbyPlayerAi;
import forge.game.Game;
import forge.game.player.Player;
import forge.game.player.PlayerController;

/**
 * LobbyPlayer implementation for Claude AI integration.
 * This creates ClaudePlayerController instances instead of regular AI controllers.
 */
public class LobbyPlayerClaude extends LobbyPlayerAi {
    
    static {
        System.out.println("[LobbyPlayerClaude] Class loaded!");
    }
    
    public LobbyPlayerClaude(String name, Set<AIOption> options) {
        super(name, options);
        System.out.println("[LobbyPlayerClaude] Constructor called for: " + name);
        // Set profile to indicate this is Claude
        setAiProfile("Claude");
    }
    
    /**
     * Creates a ClaudePlayerController instead of the regular PlayerControllerAi.
     */
    private PlayerController createControllerFor(Player player) {
        System.out.println("[LobbyPlayerClaude] Creating ClaudePlayerController for player: " + player.getName());
        return new ClaudePlayerController(player.getGame(), player, this);
    }
    
    @Override
    public PlayerController createMindSlaveController(Player master, Player slave) {
        return createControllerFor(slave);
    }
    
    @Override
    public Player createIngamePlayer(Game game, final int id) {
        System.out.println("[LobbyPlayerClaude] createIngamePlayer called for: " + getName() + " (ID: " + id + ")");
        Player ai = new Player(getName(), game, id);
        PlayerController controller = createControllerFor(ai);
        System.out.println("[LobbyPlayerClaude] Created controller: " + controller.getClass().getName());
        ai.setFirstController(controller);
        System.out.println("[LobbyPlayerClaude] Player setup complete for: " + ai.getName());
        return ai;
    }
}