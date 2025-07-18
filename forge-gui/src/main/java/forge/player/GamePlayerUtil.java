package forge.player;

import forge.LobbyPlayer;
import forge.ai.AIOption;
import forge.ai.AiProfileUtil;
import forge.ai.LobbyPlayerAi;
import forge.gui.GuiBase;
import forge.gui.util.SOptionPane;
import forge.localinstance.properties.ForgeNetPreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.util.GuiDisplayUtil;
import forge.util.Localizer;
import forge.util.MyRandom;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;

public final class GamePlayerUtil {
    private GamePlayerUtil() { }
    private static Localizer localizer = Localizer.getInstance();
    private static final LobbyPlayer guiPlayer = new LobbyPlayerHuman("Human");
    public static LobbyPlayer getGuiPlayer() {
        return guiPlayer;
    }
    public static LobbyPlayer getGuiPlayer(final String name, final int avatarIndex, final int sleeveIndex, final boolean writePref) {
        if (writePref) {
            if (!name.equals(guiPlayer.getName())) {
                guiPlayer.setName(name);
                FModel.getPreferences().setPref(FPref.PLAYER_NAME, name);
                FModel.getPreferences().save();
            }

            guiPlayer.setAvatarIndex(avatarIndex);
            guiPlayer.setSleeveIndex(sleeveIndex);
            return guiPlayer;
        }
        //use separate LobbyPlayerHuman instance for human players beyond first
        return new LobbyPlayerHuman(name, avatarIndex, sleeveIndex);
    }

    public static LobbyPlayer getQuestPlayer() {
        return guiPlayer; //TODO: Make this a separate player
    }

    public static LobbyPlayer createAiPlayer() {
        return createAiPlayer(GuiDisplayUtil.getRandomAiName());
    }
    public static LobbyPlayer createAiPlayer(final String name) {
        final int avatarCount = GuiBase.getInterface().getAvatarCount();
        final int sleeveCount = GuiBase.getInterface().getSleevesCount();
        return createAiPlayer(name, avatarCount == 0 ? 0 : MyRandom.getRandom().nextInt(avatarCount), sleeveCount == 0 ? 0 : MyRandom.getRandom().nextInt(sleeveCount));
    }
    public static LobbyPlayer createAiPlayer(final String name, final String profileOverride) {
        final int avatarCount = GuiBase.getInterface().getAvatarCount();
        final int sleeveCount = GuiBase.getInterface().getSleevesCount();
        return createAiPlayer(name, avatarCount == 0 ? 0 : MyRandom.getRandom().nextInt(avatarCount), sleeveCount == 0 ? 0 : MyRandom.getRandom().nextInt(sleeveCount), null, profileOverride);
    }
    public static LobbyPlayer createAiPlayer(final String name, final int avatarIndex) {
        final int sleeveCount = GuiBase.getInterface().getSleevesCount();
        return createAiPlayer(name, avatarIndex, sleeveCount == 0 ? 0 : MyRandom.getRandom().nextInt(sleeveCount), null, "");
    }
    public static LobbyPlayer createAiPlayer(final String name, final int avatarIndex, final int sleeveIndex) {
        return createAiPlayer(name, avatarIndex, sleeveIndex, null, "");
    }
    public static LobbyPlayer createAiPlayer(final String name, final int avatarIndex, final int sleeveIndex, final Set<AIOption> options) {
        return createAiPlayer(name, avatarIndex, sleeveIndex, options, "");
    }
    public static LobbyPlayer createAiPlayer(final String name, final int avatarIndex, final int sleeveIndex, final Set<AIOption> options, final String profileOverride) {
        LobbyPlayerAi player;
        
        // Check if Claude AI is requested via name prefix
        // Since we're not using a profile file, detect Claude AI by name
        if (name.startsWith("Claude")) {
            System.out.println("[GamePlayerUtil] Creating Claude AI player: " + name);
            // Use reflection to avoid compile-time dependency
            try {
                Class<?> claudeClass = Class.forName("forge.ai.claude.LobbyPlayerClaude");
                player = (LobbyPlayerAi) claudeClass.getConstructor(String.class, Set.class).newInstance(name, options);
            } catch (Exception e) {
                System.err.println("[GamePlayerUtil] Failed to create Claude player, falling back to regular AI: " + e.getMessage());
                player = new LobbyPlayerAi(name, options);
            }
        } else {
            player = new LobbyPlayerAi(name, options);
        }

        // TODO: implement specific AI profiles for quest mode.
        String profile = "";
        if (profileOverride.isEmpty()) {
            String lastProfileChosen = FModel.getPreferences().getPref(FPref.UI_CURRENT_AI_PROFILE);
            if (!AiProfileUtil.getProfilesDisplayList().contains(lastProfileChosen)) {
                System.out.println("[AI Preferences] Unknown profile " + lastProfileChosen + " was requested, resetting to default.");
                lastProfileChosen = "Default";
                FModel.getPreferences().setPref(FPref.UI_CURRENT_AI_PROFILE, "Default");
                FModel.getPreferences().save();
            }
            player.setRotateProfileEachGame(lastProfileChosen.equals(AiProfileUtil.AI_PROFILE_RANDOM_DUEL));
            if (lastProfileChosen.equals(AiProfileUtil.AI_PROFILE_RANDOM_MATCH)) {
                lastProfileChosen = AiProfileUtil.getRandomProfile();
            }
            profile = lastProfileChosen;
        } else {
            profile = profileOverride;
        }

        assert (!profile.isEmpty());
        
        player.setAiProfile(profile);
        player.setAvatarIndex(avatarIndex);
        player.setSleeveIndex(sleeveIndex);
        return player;
    }

