package forge.ai.claude;

import forge.LobbyPlayer;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilMana;
import forge.ai.PlayerControllerAi;
import forge.card.ColorSet;
import forge.card.mana.ManaCost;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.GameView;
import forge.game.card.*;
import forge.game.combat.Combat;
import forge.game.cost.CostPartMana;
import forge.game.mana.*;
import forge.game.phase.PhaseType;
import forge.game.player.*;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.*;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.ZoneType;
import forge.card.ICardFace;
import forge.util.ITriggerEvent;
import forge.util.collect.FCollectionView;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

/**
 * Claude-powered AI controller for Forge MTG.
 * 
 * This class extends PlayerControllerAi to integrate Claude's decision-making
 * capabilities into Forge's rules engine. It provides:
 * 
 * - Complete game state serialization to protocol JSON format
 * - Action translation between Forge SpellAbility objects and protocol format
 * - Robust network communication with the Python Claude server
 * - Graceful error handling and fallback to default AI behavior
 * - Connection lifecycle management and reconnection logic
 * 
 * The integration preserves Forge's existing AI hierarchy while giving Claude
 * strategic control over gameplay decisions. All tactical rule enforcement
 * remains in Forge's hands for correctness and performance.
 * 
 * Connection Flow:
 * 1. Lazy connection on first decision request
 * 2. Game state serialization and legal action extraction  
 * 3. Network request to Python server (with Claude API call)
 * 4. Response parsing and action mapping back to Forge format
 * 5. Graceful fallback to parent AI on any failures
 */
public class ClaudePlayerController extends PlayerControllerAi {
    
    // Connection and protocol management
    private ClaudeProtocolClient protocolClient;
    private final ProtocolHandler protocolHandler;
    private final MessageValidator messageValidator;
    private final String gameId;
    private boolean connectionInitialized = false;
    private boolean connectionFailed = false;
    
    // Timing and performance
    private long lastDecisionTime = 0;
    private int totalDecisionCount = 0;
    private int claudeSuccessfulDecisions = 0;
    private int fallbackDecisions = 0;
    
    // Connection configuration
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = ProtocolMessage.DEFAULT_PORT; // 8889
    private static final int PLAYER_2_PORT = 8890; // Different port for player 2
    private static final int DECISION_TIMEOUT_MS = 30000; // 30 seconds - matches Python timeout
    private static final int CONNECTION_RETRY_ATTEMPTS = 2;
    
    // Session management
    private String sessionId = null;
    private boolean gameInitialized = false;
    
    /**
     * Constructor matching parent PlayerControllerAi signature.
     * 
     * @param game Game instance providing access to complete game state
     * @param player Player this controller manages
     * @param lobbyPlayer Lobby player configuration  
     */
    public ClaudePlayerController(Game game, Player player, LobbyPlayer lobbyPlayer) {
        super(game, player, lobbyPlayer);
        
        this.protocolHandler = new ProtocolHandler("claude-controller");
        this.messageValidator = new MessageValidator();
        this.gameId = "forge-game-" + UUID.randomUUID().toString().substring(0, 8);
        
        System.out.println("[ClaudePlayerController] Initialized for player: " + player.getName() + 
                          " (Game ID: " + gameId + ")");
    }
    
    /**
     * Main decision-making method for Claude AI.
     * 
     * This overrides the AI's spell ability selection to integrate Claude's
     * decision-making. Called during the main phase to determine what to play.
     * 
     * @return List containing the chosen SpellAbility, or empty list for pass
     */
    @Override
    public List<SpellAbility> chooseSpellAbilityToPlay() {
        System.out.println("\n[ClaudePlayerController] ========== chooseSpellAbilityToPlay CALLED ==========");
        System.out.println("[ClaudePlayerController] Player: " + player.getName());
        System.out.println("[ClaudePlayerController] Game phase: " + player.getGame().getPhaseHandler().getPhase());
        System.out.println("[ClaudePlayerController] Turn player: " + player.getGame().getPhaseHandler().getPlayerTurn().getName());
        boolean isMain1 = player.getGame().getPhaseHandler().is(PhaseType.MAIN1);
        boolean isMain2 = player.getGame().getPhaseHandler().is(PhaseType.MAIN2);
        System.out.println("[ClaudePlayerController] Is main phase: " + (isMain1 || isMain2));
        System.out.println("[ClaudePlayerController] Priority player: " + player.getGame().getPhaseHandler().getPriorityPlayer().getName());
        System.out.println("[ClaudePlayerController] Is my turn: " + (player.getGame().getPhaseHandler().getPlayerTurn() == player));
        System.out.println("[ClaudePlayerController] Is my priority: " + (player.getGame().getPhaseHandler().getPriorityPlayer() == player));
        System.out.println("[ClaudePlayerController] Stack empty: " + player.getGame().getStack().isEmpty());
        totalDecisionCount++;
        
        // Collect ALL available actions more comprehensively
        List<SpellAbility> allAbilities = new ArrayList<>();
        
        // 1. Get spells from hand directly
        CardCollectionView hand = player.getCardsIn(ZoneType.Hand);
        System.out.println("[ClaudePlayerController] Hand size: " + hand.size());
        for (Card c : hand) {
            System.out.println("[ClaudePlayerController]   Card: " + c.getName() + " (Type: " + c.getType() + ", CMC: " + c.getCMC() + ")");
            for (SpellAbility sa : c.getSpellAbilities()) {
                // IMPORTANT: Set the activating player before checking canPlay()
                sa.setActivatingPlayer(player);
                boolean canPlay = sa.canPlay();
                
                // Also check if we can pay the mana cost
                boolean canPayMana = true;
                if (canPlay && sa.getPayCosts() != null && sa.getPayCosts().hasManaCost()) {
                    // Check if we can actually pay the mana cost using the 3-parameter version
                    canPayMana = ComputerUtilMana.canPayManaCost(sa, player, 0, false);
                    if (!canPayMana && canPlay) {
                        System.out.println("[ClaudePlayerController]     ✗ Can play but can't pay mana: " + sa.toString() + " (Cost: " + sa.getPayCosts().getTotalMana() + ")");
                        canPlay = false; // Don't include it in legal actions
                    }
                }
                
                if (canPlay) {
                    allAbilities.add(sa);
                    System.out.println("[ClaudePlayerController]     ✓ Can play: " + sa.toString());
                } else {
                    // Debug why we can't play
                    System.out.println("[ClaudePlayerController]     ✗ Cannot play: " + sa.toString() + 
                        " (isLand: " + sa.isLandAbility() + ")");
                }
            }
        }
        
        // 2. Log some debug info about lands if in main phase
        if ((isMain1 || isMain2) && player == player.getGame().getPhaseHandler().getPlayerTurn() 
            && player.getGame().getStack().isEmpty()) {
            System.out.println("[ClaudePlayerController] Lands played this turn: " + player.getLandsPlayedThisTurn());
            
            int landsInHand = 0;
            for (Card c : hand) {
                if (c.isLand()) {
                    landsInHand++;
                }
            }
            System.out.println("[ClaudePlayerController] Total lands in hand: " + landsInHand);
        }
        
        // 3. Get activated abilities from battlefield
        CardCollectionView battlefield = player.getCardsIn(ZoneType.Battlefield);
        System.out.println("[ClaudePlayerController] Battlefield size: " + battlefield.size());
        for (Card c : battlefield) {
            for (SpellAbility sa : c.getAllSpellAbilities()) {
                if (sa.isActivatedAbility()) {
                    // Skip mana abilities - they don't use the stack and shouldn't be presented as actions
                    if (sa.isManaAbility()) {
                        continue;
                    }
                    // IMPORTANT: Set the activating player before checking canPlay()
                    sa.setActivatingPlayer(player);
                    if (sa.canPlay()) {
                        allAbilities.add(sa);
                        System.out.println("[ClaudePlayerController]   - Can activate: " + c.getName() + " - " + sa.toString());
                    }
                }
            }
        }
        
        // Filter out unsupported cards only
        allAbilities.removeIf(sa -> 
            (sa.getHostCard() != null && ComputerUtilCard.isCardRemAIDeck(sa.getHostCard())));
        
        // Check if we have any real actions besides pass
        if (allAbilities.isEmpty()) {
            // No actions available - automatically pass without asking server
            System.out.println("[ClaudePlayerController] No actions available - automatically passing (returning null)");
            System.out.println("[ClaudePlayerController] ===================================================\n");
            return null;  // Return null to indicate pass, not empty list
        }
        
        System.out.println("[ClaudePlayerController] Found " + allAbilities.size() + " possible actions (plus pass option)");
        
        // Add pass priority as an option since we have other choices
        allAbilities.add(null); // null represents pass
        
        // Now ask Claude to choose between the available actions
        SpellAbility chosen = getAbilityToPlay(null, allAbilities, null);
        
        if (chosen != null) {
            System.out.println("[ClaudePlayerController] Claude chose: " + chosen.getDescription());
            return Collections.singletonList(chosen);
        } else {
            System.out.println("[ClaudePlayerController] Claude chose to pass");
            return null;  // Return null to indicate pass, not empty list
        }
    }
    
