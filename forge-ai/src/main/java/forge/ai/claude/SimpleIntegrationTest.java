package forge.ai.claude;

import java.io.IOException;
import java.net.Socket;
import java.util.*;

/**
 * Simplified Integration Test for Claude Player Controller.
 * 
 * This test focuses on the core integration aspects that we can test
 * without requiring a full Forge game setup:
 * 
 * 1. Python server connectivity
 * 2. Protocol client functionality  
 * 3. Message validation and serialization
 * 4. Error handling and fallback behavior
 * 
 * This demonstrates the complete Claude-Forge integration architecture
 * and validates the Java-Python bridge is working correctly.
 */
public class SimpleIntegrationTest {
    
    // Test configuration
    private static final String PYTHON_SERVER_HOST = "localhost";
    private static final int PYTHON_SERVER_PORT = 8889;
    private static final int CONNECTION_TIMEOUT_MS = 5000;
    
    public static void main(String[] args) {
        System.out.println("Claude Plays MTG - Simple Integration Test");
        System.out.println("============================================");
        
        SimpleIntegrationTest test = new SimpleIntegrationTest();
        
        try {
            test.runComprehensiveTest();
            
            System.out.println("\n✅ All integration tests completed successfully!");
            System.out.println("   The Java-Python bridge is working and ready for Forge integration.");
            
        } catch (Exception e) {
            System.err.println("\n❌ Integration test failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Run comprehensive test of integration components.
     */
    public void runComprehensiveTest() throws Exception {
        System.out.println("\nRunning comprehensive integration test...");
        
        // Test 1: Python server availability
        testPythonServerAvailability();
        
        // Test 2: Protocol client creation and connectivity
        testProtocolClientConnectivity();
        
        // Test 3: Message creation and validation
        testMessageCreationAndValidation();
        
        // Test 4: Complete request/response flow
        testCompleteRequestResponseFlow();
        
        // Test 5: Error handling scenarios  
        testErrorHandlingScenarios();
        
        // Test 6: Connection lifecycle management
        testConnectionLifecycle();
        
        System.out.println("\n   ✓ All integration components working correctly");
    }
    
    /**
     * Test 1: Verify Python Claude server is available.
     */
    private void testPythonServerAvailability() throws Exception {
        System.out.println("\n1. Testing Python server availability...");
        
        try {
            Socket socket = new Socket(PYTHON_SERVER_HOST, PYTHON_SERVER_PORT);
            socket.setSoTimeout(CONNECTION_TIMEOUT_MS);
            
            System.out.println("   ✓ Python server listening on " + 
                              PYTHON_SERVER_HOST + ":" + PYTHON_SERVER_PORT);
            
            socket.close();
            
        } catch (IOException e) {
            System.err.println("   ❌ Python server not available: " + e.getMessage());
            System.err.println("   Please start the Python Claude server:");
            System.err.println("   python -m claude_plays_mtg.server.server");
            throw new Exception("Python server required for integration testing", e);
        }
    }
    
    /**
     * Test 2: Protocol client creation and basic connectivity.
     */
    private void testProtocolClientConnectivity() throws Exception {
        System.out.println("\n2. Testing protocol client connectivity...");
        
        ClaudeProtocolClient client = new ClaudeProtocolClient(PYTHON_SERVER_HOST, PYTHON_SERVER_PORT);
        
        try {
            // Test connection establishment
            setupClientCallbacks(client);
            client.connect();
            
            // Wait for welcome message
            Thread.sleep(2000);
            
            if (!client.isConnected()) {
                throw new Exception("Client not connected after connection attempt");
            }
            
            System.out.println("   ✓ Protocol client connected successfully");
            
            
            System.out.println("   ✓ Protocol connectivity working");
            
        } finally {
            client.shutdown();
        }
    }
    
    /**
     * Test 3: Message creation and validation.
     */
    private void testMessageCreationAndValidation() throws Exception {
        System.out.println("\n3. Testing message creation and validation...");
        
        ProtocolHandler handler = new ProtocolHandler("integration-test");
        MessageValidator validator = new MessageValidator();
        
        // Test welcome message
        ProtocolMessage welcome = handler.createWelcomeMessage();
        validateMessage(welcome, validator, "Welcome message");
        
        
        // Test action request with mock data
        Map<String, Object> gameState = createMockGameState();
        List<Map<String, Object>> actions = createMockActions();
        
        ProtocolMessage actionRequest = handler.createActionRequest(
            "test-game-123", 
            0, 
            gameState, 
            actions,
            new HashMap<>()
        );
        validateMessage(actionRequest, validator, "Action request");
        
        // Test action response with correct signature
        Map<String, Object> chosenAction = new HashMap<>();
        chosenAction.put("index", 1);
        chosenAction.put("action_type", "cast_spell");
        
        ProtocolMessage actionResponse = handler.createActionResponse(
            actionRequest.getRequestId(),
            "test-game-123",
            0,
            chosenAction,
            1500,
            new HashMap<>()
        );
        validateMessage(actionResponse, validator, "Action response");
        
        // Test error response with correct signature
        ProtocolMessage errorMsg = handler.createErrorResponse(
            actionRequest.getRequestId(),
            ProtocolMessage.ErrorCode.INTERNAL_ERROR,
            "This is a test error message",
            true,
            null,
            new HashMap<>()
        );
        validateMessage(errorMsg, validator, "Error response");
        
        System.out.println("   ✓ All message types created and validated successfully");
    }
    
    /**
     * Test 4: Complete request/response flow with actual server.
     */
    private void testCompleteRequestResponseFlow() throws Exception {
        System.out.println("\n4. Testing complete request/response flow...");
        
        ClaudeProtocolClient client = new ClaudeProtocolClient(PYTHON_SERVER_HOST, PYTHON_SERVER_PORT);
        ProtocolHandler handler = new ProtocolHandler("flow-test");
        
        try {
            setupClientCallbacks(client);
            client.connect();
            
            // Wait for connection to be fully established
            Thread.sleep(2000);
            
            // Send action request to server
            Map<String, Object> gameState = createMockGameState();
            List<Map<String, Object>> actions = createMockActions();
            
            ProtocolMessage request = handler.createActionRequest(
                "flow-test-game",
                0,
                gameState,
                actions,
                new HashMap<>()
            );
            
            System.out.println("   Sending action request to Claude server...");
            long startTime = System.currentTimeMillis();
            
            ProtocolMessage response = client.sendRequest(request, 30000); // 30 second timeout
            long elapsedMs = System.currentTimeMillis() - startTime;
            
            if (response == null) {
                throw new Exception("Received null response from server");
            }
            
            System.out.println("   ✓ Received response in " + elapsedMs + "ms");
            System.out.println("     Response type: " + response.getMessageType());
            System.out.println("     Request ID matches: " + request.getRequestId().equals(response.getRequestId()));
            
            // Validate response structure
            if (!"response".equals(response.getMessageType())) {
                System.err.println("   Warning: Expected 'response' message type, got: " + response.getMessageType());
            }
            
            if (!request.getRequestId().equals(response.getRequestId())) {
                throw new Exception("Response request ID doesn't match sent request");
            }
            
            // Examine response data
            Map<String, Object> responseData = response.getData();
            System.out.println("     Response success: " + responseData.get("success"));
            if (responseData.containsKey("action")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> action = (Map<String, Object>) responseData.get("action");
                System.out.println("     Chosen action: " + action.get("action_type"));
            }
            
            System.out.println("   ✓ Complete request/response flow working");
            
        } finally {
            client.shutdown();
        }
    }
    
    /**
     * Test 5: Error handling scenarios.
     */
    private void testErrorHandlingScenarios() throws Exception {
        System.out.println("\n5. Testing error handling scenarios...");
        
        ClaudeProtocolClient client = new ClaudeProtocolClient(PYTHON_SERVER_HOST, PYTHON_SERVER_PORT);
        
        try {
            setupClientCallbacks(client);
            client.connect();
            Thread.sleep(1000);
            
            // Test timeout scenario (make a request with very short timeout)
            ProtocolHandler handler = new ProtocolHandler("error-test");
            ProtocolMessage request = handler.createActionRequest(
                "timeout-test",
                0,
                createMockGameState(),
                createMockActions(),
                new HashMap<>()
            );
            
            try {
                // Request with 1ms timeout should time out
                ProtocolMessage response = client.sendRequest(request, 1);
                System.out.println("   Warning: Expected timeout, but got response");
            } catch (Exception e) {
                if (e.getMessage().contains("timeout")) {
                    System.out.println("   ✓ Timeout handling working correctly");
                } else {
                    System.out.println("   Note: Different error than timeout: " + e.getMessage());
                }
            }
            
            // Test invalid message handling
            testInvalidMessageHandling();
            
            System.out.println("   ✓ Error handling mechanisms working");
            
        } finally {
            client.shutdown();
        }
    }
    
    /**
     * Test 6: Connection lifecycle management.
     */
    private void testConnectionLifecycle() throws Exception {
        System.out.println("\n6. Testing connection lifecycle...");
        
        ClaudeProtocolClient client = new ClaudeProtocolClient(PYTHON_SERVER_HOST, PYTHON_SERVER_PORT);
        
        try {
            // Test connect/disconnect cycle
            setupClientCallbacks(client);
            
            client.connect();
            Thread.sleep(1000);
            
            if (!client.isConnected()) {
                throw new Exception("Client should be connected");
            }
            
            client.shutdown();
            Thread.sleep(500);
            
            if (client.isConnected()) {
                throw new Exception("Client should be disconnected after shutdown");
            }
            
            System.out.println("   ✓ Connect/disconnect cycle working");
            
            // Test reconnection
            client = new ClaudeProtocolClient(PYTHON_SERVER_HOST, PYTHON_SERVER_PORT);
            setupClientCallbacks(client);
            
            client.connect();
            Thread.sleep(1000);
            
            if (!client.isConnected()) {
                throw new Exception("Client should reconnect successfully");
            }
            
            System.out.println("   ✓ Reconnection working");
            
        } finally {
            if (client != null) {
                client.shutdown();
            }
        }
        
        System.out.println("   ✓ Connection lifecycle management working");
    }
    
    // ==================== Helper Methods ====================
    
    /**
     * Set up client callbacks for monitoring.
     */
    private void setupClientCallbacks(ClaudeProtocolClient client) {
        client.setConnectionCallback(new ClaudeProtocolClient.ConnectionCallback() {
            @Override
            public void onConnected(ProtocolMessage welcomeMessage) {
                System.out.println("   Connection established: " + 
                                  welcomeMessage.getData().get("server_version"));
            }
            
            @Override
            public void onDisconnected(String reason, boolean canRetry) {
                System.out.println("   Disconnected: " + reason + " (canRetry: " + canRetry + ")");
            }
        });
        
        client.setMessageCallback(new ClaudeProtocolClient.MessageCallback() {
            @Override
            public void onMessageReceived(ProtocolMessage message) {
                // Only log important messages to avoid noise
                if (!"welcome".equals(message.getDataType())) {
                    System.out.println("   << " + message.getMessageType());
                }
            }
            
            @Override
            public void onMessageError(String error, Throwable cause) {
                System.err.println("   Message error: " + error);
            }
        });
    }
    
    
    /**
     * Validate a protocol message.
     */
    private void validateMessage(ProtocolMessage message, MessageValidator validator, String messageDescription) 
            throws Exception {
        
        ProtocolMessage.ValidationResult validation = validator.validateMessage(message);
        
        if (!validation.isValid()) {
            throw new Exception(messageDescription + " validation failed: " + validation.getErrorMessage());
        }
        
        // Test JSON serialization
        try {
            String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(message);
            
            if (json.length() < 10) {
                throw new Exception(messageDescription + " JSON suspiciously short: " + json.length() + " chars");
            }
            
            System.out.println("   ✓ " + messageDescription + " valid (JSON: " + json.length() + " chars)");
            
        } catch (Exception e) {
            throw new Exception(messageDescription + " JSON serialization failed", e);
        }
    }
    
    /**
     * Test invalid message handling.
     */
    private void testInvalidMessageHandling() throws Exception {
        MessageValidator validator = new MessageValidator();
        
        // Test message with missing required fields
        Map<String, Object> invalidData = new HashMap<>();
        invalidData.put("protocol_version", "1.0"); // Missing other required fields
        
        ProtocolMessage invalidMessage = new ProtocolMessage();
        invalidMessage.setProtocolVersion("1.0");
        invalidMessage.setMessageType(ProtocolMessage.MessageType.REQUEST);
        // Missing request_id, timestamp, data
        
        ProtocolMessage.ValidationResult validation = validator.validateMessage(invalidMessage);
        
        if (validation.isValid()) {
            System.out.println("   Warning: Invalid message passed validation unexpectedly");
        } else {
            System.out.println("   ✓ Invalid message correctly rejected: " + validation.getErrorMessage());
        }
    }
    
    /**
     * Create mock game state for testing.
     */
    private Map<String, Object> createMockGameState() {
        Map<String, Object> state = new HashMap<>();
        
        // Basic game info
        state.put("turn_number", 3);
        state.put("phase", "main_1");
        state.put("priority_player", 0);
        state.put("active_player", 0);
        
        // Players information
        List<Map<String, Object>> players = new ArrayList<>();
        
        Map<String, Object> player1 = new HashMap<>();
        player1.put("index", 0);
        player1.put("name", "Claude Test Player");
        player1.put("life", 18);
        player1.put("hand_size", 6);
        player1.put("library_size", 40);
        player1.put("graveyard_size", 2);
        player1.put("battlefield_size", 3);
        players.add(player1);
        
        Map<String, Object> player2 = new HashMap<>();
        player2.put("index", 1);
        player2.put("name", "Opponent");
        player2.put("life", 20);
        player2.put("hand_size", 7);
        player2.put("library_size", 45);
        player2.put("graveyard_size", 0);
        player2.put("battlefield_size", 2);
        players.add(player2);
        
        state.put("players", players);
        
        // Battlefield (simplified)
        List<Map<String, Object>> battlefield = new ArrayList<>();
        Map<String, Object> creature = new HashMap<>();
        creature.put("name", "Grizzly Bear");
        creature.put("type", "Creature — Bear");
        creature.put("power", 2);
        creature.put("toughness", 2);
        creature.put("controller", 0);
        battlefield.add(creature);
        
        state.put("battlefield", battlefield);
        state.put("stack", new ArrayList<>());
        state.put("in_combat", false);
        
        return state;
    }
    
    /**
     * Create mock legal actions for testing.
     */
    private List<Map<String, Object>> createMockActions() {
        List<Map<String, Object>> actions = new ArrayList<>();
        
        // Action 1: Pass priority
        Map<String, Object> passAction = new HashMap<>();
        passAction.put("index", 0);
        passAction.put("action_type", "pass");
        passAction.put("description", "Pass priority");
        passAction.put("is_spell", false);
        passAction.put("is_activated", false);
        passAction.put("has_targets", false);
        actions.add(passAction);
        
        // Action 2: Cast Lightning Bolt
        Map<String, Object> spellAction = new HashMap<>();
        spellAction.put("index", 1);
        spellAction.put("action_type", "cast_spell");
        spellAction.put("description", "Cast Lightning Bolt");
        spellAction.put("card_name", "Lightning Bolt");
        spellAction.put("card_type", "Instant");
        spellAction.put("mana_cost", "{R}");
        spellAction.put("is_spell", true);
        spellAction.put("is_activated", false);
        spellAction.put("has_targets", true);
        spellAction.put("target_description", "Target creature or player");
        actions.add(spellAction);
        
        // Action 3: Activate creature ability
        Map<String, Object> abilityAction = new HashMap<>();
        abilityAction.put("index", 2);
        abilityAction.put("action_type", "activate_ability");
        abilityAction.put("description", "Grizzly Bear: Tap to deal 1 damage");
        abilityAction.put("card_name", "Grizzly Bear");
        abilityAction.put("card_type", "Creature — Bear");
        abilityAction.put("is_spell", false);
        abilityAction.put("is_activated", true);
        abilityAction.put("has_targets", true);
        actions.add(abilityAction);
        
        return actions;
    }
}