    public static void setPlayerName() {
        final String oldPlayerName = FModel.getPreferences().getPref(FPref.PLAYER_NAME);

        String newPlayerName;
        try {
            if (StringUtils.isBlank(oldPlayerName)) {
                newPlayerName = getVerifiedPlayerName(getPlayerNameUsingFirstTimePrompt(), oldPlayerName);
            } else {
                newPlayerName = getVerifiedPlayerName(getPlayerNameUsingStandardPrompt(oldPlayerName), oldPlayerName);
            }
        } catch (final IllegalStateException ise){
            //now is not a good time for this...
            newPlayerName = StringUtils.isBlank(oldPlayerName) ? "Human" : oldPlayerName;
        }

        FModel.getPreferences().setPref(FPref.PLAYER_NAME, newPlayerName);
        FModel.getPreferences().save();

        if (StringUtils.isBlank(oldPlayerName) && !newPlayerName.equals("Human")) {
            showThankYouPrompt(newPlayerName);
        }
    }

    public static void setServerPort() {
        final int oldPort = FModel.getNetPreferences().getPrefInt(ForgeNetPreferences.FNetPref.NET_PORT);
        int newPort = getServerPortPrompt(oldPort);
        FModel.getNetPreferences().setPref(ForgeNetPreferences.FNetPref.NET_PORT, String.valueOf(newPort));
        FModel.getNetPreferences().save();
    }

    private static void showThankYouPrompt(final String playerName) {
        SOptionPane.showMessageDialog("Thank you, " + playerName + ". "
                + "You will not be prompted again but you can change\n"
                + "your name at any time using the \"Player Name\" setting in Preferences\n"
                + "or via the constructed match setup screen\n");
    }

    private static String getPlayerNameUsingFirstTimePrompt() {
        return SOptionPane.showInputDialog(
                "By default, Forge will refer to you as the \"Human\" during gameplay.\n" +
                        "If you would prefer a different name please enter it now.",
                        "Personalize Forge Gameplay",
                        SOptionPane.QUESTION_ICON);
    }

    private static String getPlayerNameUsingStandardPrompt(final String playerName) {
        return SOptionPane.showInputDialog(
                "Please enter a new name. (alpha-numeric only)",
                "Personalize Forge Gameplay",
                null,
                playerName);
    }

    private static Integer getServerPortPrompt(final Integer serverPort) {
        String input = SOptionPane.showInputDialog(
                localizer.getMessage("sOPServerPromptMessage"),
                localizer.getMessage("sOPServerPromptTitle"),
                null,
                serverPort.toString(),
                null,
                true
        );
        Integer port;
        try {
             port = Integer.parseInt(input);
        } catch (NumberFormatException nfe) {
            SOptionPane.showErrorDialog(localizer.getMessage("sOPServerPromptError", input));
            return serverPort;
        }
        if(port < 0 || port > 65535) {
            SOptionPane.showErrorDialog(localizer.getMessage("sOPServerPromptError", input));
            return serverPort;
        }
        return  port;
    }

    private static String getVerifiedPlayerName(String newName, final String oldName) {
        if (newName == null || !StringUtils.isAlphanumericSpace(newName)) {
            newName = (StringUtils.isBlank(oldName) ? "Human" : oldName);
        } else if (StringUtils.isWhitespace(newName)) {
            newName = "Human";
        } else {
            newName = newName.trim();
        }
        return newName;
    }


}