    /**
     * Core decision-making logic for Claude AI.
     * 
     * This is the primary integration point where Claude receives the game state
     * and legal actions, makes a strategic decision based on its understanding,
     * and return the chosen action.
     * 
     * @param hostCard Card that's causing the decision (may be null)
     * @param abilities List of legal SpellAbility options to choose from
     * @param triggerEvent Trigger that caused this decision (may be null)
     * @return Chosen SpellAbility, or null if no valid choice
     */
    @Override
    public SpellAbility getAbilityToPlay(Card hostCard, List<SpellAbility> abilities, ITriggerEvent triggerEvent) {
        System.out.println("[ClaudePlayerController] getAbilityToPlay CALLED for player: " + player.getName());
        totalDecisionCount++;
        long startTime = System.currentTimeMillis();
        
        System.out.println("[ClaudePlayerController] Decision request #" + totalDecisionCount + 
                          " - " + abilities.size() + " legal actions available");
        
        // Quick validation and fallback cases
        if (abilities.isEmpty()) {
            System.out.println("[ClaudePlayerController] No abilities available - returning null");
            return null;
        }
        
        // Always consult Claude, even for single actions, to ensure the system is working
        // This helps us debug whether Claude is actually making decisions
        
        // Remove null entries for serialization (we'll add pass as an explicit action)
        List<SpellAbility> nonNullAbilities = new ArrayList<>();
        boolean hasPassOption = false;
        for (SpellAbility sa : abilities) {
            if (sa == null) {
                hasPassOption = true;
            } else {
                nonNullAbilities.add(sa);
            }
        }
        
        try {
            // 1. Ensure connection is established
            if (!ensureConnection()) {
                System.err.println("[ClaudePlayerController] Connection failed - falling back to default AI");
                return fallbackToDefaultAI(hostCard, abilities, triggerEvent);
            }
            
            // 2. Initialize game session on first action request if not already done
            if (!gameInitialized && connectionInitialized) {
                System.out.println("[ClaudePlayerController] First action request - initializing game session");
                if (!initializeGameSession()) {
                    System.err.println("[ClaudePlayerController] Game initialization failed - falling back to default AI");
                    return fallbackToDefaultAI(hostCard, abilities, triggerEvent);
                }
            }
            
            // 2. Serialize current game state
            Map<String, Object> gameState = serializeGameState();
            
            // 3. Serialize legal actions  
            List<Map<String, Object>> legalActions = serializeLegalActions(nonNullAbilities);
            
            // 4. Add pass option if available
            if (hasPassOption) {
                Map<String, Object> passAction = new HashMap<>();
                passAction.put("index", nonNullAbilities.size());
                passAction.put("action_type", "pass");
                passAction.put("description", "Pass priority");
                legalActions.add(passAction);
            }
            
            // 5. Create decision context for Claude
            Map<String, Object> decisionContext = createDecisionContext(hostCard, triggerEvent);
            
            // Add prompt instructions for Claude to use specific tags
            decisionContext.put("prompt_instructions", 
                "IMPORTANT: Express your decision using EXACTLY this format:\n" +
                "<decision>\n" +
                "<index>N</index>\n" +
                "<reasoning>Your reasoning here</reasoning>\n" +
                "</decision>\n" +
                "Where N is the index number of your chosen action from the list provided.\n" +
                "Do not include any other text outside these tags.");
            
            // 6. Send action request to Claude via protocol client
            // Add session_id to game state for server to track
            gameState.put("session_id", sessionId);
            
            // 6. Send action request to Claude via protocol client
            ProtocolMessage actionRequest = protocolHandler.createActionRequest(
                gameId,
                getPlayerIndex(player),
                gameState,
                legalActions,
                decisionContext
            );
            
            System.out.println("[ClaudePlayerController] Sending action request to Claude server...");
            
            // 6. Wait for Claude's response with timeout
            ProtocolMessage response = protocolClient.sendRequest(actionRequest, DECISION_TIMEOUT_MS);
            
            // 7. Validate and process response
            ProtocolMessage.ValidationResult validation = messageValidator.validateMessage(response);
            if (!validation.isValid()) {
                System.err.println("[ClaudePlayerController] Invalid response: " + validation.getErrorMessage());
                return fallbackToDefaultAI(hostCard, abilities, triggerEvent);
            }
            
            // 8. Extract chosen action from response
            SpellAbility chosenAction = parseClaudeResponse(response, nonNullAbilities, hasPassOption);
            
            // Check if parseClaudeResponse returned null because Claude chose to pass
            // or because of an error
            @SuppressWarnings("unchecked")
            Map<String, Object> actionData = (Map<String, Object>) response.getData().get("action");
            if (actionData != null) {
                Object indexObj = actionData.get("index");
                if (hasPassOption && indexObj instanceof Integer && 
                    (Integer)indexObj == nonNullAbilities.size()) {
                    // Claude explicitly chose to pass
                    claudeSuccessfulDecisions++;
                    long elapsedMs = System.currentTimeMillis() - startTime;
                    lastDecisionTime = elapsedMs;
                    System.out.println("[ClaudePlayerController] Claude decision successful in " + 
                                      elapsedMs + "ms - chose to pass");
                    return null; // Return null to indicate pass
                }
            }
            
            if (chosenAction != null) {
                claudeSuccessfulDecisions++;
                long elapsedMs = System.currentTimeMillis() - startTime;
                lastDecisionTime = elapsedMs;
                
                System.out.println("[ClaudePlayerController] Claude decision successful in " + 
                                  elapsedMs + "ms - chosen: " + describeAction(chosenAction));
                
                return chosenAction;
            } else {
                System.err.println("[ClaudePlayerController] Failed to parse Claude's chosen action");
                return fallbackToDefaultAI(hostCard, abilities, triggerEvent);
            }
            
        } catch (IOException e) {
            System.err.println("[ClaudePlayerController] Network error during decision: " + e.getMessage());
            // Connection may be broken - mark for retry
            connectionFailed = true;
            return fallbackToDefaultAI(hostCard, abilities, triggerEvent);
            
        } catch (TimeoutException e) {
            System.err.println("[ClaudePlayerController] Claude decision timed out after " + 
                              DECISION_TIMEOUT_MS + "ms");
            return fallbackToDefaultAI(hostCard, abilities, triggerEvent);
            
        } catch (Exception e) {
            System.err.println("[ClaudePlayerController] Unexpected error during decision: " + e.getMessage());
            e.printStackTrace();
            return fallbackToDefaultAI(hostCard, abilities, triggerEvent);
        }
    }
    
