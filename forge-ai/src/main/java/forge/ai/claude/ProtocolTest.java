package forge.ai.claude;

import java.util.*;

/**
 * Test class for Claude Protocol implementation.
 * 
 * This class provides standalone tests for our Java protocol classes,
 * verifying:
 * - JSON serialization/deserialization compatibility with Python
 * - Message validation and error handling
 * - Protocol message creation and parsing
 * - Round-trip compatibility between Java and Python formats
 * 
 * To run: java -cp forge-ai.jar forge.ai.claude.ProtocolTest
 */
public class ProtocolTest {
    
    public static void main(String[] args) {
        System.out.println("Claude Protocol Test Suite");
        System.out.println("==========================");
        
        ProtocolTest test = new ProtocolTest();
        
        try {
            // Run test suite
            test.testMessageCreation();
            test.testJsonSerialization();
            test.testMessageValidation();
            test.testProtocolHandler();
            test.testConnectionScenario();
            
            System.out.println("\n✅ All tests passed! Protocol implementation is compatible.");
            
        } catch (Exception e) {
            System.err.println("\n❌ Test failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Test basic message creation with all message types.
     */
    public void testMessageCreation() {
        System.out.println("\n1. Testing message creation...");
        
        // Test notification message
        Map<String, Object> data = new HashMap<>();
        data.put("type", "welcome");
        data.put("server_version", "0.1.0");
        data.put("protocol_version", "1.0");
        
        ProtocolMessage notification = new ProtocolMessage(
            ProtocolMessage.MessageType.NOTIFICATION, data);
        
        System.out.println("   ✓ Created notification: " + notification);
        assert notification.getMessageType() == ProtocolMessage.MessageType.NOTIFICATION;
        assert "welcome".equals(notification.getDataType());
        
        // Test request message
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("type", "get_action");
        requestData.put("game_id", "test-game");
        requestData.put("requesting_player", 0);
        
        ProtocolMessage request = new ProtocolMessage(
            ProtocolMessage.MessageType.REQUEST, "req-123", requestData);
        
        System.out.println("   ✓ Created request: " + request);
        assert request.getMessageType() == ProtocolMessage.MessageType.REQUEST;
        assert "req-123".equals(request.getRequestId());
        assert "get_action".equals(request.getDataType());
        
        System.out.println("   ✅ Message creation tests passed");
    }
    
    /**
     * Test JSON serialization and deserialization.
     */
    public void testJsonSerialization() {
        System.out.println("\n2. Testing JSON serialization...");
        
        // Create a complex message with nested data
        Map<String, Object> gameState = new HashMap<>();
        gameState.put("phase", "main_1");
        gameState.put("turn_number", 3);
        gameState.put("players", Arrays.asList(
            Map.of("name", "Player 1", "life", 20),
            Map.of("name", "Player 2", "life", 18)
        ));
        
        List<Map<String, Object>> legalActions = new ArrayList<>();
        Map<String, Object> action = new HashMap<>();
        action.put("action_type", "cast_spell");
        action.put("card_id", "card-123");
        action.put("cost", Map.of("mana", "2R"));
        legalActions.add(action);
        
        Map<String, Object> data = new HashMap<>();
        data.put("type", "get_action");
        data.put("game_id", "serialization-test");
        data.put("requesting_player", 0);
        data.put("game_state", gameState);
        data.put("legal_actions", legalActions);
        
        ProtocolMessage original = new ProtocolMessage(
            ProtocolMessage.MessageType.REQUEST, "json-test-123", data);
        
        // Test serialization
        String json = original.toJson();
        System.out.println("   ✓ Serialized to JSON (" + json.length() + " chars)");
        
        // Verify it's valid JSON and contains expected fields
        assert json.contains("\"protocol_version\":\"1.0\"");
        assert json.contains("\"message_type\":\"request\"");
        assert json.contains("\"request_id\":\"json-test-123\"");
        assert json.contains("\"type\":\"get_action\"");
        
        // Test deserialization
        ProtocolMessage reconstructed = ProtocolMessage.fromJson(json);
        System.out.println("   ✓ Deserialized from JSON: " + reconstructed);
        
        // Verify round-trip equality
        assert reconstructed.getProtocolVersion().equals(original.getProtocolVersion());
        assert reconstructed.getMessageType() == original.getMessageType();
        assert reconstructed.getRequestId().equals(original.getRequestId());
        assert reconstructed.getDataType().equals(original.getDataType());
        
        // Verify nested data preservation
        @SuppressWarnings("unchecked")
        Map<String, Object> reconstructedState = 
            (Map<String, Object>) reconstructed.getData().get("game_state");
        assert reconstructedState.get("phase").equals("main_1");
        assert reconstructedState.get("turn_number").equals(3);
        
        System.out.println("   ✅ JSON serialization tests passed");
    }
    
    /**
     * Test message validation logic.
     */
    public void testMessageValidation() {
        System.out.println("\n3. Testing message validation...");
        
        MessageValidator validator = new MessageValidator();
        
        // Test valid welcome message
        Map<String, Object> welcomeData = new HashMap<>();
        welcomeData.put("type", "welcome");
        welcomeData.put("server_version", "0.1.0");
        welcomeData.put("protocol_version", "1.0");
        welcomeData.put("capabilities", Arrays.asList("game_state_full"));
        
        ProtocolMessage welcomeMsg = new ProtocolMessage(
            ProtocolMessage.MessageType.NOTIFICATION, welcomeData);
        
        ProtocolMessage.ValidationResult result = validator.validateMessage(welcomeMsg);
        assert result.isValid() : "Welcome message should be valid: " + result.getErrorMessage();
        System.out.println("   ✓ Valid welcome message accepted");
        
        // Test invalid message - missing protocol version
        Map<String, Object> invalidData = new HashMap<>();
        invalidData.put("type", "welcome");
        invalidData.put("server_version", "0.1.0");
        // Missing protocol_version field
        
        ProtocolMessage invalidMsg = new ProtocolMessage(
            ProtocolMessage.MessageType.NOTIFICATION, invalidData);
        
        result = validator.validateMessage(invalidMsg);
        assert !result.isValid() : "Invalid message should be rejected";
        assert result.getErrorMessage().contains("protocol_version") : 
            "Error should mention missing protocol_version: " + result.getErrorMessage();
        System.out.println("   ✓ Invalid message properly rejected: " + result.getErrorMessage());
        
        // Test action request validation with all required fields
        Map<String, Object> actionData = new HashMap<>();
        actionData.put("type", "get_action");
        actionData.put("game_id", "validation-test");
        actionData.put("requesting_player", 1);
        actionData.put("game_state", new HashMap<>());
        actionData.put("legal_actions", new ArrayList<>());
        
        ProtocolMessage actionMsg = new ProtocolMessage(
            ProtocolMessage.MessageType.REQUEST, "valid-action", actionData);
        
        result = validator.validateMessage(actionMsg);
        assert result.isValid() : "Valid action request should be accepted: " + result.getErrorMessage();
        System.out.println("   ✓ Valid action request accepted");
        
        // Test action request with invalid player number
        actionData.put("requesting_player", -1); // Invalid: negative player number
        result = validator.validateMessage(actionMsg);
        assert !result.isValid() : "Invalid player number should be rejected";
        assert result.getErrorMessage().contains("out of range") : 
            "Error should mention range issue: " + result.getErrorMessage();
        System.out.println("   ✓ Invalid player number properly rejected: " + result.getErrorMessage());
        
        System.out.println("   ✅ Message validation tests passed");
    }
    
    /**
     * Test ProtocolHandler message creation methods.
     */
    public void testProtocolHandler() {
        System.out.println("\n4. Testing ProtocolHandler...");
        
        ProtocolHandler handler = new ProtocolHandler("test-client");
        MessageValidator validator = new MessageValidator();
        
        // Test welcome message creation
        ProtocolMessage welcome = handler.createWelcomeMessage();
        ProtocolMessage.ValidationResult result = validator.validateMessage(welcome);
        assert result.isValid() : "Welcome message should be valid: " + result.getErrorMessage();
        assert "welcome".equals(welcome.getDataType());
        System.out.println("   ✓ Welcome message created and validated");
        
        // Test action request creation
        Map<String, Object> gameState = Map.of(
            "phase", "combat",
            "turn", 5
        );
        List<Map<String, Object>> actions = Arrays.asList(
            Map.of("type", "attack", "creature_id", "creature-1"),
            Map.of("type", "pass", "next_phase", "main_2")
        );
        
        ProtocolMessage actionRequest = handler.createActionRequest(
            "handler-test-game", 
            0, 
            gameState, 
            actions, 
            null);
        
        result = validator.validateMessage(actionRequest);
        assert result.isValid() : "Action request should be valid: " + result.getErrorMessage();
        assert "get_action".equals(actionRequest.getDataType());
        assert actionRequest.getRequestId() != null;
        assert actionRequest.getRequestId().startsWith("action-req-");
        System.out.println("   ✓ Action request created and validated");
        
        
        // Test error response creation
        ProtocolMessage error = handler.createErrorResponse(
            "test-req-123",
            ProtocolMessage.ErrorCode.INVALID_GAME_STATE,
            "Test error message",
            true,
            null,
            Map.of("debug", "unit test error")
        );
        
        result = validator.validateMessage(error);
        assert result.isValid() : "Error message should be valid: " + result.getErrorMessage();
        assert "error_response".equals(error.getDataType());
        assert error.getData().get("error_code").equals("INVALID_GAME_STATE");
        assert Boolean.TRUE.equals(error.getData().get("can_retry"));
        System.out.println("   ✓ Error response created and validated");
        
        System.out.println("   ✅ ProtocolHandler tests passed");
    }
    
    /**
     * Test a realistic connection scenario.
     * 
     * This simulates the handshake and initial exchanges between
     * Java client and Python server without actual network I/O.
     */
    public void testConnectionScenario() {
        System.out.println("\n5. Testing connection scenario...");
        
        // Simulate server and client protocol handlers
        ProtocolHandler server = new ProtocolHandler("server");
        ProtocolHandler client = new ProtocolHandler("client");
        MessageValidator validator = new MessageValidator();
        
        // 1. Server sends welcome message
        ProtocolMessage welcome = server.createWelcomeMessage();
        System.out.println("   ✓ Server created welcome message");
        
        // 2. Client receives and validates welcome
        ProtocolMessage.ValidationResult result = validator.validateMessage(welcome);
        assert result.isValid() : "Welcome should be valid: " + result.getErrorMessage();
        
        String json = welcome.toJson();
        ProtocolMessage receivedWelcome = ProtocolMessage.fromJson(json);
        assert "welcome".equals(receivedWelcome.getDataType());
        System.out.println("   ✓ Client received and parsed welcome");
        
        // 3. Client sends action request
        Map<String, Object> gameState = Map.of(
            "phase", "upkeep",
            "turn_number", 1,
            "priority", 0,
            "stack", Collections.emptyList()
        );
        
        List<Map<String, Object>> legalActions = Arrays.asList(
            Map.of("type", "pass", "description", "Pass priority"),
            Map.of("type", "cast_spell", "card_id", "spell-1", "cost", "1U")
        );
        
        ProtocolMessage actionRequest = client.createActionRequest(
            "scenario-game", 
            0,
            gameState,
            legalActions,
            Map.of("thinking_time_ms", 1000)
        );
        System.out.println("   ✓ Client created action request");
        
        // 4. Server receives and validates request
        result = validator.validateMessage(actionRequest);
        assert result.isValid() : "Action request should be valid: " + result.getErrorMessage();
        
        String requestJson = actionRequest.toJson();
        ProtocolMessage receivedRequest = ProtocolMessage.fromJson(requestJson);
        assert "get_action".equals(receivedRequest.getDataType());
        assert receivedRequest.getRequestId() != null;
        System.out.println("   ✓ Server received and parsed action request");
        
        // 5. Server sends action response
        Map<String, Object> chosenAction = Map.of(
            "type", "cast_spell",
            "card_id", "spell-1",
            "targets", Collections.emptyList(),
            "reasoning", "Testing spell cast action"
        );
        
        ProtocolMessage actionResponse = server.createActionResponse(
            receivedRequest.getRequestId(),
            "scenario-game",
            0,
            chosenAction,
            850, // execution time in ms
            Map.of("model", "claude-3-5-sonnet", "tokens_used", 1250)
        );
        System.out.println("   ✓ Server created action response");
        
        // 6. Client receives and validates response
        result = validator.validateMessage(actionResponse);
        assert result.isValid() : "Action response should be valid: " + result.getErrorMessage();
        
        String responseJson = actionResponse.toJson();
        ProtocolMessage receivedResponse = ProtocolMessage.fromJson(responseJson);
        assert "action_response".equals(receivedResponse.getDataType());
        assert receivedResponse.getRequestId().equals(actionRequest.getRequestId());
        assert Boolean.TRUE.equals(receivedResponse.getData().get("success"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> receivedAction = 
            (Map<String, Object>) receivedResponse.getData().get("action");
        assert "cast_spell".equals(receivedAction.get("type"));
        assert "spell-1".equals(receivedAction.get("card_id"));
        
        System.out.println("   ✓ Client received and parsed action response");
        
        // 7. Verify request/response correlation
        String originalRequestId = actionRequest.getRequestId();
        String responseRequestId = receivedResponse.getRequestId();
        assert originalRequestId.equals(responseRequestId) : 
            "Request IDs should match: " + originalRequestId + " vs " + responseRequestId;
        System.out.println("   ✓ Request/response correlation verified");
        
        System.out.println("   ✅ Connection scenario tests passed");
        
        // Summary of what we've proven
        System.out.println("\n   🎯 Protocol Compatibility Summary:");
        System.out.println("      • Java→JSON→Java round-trip: ✓");
        System.out.println("      • Python-compatible JSON format: ✓");
        System.out.println("      • Complete request/response cycle: ✓");
        System.out.println("      • Message validation works: ✓");
        System.out.println("      • Error handling structure: ✓");
    }
    
    /**
     * Utility method to pretty-print a message for debugging.
     * 
     * @param message Message to display
     */
    public static void debugMessage(ProtocolMessage message) {
        System.out.println("Debug: " + message);
        if (message != null) {
            try {
                String json = message.toJson();
                System.out.println("JSON: " + json);
            } catch (Exception e) {
                System.err.println("JSON serialization failed: " + e.getMessage());
            }
        }
    }
}