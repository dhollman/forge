package forge.ai.claude;

import java.util.*;

/**
 * Handles protocol message processing and routing for Claude integration.
 * 
 * This is the Java equivalent of the Python ProtocolHandler class,
 * providing comprehensive message creation, validation, and lifecycle management.
 * 
 * Provides:
 * - Message creation for different request types
 * - Message validation and parsing
 * - Request/response correlation tracking  
 * - Error message generation
 * 
 * Used by both server and client sides of the communication.
 */
public class ProtocolHandler {
    
    private final String componentName;
    private final Map<String, Long> pendingRequests; // request_id -> start_time (milliseconds)
    private final String connectionId;
    
    /**
     * Initialize protocol handler.
     * 
     * @param componentName Identifier for logging (e.g., "server", "client")
     */
    public ProtocolHandler(String componentName) {
        this.componentName = componentName != null ? componentName : "unknown";
        this.pendingRequests = new HashMap<>();
        this.connectionId = this.componentName + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * Create welcome message for connection establishment.
     * 
     * Sent by server when client connects to confirm compatibility
     * and provide server capabilities. This is the Java equivalent of 
     * Python's create_welcome_message() method.
     * 
     * @return Welcome notification message
     */
    public ProtocolMessage createWelcomeMessage() {
        System.out.println("[" + componentName + "] Creating welcome message");
        
        Map<String, Object> data = new HashMap<>();
        data.put("type", "welcome");
        data.put("server_version", "0.1.0");
        data.put("protocol_version", ProtocolMessage.PROTOCOL_VERSION); // Required for validation
        data.put("supported_protocol_versions", Arrays.asList("1.0"));
        data.put("capabilities", Arrays.asList(
            "game_state_full",
            "action_reasoning", 
            "deck_conversion",
            "error_recovery"
        ));
        data.put("max_concurrent_games", 1);
        
        return new ProtocolMessage(ProtocolMessage.MessageType.NOTIFICATION, data);
    }
    
    /**
     * Create action request message.
     * 
     * Used by Java client to request Claude to choose an action.
     * This matches the Python create_action_request() method.
     * 
     * @param gameId Unique identifier for the game
     * @param requestingPlayer Player index requesting the action
     * @param gameState Complete game state (GameView JSON)
     * @param legalActions List of valid actions Claude can choose from
     * @param decisionContext Optional additional context for Claude
     * @return Action request message with unique request_id
     */
    public ProtocolMessage createActionRequest(
            String gameId,
            int requestingPlayer,
            Map<String, Object> gameState,
            List<Map<String, Object>> legalActions,
            Map<String, Object> decisionContext) {
        
        String requestId = "action-req-" + UUID.randomUUID().toString().substring(0, 8);
        System.out.println("[" + componentName + "] Creating action request " + requestId + 
                          " for player " + requestingPlayer);
        
        // Track request timing (milliseconds since epoch)
        pendingRequests.put(requestId, System.currentTimeMillis());
        
        Map<String, Object> data = new HashMap<>();
        data.put("type", "get_action");
        data.put("game_id", gameId);
        data.put("requesting_player", requestingPlayer);
        data.put("game_state", gameState);
        data.put("legal_actions", legalActions);
        data.put("decision_context", decisionContext != null ? decisionContext : new HashMap<>());
        
        return new ProtocolMessage(ProtocolMessage.MessageType.REQUEST, requestId, data);
    }
    
    /**
     * Create action response message.
     * 
     * Used by Python server to send Claude's chosen action back to Java.
     * This matches the Python create_action_response() method.
     * 
     * @param requestId Matching request_id from original request
     * @param gameId Game identifier
     * @param requestingPlayer Player who requested the action
     * @param action Claude's chosen action with reasoning
     * @param executionTimeMs Total processing time in milliseconds
     * @param claudeMetadata Optional Claude API usage metadata
     * @return Action response message
     */
    public ProtocolMessage createActionResponse(
            String requestId,
            String gameId,
            int requestingPlayer,
            Map<String, Object> action,
            int executionTimeMs,
            Map<String, Object> claudeMetadata) {
        
        System.out.println("[" + componentName + "] Creating action response for request " + requestId);
        
        // Clear request tracking
        pendingRequests.remove(requestId);
        
        Map<String, Object> data = new HashMap<>();
        data.put("type", "action_response");
        data.put("game_id", gameId);
        data.put("requesting_player", requestingPlayer);
        data.put("success", true);
        data.put("action", action);
        data.put("execution_time_ms", executionTimeMs);
        data.put("claude_metadata", claudeMetadata != null ? claudeMetadata : new HashMap<>());
        
        return new ProtocolMessage(ProtocolMessage.MessageType.RESPONSE, requestId, data);
    }
    
    /**
     * Create error response message.
     * 
     * Used when processing fails or exceptions occur.
     * This matches the Python create_error_response() method.
     * 
     * @param requestId Original request ID (if applicable, null for standalone errors)
     * @param errorCode Structured error code for programmatic handling
     * @param errorMessage Human-readable error description
     * @param canRetry Whether the operation can be retried
     * @param fallbackAction Optional safe fallback action
     * @param debugInfo Optional debugging information
     * @return Error response message
     */
    public ProtocolMessage createErrorResponse(
            String requestId,
            ProtocolMessage.ErrorCode errorCode,
            String errorMessage,
            boolean canRetry,
            Map<String, Object> fallbackAction,
            Map<String, Object> debugInfo) {
        
        System.err.println("[" + componentName + "] Creating error response: " + 
                          errorCode + " - " + errorMessage);
        
        // Clear request tracking if applicable
        if (requestId != null) {
            pendingRequests.remove(requestId);
        }
        
        Map<String, Object> data = new HashMap<>();
        data.put("type", "error_response");
        data.put("error_code", errorCode.toString());
        data.put("error_message", errorMessage);
        data.put("can_retry", canRetry);
        data.put("retry_after_ms", canRetry ? 1000 : null);
        data.put("fallback_action", fallbackAction);
        data.put("debug_info", debugInfo != null ? debugInfo : new HashMap<>());
        
        return new ProtocolMessage(ProtocolMessage.MessageType.ERROR, requestId, data);
    }
    
    
    /**
     * Check for timed out requests.
     * 
     * This matches the Python check_request_timeouts() method,
     * identifying requests that have exceeded the Claude API timeout.
     * 
     * @return List of request_ids that have timed out
     */
    public List<String> checkRequestTimeouts() {
        List<String> timedOut = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
        long timeoutMs = ProtocolMessage.DEFAULT_TIMEOUT_CLAUDE_SECONDS * 1000L;
        
        // Iterate through pending requests (copy to avoid concurrent modification)
        Map<String, Long> requestsCopy = new HashMap<>(pendingRequests);
        for (Map.Entry<String, Long> entry : requestsCopy.entrySet()) {
            String requestId = entry.getKey();
            long startTime = entry.getValue();
            
            if (currentTime - startTime > timeoutMs) {
                timedOut.add(requestId);
                double durationSeconds = (currentTime - startTime) / 1000.0;
                System.err.println("[" + componentName + "] Request " + requestId + 
                                   " timed out after " + String.format("%.1f", durationSeconds) + " seconds");
            }
        }
        
        return timedOut;
    }
    
    /**
     * Clear request tracking and return duration.
     * 
     * This matches the Python clear_request() method.
     * 
     * @param requestId Request to clear
     * @return Request duration in seconds, or -1 if not found
     */
    public double clearRequest(String requestId) {
        Long startTime = pendingRequests.remove(requestId);
        if (startTime == null) {
            return -1.0;
        }
        
        long duration = System.currentTimeMillis() - startTime;
        return duration / 1000.0; // Convert to seconds
    }
    
    /**
     * Create initialize game request message.
     * 
     * Used to set up a new game session with player information.
     * 
     * @param gameId Unique identifier for the new game
     * @param players List of player configurations  
     * @return Initialize game request message
     */
    public ProtocolMessage createInitializeGameRequest(
            String gameId,
            List<Map<String, Object>> players) {
        
        String requestId = "init-req-" + UUID.randomUUID().toString().substring(0, 8);
        System.out.println("[" + componentName + "] Creating initialize game request " + requestId);
        
        // Track request timing
        pendingRequests.put(requestId, System.currentTimeMillis());
        
        Map<String, Object> data = new HashMap<>();
        data.put("type", "initialize_game");
        data.put("game_id", gameId);
        data.put("players", players);
        
        return new ProtocolMessage(ProtocolMessage.MessageType.REQUEST, requestId, data);
    }
    
    /**
     * Create game ready response message.
     * 
     * Used to confirm successful game initialization.
     * 
     * @param requestId Matching request_id from initialize request
     * @param gameId Game identifier  
     * @return Game ready response message
     */
    public ProtocolMessage createGameReadyResponse(String requestId, String gameId) {
        System.out.println("[" + componentName + "] Creating game ready response for request " + requestId);
        
        // Clear request tracking
        pendingRequests.remove(requestId);
        
        Map<String, Object> data = new HashMap<>();
        data.put("type", "game_ready");
        data.put("game_id", gameId);
        data.put("success", true);
        
        return new ProtocolMessage(ProtocolMessage.MessageType.RESPONSE, requestId, data);
    }
    
    // Getters for connection tracking
    
    public String getComponentName() {
        return componentName;
    }
    
    public String getConnectionId() {
        return connectionId;
    }
    
    public int getPendingRequestCount() {
        return pendingRequests.size();
    }
    
}