    /**
     * Ensure network connection to Python server is established.
     * 
     * This uses lazy initialization to create the connection only when needed,
     * with retry logic for temporary network issues.
     * 
     * @return true if connection is ready, false if connection failed
     */
    private boolean ensureConnection() {
        // Quick path: connection already working
        if (connectionInitialized && !connectionFailed) {
            return true;
        }
        
        // If connection previously failed, don't retry immediately
        if (connectionFailed) {
            System.out.println("[ClaudePlayerController] Connection previously failed - not retrying");
            return false;
        }
        
        System.out.println("[ClaudePlayerController] Establishing connection to Claude server...");
        
        try {
            // Determine port based on player name
            int port = DEFAULT_PORT;
            String playerName = player.getName();
            if (playerName != null && playerName.contains("Claude 2")) {
                port = PLAYER_2_PORT;
                System.out.println("[ClaudePlayerController] Using port " + port + " for player 2");
            } else {
                System.out.println("[ClaudePlayerController] Using port " + port + " for player 1");
            }
            
            // Create and configure protocol client
            protocolClient = new ClaudeProtocolClient(DEFAULT_HOST, port);
            
            // Set up connection callbacks for monitoring
            protocolClient.setConnectionCallback(new ClaudeProtocolClient.ConnectionCallback() {
                @Override
                public void onConnected(ProtocolMessage welcomeMessage) {
                    System.out.println("[ClaudePlayerController] Connected to Claude server - " + 
                                      welcomeMessage.getData().get("server_version"));
                    connectionInitialized = true;
                    connectionFailed = false;
                }
                
                @Override
                public void onDisconnected(String reason, boolean canRetry) {
                    System.err.println("[ClaudePlayerController] Disconnected from Claude server: " + reason);
                    if (!canRetry) {
                        connectionFailed = true;
                    }
                }
            });
            
            // Set up message callback for debugging
            protocolClient.setMessageCallback(new ClaudeProtocolClient.MessageCallback() {
                @Override
                public void onMessageReceived(ProtocolMessage message) {
                    // Debug logging for message tracing
                    System.out.println("[ClaudePlayerController] Received: " + message.getMessageType());
                }
                
                @Override
                public void onMessageError(String error, Throwable cause) {
                    System.err.println("[ClaudePlayerController] Message error: " + error);
                }
            });
            
            // Attempt connection with timeout
            protocolClient.connect();
            
            // Connection successful - wait a moment for welcome message
            Thread.sleep(1000);
            
            if (connectionInitialized) {
                System.out.println("[ClaudePlayerController] Connection established successfully");
                // Game initialization will happen on first action request
                return true;
            } else {
                System.err.println("[ClaudePlayerController] Connection attempt completed but not initialized");
                connectionFailed = true;
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("[ClaudePlayerController] Failed to establish connection: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            connectionFailed = true;
            
            // Clean up failed connection attempt
            if (protocolClient != null) {
                try {
                    protocolClient.disconnect();
                } catch (Exception cleanupEx) {
                    System.err.println("[ClaudePlayerController] Error during connection cleanup: " + 
                                      cleanupEx.getMessage());
                }
                protocolClient = null;
            }
            
            return false;
        }
    }
    
    /**
     * Initialize game session with the server.
     * 
     * @return true if game session initialized successfully
     */
    private boolean initializeGameSession() {
        System.out.println("[ClaudePlayerController] Initializing game session...");
        
        try {
            // Create player data
            List<Map<String, Object>> playersData = new ArrayList<>();
            Game game = getGame();
            
            for (Player p : game.getPlayers()) {
                Map<String, Object> playerData = new HashMap<>();
                playerData.put("name", p.getName());
                playerData.put("index", getPlayerIndex(p));
                playerData.put("deck_name", "Player " + getPlayerIndex(p) + " Deck"); // Forge doesn't expose deck in game
                playersData.add(playerData);
            }
            
            // Create initialize game request
            ProtocolMessage initRequest = protocolHandler.createInitializeGameRequest(
                gameId,
                playersData
            );
            
            System.out.println("[ClaudePlayerController] Sending initialize_game request...");
            
            // Send request and wait for response
            ProtocolMessage response = protocolClient.sendRequest(initRequest, 10000); // 10 second timeout
            
            // Check response
            if (response != null && response.getData() != null) {
                String status = (String) response.getData().get("status");
                if ("ready".equals(status)) {
                    sessionId = (String) response.getData().get("session_id");
                    gameInitialized = true;
                    System.out.println("[ClaudePlayerController] Game session initialized: " + sessionId);
                    return true;
                } else {
                    System.err.println("[ClaudePlayerController] Game initialization failed: status = " + status);
                }
            }
            
        } catch (Exception e) {
            System.err.println("[ClaudePlayerController] Failed to initialize game session: " + e.getMessage());
            e.printStackTrace();
        }
        
        gameInitialized = false;
        return false;
    }
    
    /**
     * Fallback to parent AI behavior when Claude integration fails.
     * 
     * This ensures the game remains playable even when Claude is unavailable,
     * maintaining the player experience while providing fallback intelligence.
     * 
     * @param hostCard Original hostCard parameter
     * @param abilities Original abilities parameter  
     * @param triggerEvent Original triggerEvent parameter
     * @return Action chosen by parent AI logic
     */
    private SpellAbility fallbackToDefaultAI(Card hostCard, List<SpellAbility> abilities, ITriggerEvent triggerEvent) {
        // No fallback - fail hard to see the real issue
        throw new RuntimeException("Claude AI failed to make a decision - stopping game. Total failed decisions: " + (fallbackDecisions + 1));
    }
    
    /**
     * Serialize current game state to protocol JSON format.
     * 
     * This creates a comprehensive representation of the game state that Claude
     * can understand and reason about, including:
     * - Game phase and turn information
     * - All players' visible information
     * - Battlefield state with all permanents
     * - Stack state for timing decisions
     * - Zone contents respecting hidden information rules
     * 
     * @return Map representing game state for JSON serialization
     */
    private Map<String, Object> serializeGameState() {
        Map<String, Object> state = new HashMap<>();
        
        try {
            Game game = getGame();
            GameView gameView = game.getView();
            
            // Basic game information
            state.put("turn_number", game.getPhaseHandler().getTurn());
            state.put("phase", game.getPhaseHandler().getPhase().toString());
            state.put("phase_index", game.getPhaseHandler().getPhase().ordinal());
            
            // Get the priority player index
            Player priorityPlayer = game.getPhaseHandler().getPriorityPlayer();
            state.put("priority_player", getPlayerIndex(priorityPlayer));
            
            // Get the active player (turn player) index
            Player turnPlayer = game.getPhaseHandler().getPlayerTurn();
            if (turnPlayer == null) {
                // Fallback: use priority player if turn player is null
                turnPlayer = priorityPlayer;
            }
            state.put("active_player", getPlayerIndex(turnPlayer));
            state.put("active_player_index", getPlayerIndex(turnPlayer)); // Add as separate field for compatibility
            
            // Players information
            List<Map<String, Object>> players = new ArrayList<>();
            for (Player gamePlayer : game.getPlayers()) {
                Map<String, Object> playerInfo = new HashMap<>();
                playerInfo.put("index", getPlayerIndex(gamePlayer));
                playerInfo.put("name", gamePlayer.getName());
                playerInfo.put("life", gamePlayer.getLife());
                playerInfo.put("hand_size", gamePlayer.getCardsIn(forge.game.zone.ZoneType.Hand).size());
                playerInfo.put("library_size", gamePlayer.getCardsIn(forge.game.zone.ZoneType.Library).size());
                playerInfo.put("graveyard_size", gamePlayer.getCardsIn(forge.game.zone.ZoneType.Graveyard).size());
                playerInfo.put("battlefield_size", gamePlayer.getCardsIn(forge.game.zone.ZoneType.Battlefield).size());
                
                // Add mana information
                playerInfo.put("available_mana", serializeManaPools(gamePlayer));
                
                // For the requesting player, include hand contents
                if (gamePlayer.equals(player)) {
                    playerInfo.put("hand", serializeCardList(gamePlayer.getCardsIn(forge.game.zone.ZoneType.Hand), getPlayerIndex(gamePlayer)));
                }
                
                players.add(playerInfo);
            }
            state.put("players", players);
            
            // Battlefield state - all visible permanents
            List<Map<String, Object>> battlefield = new ArrayList<>();
            for (Player gamePlayer : game.getPlayers()) {
                CardCollectionView battlefieldCards = gamePlayer.getCardsIn(forge.game.zone.ZoneType.Battlefield);
                for (Card card : battlefieldCards) {
                    battlefield.add(serializeCard(card, getPlayerIndex(gamePlayer)));
                }
            }
            state.put("battlefield", battlefield);
            
            // Stack state - current spells/abilities waiting to resolve
            List<Map<String, Object>> stack = new ArrayList<>();
            // TODO: Implement stack serialization when needed for complex interactions
            state.put("stack", stack);
            
            // Combat state if in combat
            state.put("in_combat", game.getCombat() != null);
            if (game.getCombat() != null) {
                state.put("combat_state", serializeCombatState(game.getCombat()));
            }
            
        } catch (Exception e) {
            System.err.println("[ClaudePlayerController] Error serializing game state: " + e.getMessage());
            e.printStackTrace();
            // Return partial state on error - better than failing entirely
        }
        
        return state;
    }
    
    /**
     * Serialize legal actions for Claude to choose from.
     * 
     * Converts Forge's SpellAbility objects into protocol JSON format,
     * providing sufficient information for Claude to understand the
     * strategic implications of each action.
     * 
     * @param abilities List of legal SpellAbility objects
     * @return List of action descriptions in protocol format
     */
    private List<Map<String, Object>> serializeLegalActions(List<SpellAbility> abilities) {
        List<Map<String, Object>> actions = new ArrayList<>();
        
        for (int i = 0; i < abilities.size(); i++) {
            SpellAbility ability = abilities.get(i);
            
            Map<String, Object> action = new HashMap<>();
            action.put("index", i); // For correlation when parsing response
            
            String actionType = determineActionType(ability);
            action.put("action_type", actionType);
            
            // Create a better description that includes card name
            String description;
            if (ability.getHostCard() != null) {
                Card hostCard = ability.getHostCard();
                String cardName = hostCard.getName();
                
                // For play land actions, use specific format
                if (actionType.equals("Play land")) {
                    description = cardName;  // Just the land name, no "other" suffix
                } else if (actionType.equals("cast_spell")) {
                    description = "Cast " + cardName;
                } else if (ability.getDescription() != null && !ability.getDescription().isEmpty()) {
                    description = cardName + " - " + ability.getDescription();
                } else {
                    description = describeAction(ability);
                }
                
                action.put("card_name", cardName);
                action.put("card_id", "card-" + hostCard.getId());
                action.put("card_type", hostCard.getType().toString());
                
                // Add mana cost if it's a spell
                if (ability.isSpell()) {
                    action.put("mana_cost", hostCard.getManaCost().toString());
                }
            } else {
                // No host card - use ability description
                description = ability.getDescription() != null ? ability.getDescription() : ability.toString();
            }
            
            action.put("description", description);
            
            // Add ability details
            action.put("ability_text", ability.getDescription());
            action.put("is_spell", ability.isSpell());
            action.put("is_activated", ability.isActivatedAbility());
            action.put("is_triggered", isTriggeredAbility(ability));
            
            // Add targeting information if available
            if (ability.getTargetRestrictions() != null) {
                action.put("has_targets", true);
                action.put("target_description", ability.getTargetRestrictions().toString());
            } else {
                action.put("has_targets", false);
            }
            
            actions.add(action);
        }
        
        return actions;
    }
    
    /**
     * Create additional context for Claude's decision-making.
     * 
     * Provides situational information that helps Claude understand
     * the current decision context and make more informed choices.
     * 
     * @param hostCard Card causing the decision 
     * @param triggerEvent Trigger event if applicable
     * @return Context information map
     */
    private Map<String, Object> createDecisionContext(Card hostCard, ITriggerEvent triggerEvent) {
        Map<String, Object> context = new HashMap<>();
        
        context.put("decision_number", totalDecisionCount);
        context.put("player_name", player.getName());
        context.put("player_index", getPlayerIndex(player));
        
        if (hostCard != null) {
            context.put("host_card_name", hostCard.getName());
            context.put("host_card_type", hostCard.getType().toString());
        }
        
        if (triggerEvent != null) {
            context.put("trigger_present", true);
            context.put("trigger_type", triggerEvent.getClass().getSimpleName());
        }
        
        // Add timing hint
        context.put("thinking_time_ms", DECISION_TIMEOUT_MS);
        
        return context;
    }
    
    /**
     * Parse Claude's response and map chosen action back to SpellAbility.
     * 
     * Takes the protocol response containing Claude's decision and finds
     * the matching SpellAbility from the available options.
     * 
     * @param response Claude's action response message
     * @param abilities Original list of available abilities
     * @return Chosen SpellAbility, or null if parsing failed
     */
    private SpellAbility parseClaudeResponse(ProtocolMessage response, List<SpellAbility> abilities, boolean hasPassOption) {
        try {
            Map<String, Object> data = response.getData();
            
            // Check for error response
            if (Boolean.FALSE.equals(data.get("success"))) {
                String errorMsg = (String) data.get("error_message");
                System.err.println("[ClaudePlayerController] Claude reported error: " + errorMsg);
                return null;
            }
            
            // Get Claude's response text
            String claudeResponse = (String) data.get("claude_response");
            if (claudeResponse == null) {
                // Fall back to action data if available
                @SuppressWarnings("unchecked")
                Map<String, Object> actionData = (Map<String, Object>) data.get("action");
                if (actionData == null) {
                    System.err.println("[ClaudePlayerController] Response missing both claude_response and action data");
                    return null;
                }
                // Use existing JSON parsing logic as fallback
                return parseActionData(actionData, abilities, hasPassOption);
            }
            
            // Parse Claude's tagged response
            return parseTaggedResponse(claudeResponse, abilities, hasPassOption);
        } catch (Exception e) {
            System.err.println("[ClaudePlayerController] Error parsing Claude response: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Parse Claude's tagged response format.
     */
    private SpellAbility parseTaggedResponse(String response, List<SpellAbility> abilities, boolean hasPassOption) {
        // Extract index from <index>N</index> tags
        java.util.regex.Pattern indexPattern = java.util.regex.Pattern.compile("<index>(\\d+)</index>");
        java.util.regex.Matcher matcher = indexPattern.matcher(response);
        
        if (matcher.find()) {
            try {
                int chosenIndex = Integer.parseInt(matcher.group(1));
                
                // Check if Claude chose to pass
                if (hasPassOption && chosenIndex == abilities.size()) {
                    System.out.println("[ClaudePlayerController] Claude chose to pass (tagged index: " + chosenIndex + ")");
                    // Extract reasoning if available
                    java.util.regex.Pattern reasonPattern = java.util.regex.Pattern.compile("<reasoning>([^<]+)</reasoning>");
                    java.util.regex.Matcher reasonMatcher = reasonPattern.matcher(response);
                    if (reasonMatcher.find()) {
                        System.out.println("[ClaudePlayerController] Claude's reasoning: " + reasonMatcher.group(1).trim());
                    }
                    return null; // null indicates pass
                }
                
                if (chosenIndex >= 0 && chosenIndex < abilities.size()) {
                    System.out.println("[ClaudePlayerController] Claude chose action by tagged index: " + chosenIndex);
                    // Extract reasoning if available
                    java.util.regex.Pattern reasonPattern = java.util.regex.Pattern.compile("<reasoning>([^<]+)</reasoning>");
                    java.util.regex.Matcher reasonMatcher = reasonPattern.matcher(response);
                    if (reasonMatcher.find()) {
                        System.out.println("[ClaudePlayerController] Claude's reasoning: " + reasonMatcher.group(1).trim());
                    }
                    return abilities.get(chosenIndex);
                } else {
                    System.err.println("[ClaudePlayerController] Claude chose invalid index: " + chosenIndex + " (max: " + 
                                      (hasPassOption ? abilities.size() : abilities.size() - 1) + ")");
                }
            } catch (NumberFormatException e) {
                System.err.println("[ClaudePlayerController] Failed to parse index from tags: " + matcher.group(1));
            }
        } else {
            System.err.println("[ClaudePlayerController] No <index> tag found in Claude's response");
            System.err.println("[ClaudePlayerController] Response was: " + response);
        }
        
        return null;
    }
    
    /**
     * Parse action data from JSON structure (fallback method).
     */
    private SpellAbility parseActionData(Map<String, Object> actionData, List<SpellAbility> abilities, boolean hasPassOption) {
        // Try to match by index first (most reliable)
        Object indexObj = actionData.get("index");
        if (indexObj instanceof Integer) {
            int chosenIndex = (Integer) indexObj;
            
            // Check if Claude chose to pass
            if (hasPassOption && chosenIndex == abilities.size()) {
                System.out.println("[ClaudePlayerController] Claude chose to pass (index: " + chosenIndex + ")");
                return null; // null indicates pass
            }
            
            if (chosenIndex >= 0 && chosenIndex < abilities.size()) {
                System.out.println("[ClaudePlayerController] Claude chose action by index: " + chosenIndex);
                return abilities.get(chosenIndex);
            } else {
                System.err.println("[ClaudePlayerController] Claude chose invalid index: " + chosenIndex);
            }
        }
        
        // Fall back to matching by description or card name
        String actionType = (String) actionData.get("action_type");
        String cardName = (String) actionData.get("card_name");
        
        if (cardName != null) {
            for (int i = 0; i < abilities.size(); i++) {
                SpellAbility ability = abilities.get(i);
                if (ability.getHostCard() != null && 
                    cardName.equals(ability.getHostCard().getName())) {
                    System.out.println("[ClaudePlayerController] Claude chose action by card name: " + cardName);
                    return ability;
                }
            }
        }
        
        // If we can't match specifically, log the issue and return first safe option
        System.err.println("[ClaudePlayerController] Could not match Claude's chosen action - " +
                          "type: " + actionType + ", card: " + cardName);
        
        // No pass fallback - fail hard to see what Claude actually chose
        
        // No fallback - fail hard if we can't match Claude's choice
        throw new RuntimeException("Failed to match Claude's chosen action - " +
                                  "type: " + actionType + ", card: " + cardName + " - no valid match found");
    }
    
    // ==================== Helper Methods for Serialization ====================
    
    /**
     * Determine the type of action represented by a SpellAbility.
     */
    private String determineActionType(SpellAbility ability) {
        if (ability.isLandAbility()) {
            return "Play land";
        } else if (ability.isSpell()) {
            return "cast_spell";
        } else if (ability.isActivatedAbility()) {
            return "activate_ability";
        } else if (isTriggeredAbility(ability)) {
            return "triggered_ability";
        } else if (ability.toString().toLowerCase().contains("pass")) {
            return "pass";
        } else {
            return "other";
        }
    }
    
    /**
     * Check if this is a triggered ability.
     * Fallback for API compatibility.
     */
    private boolean isTriggeredAbility(SpellAbility ability) {
        // Simple heuristic: check if description contains "triggered" or similar
        String desc = ability.toString().toLowerCase();
        return desc.contains("triggered") || desc.contains("trigger") || desc.contains("when ");
    }
    
    /**
     * Get player index in game for protocol compatibility.
     * This creates a consistent index for players throughout the game.
     */
    private int getPlayerIndex(Player player) {
        if (player == null) {
            return -1;
        }
        
        Game game = getGame();
        List<Player> allPlayers = game.getPlayers();
        
        for (int i = 0; i < allPlayers.size(); i++) {
            if (allPlayers.get(i).equals(player)) {
                return i;
            }
        }
        
        // Fallback: return hash-based pseudo-index
        return Math.abs(player.getName().hashCode() % 8);
    }
    
    /**
     * Serialize a single card to protocol format.
     */
    private Map<String, Object> serializeCard(Card card, int controllerIndex) {
        Map<String, Object> cardInfo = new HashMap<>();
        
        cardInfo.put("id", "card-" + card.getId());
        cardInfo.put("name", card.getName());
        cardInfo.put("controller", controllerIndex);
        cardInfo.put("type", card.getType().toString());
        cardInfo.put("mana_cost", card.getManaCost().toString());
        
        // Add power/toughness for creatures
        if (card.isCreature()) {
            cardInfo.put("power", card.getCurrentPower());
            cardInfo.put("toughness", card.getCurrentToughness());
        }
        
        // Add card text/abilities
        cardInfo.put("oracle_text", card.getOracleText());
        
        return cardInfo;
    }
    
    /**
     * Serialize a collection of cards to protocol format.
     */
    private List<Map<String, Object>> serializeCardList(CardCollectionView cards, int controllerIndex) {
        List<Map<String, Object>> serialized = new ArrayList<>();
        for (Card card : cards) {
            serialized.add(serializeCard(card, controllerIndex));
        }
        return serialized;
    }
    
    /**
     * Serialize player's available mana.
     */
    private Map<String, Object> serializeManaPools(Player gamePlayer) {
        Map<String, Object> mana = new HashMap<>();
        
        // For now, simplified mana representation
        // TODO: Implement detailed mana pool serialization  
        mana.put("total", 0); // Placeholder - need to access actual mana pool
        
        return mana;
    }
    
    /**
     * Serialize combat state for strategic decision making.
     */
    private Map<String, Object> serializeCombatState(forge.game.combat.Combat combat) {
        Map<String, Object> combatInfo = new HashMap<>();
        
        // TODO: Implement detailed combat state serialization
        combatInfo.put("attacking_creatures", new ArrayList<>());
        combatInfo.put("blocking_assignments", new HashMap<>());
        
        return combatInfo;
    }
    
    /**
     * Create a human-readable description of an action for logging.
     */
    private String describeAction(SpellAbility ability) {
        StringBuilder desc = new StringBuilder();
        
        if (ability.getHostCard() != null) {
            desc.append(ability.getHostCard().getName()).append(" - ");
        }
        
        desc.append(determineActionType(ability));
        
        return desc.toString();
    }
    
    // ==================== Lifecycle and Status Methods ====================
    
    /**
     * Clean up resources when controller is no longer needed.
     */
    public void cleanup() {
        System.out.println("[ClaudePlayerController] Cleaning up resources...");
        
        // Send session end if we have an active session
        if (protocolClient != null && sessionId != null && gameInitialized) {
            try {
                // Create session end request
                Map<String, Object> endData = new HashMap<>();
                endData.put("type", "session_end");
                endData.put("session_id", sessionId);
                endData.put("game_id", gameId);
                
                ProtocolMessage endRequest = new ProtocolMessage(
                    ProtocolMessage.MessageType.REQUEST,
                    "end-req-" + sessionId,
                    endData
                );
                protocolClient.sendRequest(endRequest, 5000); // 5 second timeout
            } catch (Exception e) {
                System.err.println("[ClaudePlayerController] Error sending session end: " + e.getMessage());
            }
        }
        
        if (protocolClient != null) {
            try {
                protocolClient.shutdown();
            } catch (Exception e) {
                System.err.println("[ClaudePlayerController] Error during cleanup: " + e.getMessage());
            }
        }
        
        // Log final statistics
        System.out.println("[ClaudePlayerController] Session statistics:");
        System.out.println("  Total decisions: " + totalDecisionCount);
        System.out.println("  Claude successful: " + claudeSuccessfulDecisions + 
                          " (" + (totalDecisionCount > 0 ? (claudeSuccessfulDecisions * 100 / totalDecisionCount) : 0) + "%)");
        System.out.println("  Fallback decisions: " + fallbackDecisions + 
                          " (" + (totalDecisionCount > 0 ? (fallbackDecisions * 100 / totalDecisionCount) : 0) + "%)");
        
        if (claudeSuccessfulDecisions > 0) {
            System.out.println("  Average decision time: " + 
                              (lastDecisionTime / claudeSuccessfulDecisions) + "ms");
        }
    }
    
    /**
     * Get performance statistics for monitoring.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("total_decisions", totalDecisionCount);
        stats.put("claude_successful", claudeSuccessfulDecisions);
        stats.put("fallback_decisions", fallbackDecisions);
        stats.put("connection_initialized", connectionInitialized);
        stats.put("connection_failed", connectionFailed);
        stats.put("last_decision_time_ms", lastDecisionTime);
        
        return stats;
    }
    
    /**
     * Override mana payment to control auto-tapping behavior.
     * 
     * This method is called when a spell or ability needs mana payment.
     * For now, we'll allow the default auto-tapping behavior to ensure
     * spells can be cast successfully. In the future, this could be
     * enhanced to ask Claude which specific lands to tap.
     * 
     * @param toPay The mana cost to be paid
     * @param costPartMana The cost part requiring mana
     * @param sa The spell ability being paid for
     * @param prompt UI prompt (not used for AI)
     * @param matrix Mana conversion matrix for cost reductions
     * @param effect Whether this is an effect (vs spell)
     * @return true if mana was successfully paid, false otherwise
     */
    @Override
    public boolean payManaCost(ManaCost toPay, CostPartMana costPartMana, SpellAbility sa, String prompt, ManaConversionMatrix matrix, boolean effect) {
        System.out.println("[ClaudePlayerController] payManaCost called for: " + (sa != null ? sa.getDescription() : "null"));
        System.out.println("[ClaudePlayerController] Mana cost: " + toPay);
        
        // If Claude is available and we're integrated, ask Claude which lands to tap
        if (protocolClient != null && gameInitialized && !connectionFailed) {
            try {
                // Collect available mana sources
                List<Card> manaSources = ComputerUtilMana.getAvailableManaSources(player, false);
                List<Map<String, Object>> sources = new ArrayList<>();
                
                for (Card source : manaSources) {
                    Map<String, Object> sourceData = new HashMap<>();
                    sourceData.put("name", source.getName());
                    sourceData.put("id", source.getId());
                    sourceData.put("mana_abilities", getManaAbilities(source));
                    sourceData.put("is_tapped", source.isTapped());
                    sources.add(sourceData);
                }
                
                // Build request for mana payment decision
                Map<String, Object> decisionData = new HashMap<>();
                decisionData.put("type", "mana_payment");
                decisionData.put("game_id", gameId);
                decisionData.put("player_index", getPlayerIndex(player));
                decisionData.put("spell", sa.getDescription());
                decisionData.put("cost", toPay.toString());
                decisionData.put("available_sources", sources);
                decisionData.put("game_state", serializeGameState());
                
                String requestId = "mana-" + UUID.randomUUID().toString().substring(0, 8);
                ProtocolMessage request = new ProtocolMessage(
                    ProtocolMessage.MessageType.REQUEST,
                    requestId,
                    decisionData
                );
                
                System.out.println("[ClaudePlayerController] Asking Claude for mana payment decision...");
                
                // Send request with timeout
                ProtocolMessage response = protocolClient.sendRequest(request, 5000);
                
                if (response != null && response.getData() != null) {
                    List<Integer> tapLands = (List<Integer>) response.getData().get("tap_sources");
                    if (tapLands != null && !tapLands.isEmpty()) {
                        // Tap the specified lands
                        for (Integer sourceId : tapLands) {
                            for (Card source : manaSources) {
                                if (source.getId() == sourceId) {
                                    System.out.println("[ClaudePlayerController] Claude chose to tap: " + source.getName());
                                    source.tap(true, sa, player);
                                    break;
                                }
                            }
                        }
                        // Now let the system pay from the pool - need to convert ManaCost to ManaCostBeingPaid
                        ManaCostBeingPaid beingPaid = new ManaCostBeingPaid(toPay);
                        return ComputerUtilMana.payManaCost(beingPaid, sa, player, effect);
                    }
                }
            } catch (Exception e) {
                System.err.println("[ClaudePlayerController] Error getting mana payment from Claude: " + e.getMessage());
            }
        }
        
        // Fallback: use auto-tapper
        System.out.println("[ClaudePlayerController] Using auto-tapper fallback");
        boolean result = super.payManaCost(toPay, costPartMana, sa, prompt, matrix, effect);
        
        if (result) {
            System.out.println("[ClaudePlayerController] Mana payment successful (auto-tapped)");
        } else {
            System.out.println("[ClaudePlayerController] Mana payment failed");
        }
        
        return result;
    }
    
    /**
     * Helper method to get mana abilities from a card.
     */
    private List<String> getManaAbilities(Card source) {
        List<String> abilities = new ArrayList<>();
        for (SpellAbility manaAbility : source.getManaAbilities()) {
            if (manaAbility.getManaPart() != null) {
                String colors = manaAbility.getManaPart().getComboColors(manaAbility);
                if (!colors.isEmpty()) {
                    abilities.add("Combo " + colors);
                } else {
                    abilities.add(manaAbility.getManaPart().mana(manaAbility));
                }
            }
        }
        return abilities;
    }
    
    /**
     * Choose targets for a spell ability. This is critical for spells like Kill Shot.
     * 
     * @param sa The spell ability that needs targets
     * @return true if targets were successfully chosen, false otherwise
     */
    @Override
    public boolean chooseTargetsFor(SpellAbility sa) {
        System.out.println("[ClaudePlayerController] chooseTargetsFor called for: " + sa.getDescription());
        
        // Get target restrictions
        TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt == null) {
            System.out.println("[ClaudePlayerController] No target restrictions - no targeting needed");
            return true;
        }
        
        // Collect valid target options
        CardCollection validTargets = new CardCollection();
        List<Player> validPlayers = new ArrayList<>();
        
        // Check creatures for targeting
        if (tgt.canTgtCreature()) {
            // Get all creatures on battlefield
            for (Card c : getGame().getCardsIn(ZoneType.Battlefield)) {
                if (c.isCreature() && sa.canTarget(c)) {
                    validTargets.add(c);
                }
            }
        }
        
        // Check players for targeting  
        if (tgt.canTgtPlayer()) {
            for (Player p : getGame().getPlayers()) {
                if (sa.canTarget(p)) {
                    validPlayers.add(p);
                }
            }
        }
        
        // If no valid targets, we can't cast the spell
        if (validTargets.isEmpty() && validPlayers.isEmpty()) {
            System.out.println("[ClaudePlayerController] No valid targets available!");
            return false;
        }
        
        // If Claude is available, ask Claude to choose targets
        if (protocolClient != null && gameInitialized && !connectionFailed) {
            try {
                // Build target options
                List<Map<String, Object>> targetOptions = new ArrayList<>();
                
                // Add card targets
                for (Card target : validTargets) {
                    Map<String, Object> option = new HashMap<>();
                    option.put("type", "card");
                    option.put("id", target.getId());
                    option.put("name", target.getName());
                    option.put("controller", getPlayerIndex(target.getController()));
                    option.put("power", target.getNetPower());
                    option.put("toughness", target.getNetToughness());
                    option.put("is_tapped", target.isTapped());
                    option.put("is_attacking", target.isAttacking());
                    targetOptions.add(option);
                }
                
                // Add player targets
                for (Player target : validPlayers) {
                    Map<String, Object> option = new HashMap<>();
                    option.put("type", "player");
                    option.put("index", getPlayerIndex(target));
                    option.put("name", target.getName());
                    option.put("life", target.getLife());
                    targetOptions.add(option);
                }
                
                // Build request
                Map<String, Object> decisionData = new HashMap<>();
                decisionData.put("type", "choose_targets");
                decisionData.put("game_id", gameId);
                decisionData.put("player_index", getPlayerIndex(player));
                decisionData.put("spell", sa.getDescription());
                decisionData.put("min_targets", tgt.getMinTargets(sa.getHostCard(), sa));
                decisionData.put("max_targets", tgt.getMaxTargets(sa.getHostCard(), sa));
                decisionData.put("target_options", targetOptions);
                decisionData.put("game_state", serializeGameState());
                
                String requestId = "target-" + UUID.randomUUID().toString().substring(0, 8);
                ProtocolMessage request = new ProtocolMessage(
                    ProtocolMessage.MessageType.REQUEST,
                    requestId,
                    decisionData
                );
                
                System.out.println("[ClaudePlayerController] Asking Claude to choose targets...");
                
                // Send request with timeout
                ProtocolMessage response = protocolClient.sendRequest(request, 5000);
                
                if (response != null && response.getData() != null) {
                    List<Map<String, Object>> chosenTargets = (List<Map<String, Object>>) response.getData().get("targets");
                    if (chosenTargets != null && !chosenTargets.isEmpty()) {
                        // Apply Claude's target choices
                        for (Map<String, Object> targetData : chosenTargets) {
                            String targetType = (String) targetData.get("type");
                            if ("card".equals(targetType)) {
                                Integer cardId = (Integer) targetData.get("id");
                                for (Card card : validTargets) {
                                    if (card.getId() == cardId) {
                                        sa.getTargets().add(card);
                                        System.out.println("[ClaudePlayerController] Claude targeted: " + card.getName());
                                        break;
                                    }
                                }
                            } else if ("player".equals(targetType)) {
                                Integer playerIndex = (Integer) targetData.get("index");
                                for (Player p : validPlayers) {
                                    if (getPlayerIndex(p) == playerIndex) {
                                        sa.getTargets().add(p);
                                        System.out.println("[ClaudePlayerController] Claude targeted player: " + p.getName());
                                        break;
                                    }
                                }
                            }
                        }
                        
                        if (sa.getTargets().size() > 0) {
                            System.out.println("[ClaudePlayerController] Claude chose " + sa.getTargets().size() + " targets");
                            return true;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[ClaudePlayerController] Error getting targets from Claude: " + e.getMessage());
            }
        }
        
        // Fallback to default AI targeting
        System.out.println("[ClaudePlayerController] Using default AI targeting");
        boolean result = super.chooseTargetsFor(sa);
        
        if (result) {
            System.out.println("[ClaudePlayerController] Targets chosen: " + sa.getTargets());
        } else {
            System.out.println("[ClaudePlayerController] Failed to choose targets");
        }
        
        return result;
    }
    
    /**
     * Declare attackers during combat phase.
     * 
     * @param attacker The attacking player
     * @param combat The combat object to modify
     */
    @Override
    public void declareAttackers(Player attacker, Combat combat) {
        System.out.println("[ClaudePlayerController] declareAttackers called");
        
        // For now, don't attack to avoid complications
        // This is a temporary solution until we implement Claude-based combat
        System.out.println("[ClaudePlayerController] WARNING: Skipping attacks (temporary)");
        
        // TODO: Implement full Claude-based combat:
        // 1. Get all creatures that can attack
        // 2. Get all valid attack targets (players/planeswalkers)
        // 3. Send request to Claude with attack options
        // 4. Wait for Claude's attack declarations
        // 5. Apply the chosen attacks to the combat object
    }
    
    /**
     * Declare blockers during combat phase.
     * 
     * @param defender The defending player
     * @param combat The combat object to modify
     */
    @Override
    public void declareBlockers(Player defender, Combat combat) {
        System.out.println("[ClaudePlayerController] declareBlockers called");
        
        // For now, use default AI blocking to prevent taking unnecessary damage
        // This is a temporary solution until we implement Claude-based combat
        System.out.println("[ClaudePlayerController] WARNING: Using default AI blocking (temporary)");
        super.declareBlockers(defender, combat);
        
        // TODO: Implement full Claude-based blocking:
        // 1. Get all attacking creatures
        // 2. Get all available blockers
        // 3. Send request to Claude with blocking options
        // 4. Wait for Claude's blocking assignments
        // 5. Apply the chosen blocks to the combat object
    }
    
    /**
     * Choose cards to discard at end of turn for maximum hand size.
     * 
     * @param numDiscard Number of cards to discard
     * @return Collection of cards to discard
     */
    @Override
    public CardCollection chooseCardsToDiscardToMaximumHandSize(int numDiscard) {
        System.out.println("[ClaudePlayerController] chooseCardsToDiscardToMaximumHandSize called - need to discard " + numDiscard + " cards");
        
        // For now, use default AI discard logic
        // This is a temporary solution until we implement Claude-based discard
        System.out.println("[ClaudePlayerController] WARNING: Using default AI discard (temporary)");
        CardCollection result = super.chooseCardsToDiscardToMaximumHandSize(numDiscard);
        
        System.out.println("[ClaudePlayerController] Discarding: " + result);
        
        // TODO: Implement full Claude-based discard:
        // 1. Get current hand
        // 2. Send request to Claude with hand contents and number to discard
        // 3. Wait for Claude's discard choices
        // 4. Return the chosen cards
        
        return result;
    }
    
    /**
     * Choose permanents to sacrifice.
     * 
     * @param sa The spell ability requiring sacrifice
     * @param min Minimum number to sacrifice
     * @param max Maximum number to sacrifice
     * @param validTargets Valid sacrifice targets
     * @param message Message to display (for UI)
     * @return Collection of permanents to sacrifice
     */
    @Override
    public CardCollectionView choosePermanentsToSacrifice(SpellAbility sa, int min, int max, 
                                                          CardCollectionView validTargets, String message) {
        System.out.println("[ClaudePlayerController] choosePermanentsToSacrifice called - min: " + min + ", max: " + max);
        System.out.println("[ClaudePlayerController] Valid targets: " + validTargets);
        
        // For now, use default AI sacrifice logic
        // This is a temporary solution until we implement Claude-based sacrifice
        System.out.println("[ClaudePlayerController] WARNING: Using default AI sacrifice (temporary)");
        CardCollectionView result = super.choosePermanentsToSacrifice(sa, min, max, validTargets, message);
        
        System.out.println("[ClaudePlayerController] Sacrificing: " + result);
        
        // TODO: Implement full Claude-based sacrifice:
        // 1. Get list of valid sacrifice targets
        // 2. Send request to Claude with sacrifice options
        // 3. Wait for Claude's sacrifice choices
        // 4. Return the chosen permanents
        
        return result;
    }
    
    @Override
    public String toString() {
        return "ClaudePlayerController[" + player.getName() + ", Game: " + gameId + 
               ", Decisions: " + totalDecisionCount + "/" + claudeSuccessfulDecisions + " successful]";
    }
    
    // ==================== COMPREHENSIVE LOGGING OVERRIDES ====================
    // These overrides log all method calls to understand the AI interface flow
    
    @Override
    public boolean confirmAction(SpellAbility sa, PlayerActionConfirmMode mode, String message, List<String> options, Card cardToShow, Map<String, Object> params) {
        System.out.println("[ClaudePlayerController] confirmAction called");
        System.out.println("[ClaudePlayerController]   Mode: " + mode);
        System.out.println("[ClaudePlayerController]   Message: " + message);
        boolean result = super.confirmAction(sa, mode, message, options, cardToShow, params);
        System.out.println("[ClaudePlayerController] confirmAction returning: " + result);
        return result;
    }
    
    @Override
    public boolean confirmReplacementEffect(ReplacementEffect replacementEffect, SpellAbility effectSA, GameEntity affected, String question) {
        System.out.println("[ClaudePlayerController] confirmReplacementEffect called");
        System.out.println("[ClaudePlayerController]   Effect: " + replacementEffect);
        System.out.println("[ClaudePlayerController]   Question: " + question);
        boolean result = super.confirmReplacementEffect(replacementEffect, effectSA, affected, question);
        System.out.println("[ClaudePlayerController] confirmReplacementEffect returning: " + result);
        return result;
    }
    
    @Override
    public boolean confirmTrigger(WrappedAbility wrapper) {
        System.out.println("[ClaudePlayerController] confirmTrigger called");
        System.out.println("[ClaudePlayerController]   Trigger: " + (wrapper != null ? wrapper.toString() : "null"));
        boolean result = super.confirmTrigger(wrapper);
        System.out.println("[ClaudePlayerController] confirmTrigger returning: " + result);
        return result;
    }
    
    @Override
    public CardCollection orderBlockers(Card attacker, CardCollection blockers) {
        System.out.println("[ClaudePlayerController] orderBlockers called");
        System.out.println("[ClaudePlayerController]   Attacker: " + attacker.getName());
        System.out.println("[ClaudePlayerController]   Blockers: " + blockers.size());
        CardCollection result = super.orderBlockers(attacker, blockers);
        System.out.println("[ClaudePlayerController] orderBlockers completed");
        return result;
    }
    
    @Override
    public boolean playTrigger(Card host, WrappedAbility wrapperAbility, boolean isMandatory) {
        System.out.println("[ClaudePlayerController] playTrigger called");
        System.out.println("[ClaudePlayerController]   Host: " + host.getName());
        System.out.println("[ClaudePlayerController]   Mandatory: " + isMandatory);
        boolean result = super.playTrigger(host, wrapperAbility, isMandatory);
        System.out.println("[ClaudePlayerController] playTrigger returning: " + result);
        return result;
    }
    
    @Override
    public boolean chooseBinary(SpellAbility sa, String question, BinaryChoiceType kindOfChoice, Boolean defaultVal) {
        System.out.println("[ClaudePlayerController] chooseBinary called");
        System.out.println("[ClaudePlayerController]   Question: " + question);
        System.out.println("[ClaudePlayerController]   Choice type: " + kindOfChoice);
        boolean result = super.chooseBinary(sa, question, kindOfChoice, defaultVal);
        System.out.println("[ClaudePlayerController] chooseBinary returning: " + result);
        return result;
    }
    
    @Override
    public List<AbilitySub> chooseModeForAbility(SpellAbility sa, List<AbilitySub> possible, int min, int num, boolean allowRepeat) {
        System.out.println("[ClaudePlayerController] chooseModeForAbility called");
        System.out.println("[ClaudePlayerController]   Possible modes: " + possible.size());
        System.out.println("[ClaudePlayerController]   Min/Num: " + min + "/" + num);
        List<AbilitySub> result = super.chooseModeForAbility(sa, possible, min, num, allowRepeat);
        System.out.println("[ClaudePlayerController] chooseModeForAbility returning: " + (result != null ? result.size() + " modes" : "null"));
        return result;
    }
    
    @Override
    public byte chooseColor(String message, SpellAbility sa, ColorSet colors) {
        System.out.println("[ClaudePlayerController] chooseColor called");
        System.out.println("[ClaudePlayerController]   Message: " + message);
        System.out.println("[ClaudePlayerController]   Available colors: " + colors);
        byte result = super.chooseColor(message, sa, colors);
        System.out.println("[ClaudePlayerController] chooseColor returning: " + result);
        return result;
    }
    
    @Override
    public <T extends GameEntity> T chooseSingleEntityForEffect(FCollectionView<T> optionList, DelayedReveal delayedReveal, SpellAbility sa, String title, boolean isOptional, Player targetedPlayer, Map<String, Object> params) {
        System.out.println("[ClaudePlayerController] chooseSingleEntityForEffect called");
        System.out.println("[ClaudePlayerController]   Title: " + title);
        System.out.println("[ClaudePlayerController]   Options: " + (optionList != null ? optionList.size() : 0));
        System.out.println("[ClaudePlayerController]   Optional: " + isOptional);
        T result = super.chooseSingleEntityForEffect(optionList, delayedReveal, sa, title, isOptional, targetedPlayer, params);
        System.out.println("[ClaudePlayerController] chooseSingleEntityForEffect returning: " + result);
        return result;
    }
    
    @Override
    public CardCollectionView chooseCardsForEffect(CardCollectionView sourceList, SpellAbility sa, String title, int min, int max, boolean isOptional, Map<String, Object> params) {
        System.out.println("[ClaudePlayerController] chooseCardsForEffect called");
        System.out.println("[ClaudePlayerController]   Title: " + title);
        System.out.println("[ClaudePlayerController]   Source cards: " + sourceList.size());
        System.out.println("[ClaudePlayerController]   Min/Max: " + min + "/" + max);
        CardCollectionView result = super.chooseCardsForEffect(sourceList, sa, title, min, max, isOptional, params);
        System.out.println("[ClaudePlayerController] chooseCardsForEffect returning: " + (result != null ? result.size() + " cards" : "null"));
        return result;
    }
    
    @Override
    public String chooseCardName(SpellAbility sa, List<ICardFace> faces, String message) {
        System.out.println("[ClaudePlayerController] chooseCardName called");
        System.out.println("[ClaudePlayerController]   Message: " + message);
        System.out.println("[ClaudePlayerController]   Faces available: " + (faces != null ? faces.size() : 0));
        String result = super.chooseCardName(sa, faces, message);
        System.out.println("[ClaudePlayerController] chooseCardName returning: " + result);
        return result;
    }
    
    
    @Override
    public void playSpellAbilityNoStack(SpellAbility effectSA, boolean canSetupTargets) {
        System.out.println("[ClaudePlayerController] playSpellAbilityNoStack called");
        System.out.println("[ClaudePlayerController]   SpellAbility: " + effectSA.getDescription());
        System.out.println("[ClaudePlayerController]   Can setup targets: " + canSetupTargets);
        super.playSpellAbilityNoStack(effectSA, canSetupTargets);
        System.out.println("[ClaudePlayerController] playSpellAbilityNoStack completed");
    }
    
    @Override
    public boolean playChosenSpellAbility(SpellAbility sa) {
        System.out.println("[ClaudePlayerController] playChosenSpellAbility called");
        System.out.println("[ClaudePlayerController]   SpellAbility: " + sa.getDescription());
        boolean result = super.playChosenSpellAbility(sa);
        System.out.println("[ClaudePlayerController] playChosenSpellAbility returning: " + result);
        return result;
    }
    
    @Override
    public Player chooseStartingPlayer(boolean isFirstGame) {
        System.out.println("[ClaudePlayerController] chooseStartingPlayer called");
        System.out.println("[ClaudePlayerController]   Is first game: " + isFirstGame);
        Player result = super.chooseStartingPlayer(isFirstGame);
        System.out.println("[ClaudePlayerController] chooseStartingPlayer returning: " + (result != null ? result.getName() : "null"));
        return result;
    }
    
    @Override
    public boolean mulliganKeepHand(Player firstPlayer, int cardsToReturn) {
        System.out.println("[ClaudePlayerController] mulliganKeepHand called");
        System.out.println("[ClaudePlayerController]   First player: " + (firstPlayer != null ? firstPlayer.getName() : "null"));
        System.out.println("[ClaudePlayerController]   Cards to return: " + cardsToReturn);
        boolean result = super.mulliganKeepHand(firstPlayer, cardsToReturn);
        System.out.println("[ClaudePlayerController] mulliganKeepHand returning: " + result);
        return result;
    }
    
    @Override
    public TargetChoices chooseNewTargetsFor(SpellAbility ability, Predicate<GameObject> filter, boolean optional) {
        System.out.println("[ClaudePlayerController] chooseNewTargetsFor called");
        System.out.println("[ClaudePlayerController]   SpellAbility: " + ability.getDescription());
        System.out.println("[ClaudePlayerController]   Optional: " + optional);
        TargetChoices result = super.chooseNewTargetsFor(ability, filter, optional);
        System.out.println("[ClaudePlayerController] chooseNewTargetsFor returning: " + (result != null ? "targets chosen" : "null"));
        return result;
    }
    
    @Override
    public CardCollectionView orderMoveToZoneList(CardCollectionView cards, ZoneType destinationZone, SpellAbility source) {
        System.out.println("[ClaudePlayerController] orderMoveToZoneList called");
        System.out.println("[ClaudePlayerController]   Cards: " + cards.size());
        System.out.println("[ClaudePlayerController]   Destination: " + destinationZone);
        CardCollectionView result = super.orderMoveToZoneList(cards, destinationZone, source);
        System.out.println("[ClaudePlayerController] orderMoveToZoneList completed");
        return result;
    }
}