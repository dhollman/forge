package forge.ai.claude;

import java.util.List;
import java.util.Map;

/**
 * Validates protocol messages for correctness and security.
 * 
 * This is the Java equivalent of the Python MessageValidator class,
 * providing comprehensive validation including:
 * - JSON structure validation
 * - Protocol version compatibility
 * - Message type consistency  
 * - Data field requirements
 * - Value range checking
 * - Security considerations (size limits, etc.)
 * 
 * Used by both client and server to ensure protocol compliance.
 */
public class MessageValidator {
    
    /**
     * Comprehensive message validation.
     * 
     * This matches the Python validate_message() method,
     * performing complete validation with message-type-specific checks.
     * 
     * @param message Message to validate
     * @return Validation result with success flag and optional error message
     */
    public ProtocolMessage.ValidationResult validateMessage(ProtocolMessage message) {
        if (message == null) {
            return new ProtocolMessage.ValidationResult(false, "Message cannot be null");
        }
        
        // Basic structure validation (calls message.validate())
        ProtocolMessage.ValidationResult basicResult = message.validate();
        if (!basicResult.isValid()) {
            return basicResult;
        }
        
        // Message type specific validation
        ProtocolMessage.MessageType messageType = message.getMessageType();
        String dataType = message.getDataType();
        
        switch (messageType) {
            case REQUEST:
                return validateRequest(message);
            case RESPONSE:
                return validateResponse(message);
            case NOTIFICATION:
                return validateNotification(message);
            case ERROR:
                return validateError(message);
            default:
                return new ProtocolMessage.ValidationResult(false, 
                    "Unknown message type: " + messageType);
        }
    }
    
    /**
     * Validate request message.
     * 
     * Routes to specific request type validators based on data type.
     * 
     * @param message Request message to validate
     * @return Validation result
     */
    private ProtocolMessage.ValidationResult validateRequest(ProtocolMessage message) {
        String dataType = message.getDataType();
        
        if ("get_action".equals(dataType)) {
            return validateActionRequest(message);
        } else if ("initialize_game".equals(dataType)) {
            return validateInitRequest(message);
        } else if ("session_end".equals(dataType)) {
            return validateSessionEndRequest(message);
        } else {
            return new ProtocolMessage.ValidationResult(false, 
                "Unknown request type: " + dataType);
        }
    }
    
    /**
     * Validate action request specific fields.
     * 
     * This matches the Python _validate_action_request() method,
     * checking all required fields and their types/ranges.
     * 
     * @param message Action request message
     * @return Validation result
     */
    private ProtocolMessage.ValidationResult validateActionRequest(ProtocolMessage message) {
        Map<String, Object> data = message.getData();
        
        // Required fields (matching Python implementation)
        String[] requiredFields = {
            "game_id", "requesting_player", "game_state", "legal_actions"
        };
        
        for (String field : requiredFields) {
            if (!data.containsKey(field)) {
                return new ProtocolMessage.ValidationResult(false, 
                    "Missing required field: " + field);
            }
        }
        
        // Type validation
        Object requestingPlayerObj = data.get("requesting_player");
        if (!(requestingPlayerObj instanceof Integer)) {
            return new ProtocolMessage.ValidationResult(false, 
                "requesting_player must be an integer");
        }
        
        Object gameStateObj = data.get("game_state");
        if (!(gameStateObj instanceof Map)) {
            return new ProtocolMessage.ValidationResult(false, 
                "game_state must be a dictionary");
        }
        
        Object legalActionsObj = data.get("legal_actions");
        if (!(legalActionsObj instanceof List)) {
            return new ProtocolMessage.ValidationResult(false, 
                "legal_actions must be a list");
        }
        
        // Range validation
        int requestingPlayer = (Integer) requestingPlayerObj;
        if (requestingPlayer < 0 || requestingPlayer > 7) {
            return new ProtocolMessage.ValidationResult(false, 
                "requesting_player out of range: " + requestingPlayer);
        }
        
        // Content validation (warning but not error)
        @SuppressWarnings("unchecked")
        List<Object> legalActions = (List<Object>) legalActionsObj;
        if (legalActions.isEmpty()) {
            System.out.println("[MessageValidator] Warning: Action request with no legal actions");
            // Not invalid, but unusual (matching Python behavior)
        }
        
        return new ProtocolMessage.ValidationResult(true, null);
    }
    
    /**
     * Validate response message.
     * 
     * Routes to specific response type validators based on data type.
     * 
     * @param message Response message to validate
     * @return Validation result
     */
    private ProtocolMessage.ValidationResult validateResponse(ProtocolMessage message) {
        String dataType = message.getDataType();
        
        if ("action_response".equals(dataType)) {
            return validateActionResponse(message);
        } else if ("game_ready".equals(dataType)) {
            return validateGameReadyResponse(message);
        } else if ("mana_payment_response".equals(dataType)) {
            return validateManaPaymentResponse(message);
        } else if ("target_response".equals(dataType)) {
            return validateTargetResponse(message);
        } else {
            return new ProtocolMessage.ValidationResult(false, 
                "Unknown response type: " + dataType);
        }
    }
    
    /**
     * Validate action response specific fields.
     * 
     * This matches the Python _validate_action_response() method.
     * 
     * @param message Action response message
     * @return Validation result
     */
    private ProtocolMessage.ValidationResult validateActionResponse(ProtocolMessage message) {
        Map<String, Object> data = message.getData();
        
        // Required fields (matching Python implementation)
        String[] requiredFields = {"success", "action"};
        
        for (String field : requiredFields) {
            if (!data.containsKey(field)) {
                return new ProtocolMessage.ValidationResult(false, 
                    "Missing required field: " + field);
            }
        }
        
        Object successObj = data.get("success");
        if (!(successObj instanceof Boolean)) {
            return new ProtocolMessage.ValidationResult(false, 
                "success must be a boolean");
        }
        
        boolean success = (Boolean) successObj;
        Object actionObj = data.get("action");
        if (success && !(actionObj instanceof Map)) {
            return new ProtocolMessage.ValidationResult(false, 
                "action must be a dictionary when success=true");
        }
        
        return new ProtocolMessage.ValidationResult(true, null);
    }
    
    /**
     * Validate notification message.
     * 
     * Routes to specific notification type validators based on data type.
     * 
     * @param message Notification message to validate
     * @return Validation result
     */
    private ProtocolMessage.ValidationResult validateNotification(ProtocolMessage message) {
        String dataType = message.getDataType();
        
        if ("welcome".equals(dataType)) {
            return validateWelcome(message);
        } else if ("state_update".equals(dataType)) {
            return validateStateUpdate(message);
        } else {
            return new ProtocolMessage.ValidationResult(false, 
                "Unknown notification type: " + dataType);
        }
    }
    
    /**
     * Validate welcome message.
     * 
     * This matches the Python _validate_welcome() method,
     * ensuring server information and protocol compatibility.
     * 
     * @param message Welcome message
     * @return Validation result
     */
    private ProtocolMessage.ValidationResult validateWelcome(ProtocolMessage message) {
        Map<String, Object> data = message.getData();
        
        // Required fields (matching Python implementation)
        String[] requiredFields = {"type", "server_version", "protocol_version"};
        
        for (String field : requiredFields) {
            if (!data.containsKey(field)) {
                return new ProtocolMessage.ValidationResult(false, 
                    "Welcome message missing field: " + field);
            }
        }
        
        // Validate server version format
        Object serverVersionObj = data.get("server_version");
        if (!(serverVersionObj instanceof String)) {
            return new ProtocolMessage.ValidationResult(false, 
                "server_version must be string");
        }
        
        // Validate protocol version compatibility
        Object protocolVersionObj = data.get("protocol_version");
        if (!(protocolVersionObj instanceof String)) {
            return new ProtocolMessage.ValidationResult(false, 
                "protocol_version must be string");
        }
        
        String protocolVersion = (String) protocolVersionObj;
        if (!protocolVersion.equals(ProtocolMessage.PROTOCOL_VERSION)) {
            return new ProtocolMessage.ValidationResult(false, 
                "Incompatible protocol version: " + protocolVersion + 
                " vs " + ProtocolMessage.PROTOCOL_VERSION);
        }
        
        return new ProtocolMessage.ValidationResult(true, null);
    }
    
    
    /**
     * Validate initialize game request message.
     * 
     * This matches the Python _validate_init_request() method.
     * 
     * @param message Initialize game request message
     * @return Validation result
     */
    private ProtocolMessage.ValidationResult validateInitRequest(ProtocolMessage message) {
        Map<String, Object> data = message.getData();
        
        // Required fields (matching Python implementation)
        String[] requiredFields = {"game_id", "players"};
        
        for (String field : requiredFields) {
            if (!data.containsKey(field)) {
                return new ProtocolMessage.ValidationResult(false, 
                    "Missing required field: " + field);
            }
        }
        
        // Validate player data
        Object playersObj = data.get("players");
        if (!(playersObj instanceof List)) {
            return new ProtocolMessage.ValidationResult(false, 
                "players must be a list");
        }
        
        @SuppressWarnings("unchecked")
        List<Object> players = (List<Object>) playersObj;
        
        if (players.size() < 1 || players.size() > 8) {
            return new ProtocolMessage.ValidationResult(false, 
                "Invalid number of players: " + players.size());
        }
        
        for (int i = 0; i < players.size(); i++) {
            Object playerObj = players.get(i);
            if (!(playerObj instanceof Map)) {
                return new ProtocolMessage.ValidationResult(false, 
                    "Player " + i + " must be a dictionary");
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> player = (Map<String, Object>) playerObj;
            
            if (!player.containsKey("index")) {
                return new ProtocolMessage.ValidationResult(false, 
                    "Player " + i + " missing required field: index");
            }
            
            Object indexObj = player.get("index");
            if (!(indexObj instanceof Integer)) {
                return new ProtocolMessage.ValidationResult(false, 
                    "Player " + i + " index must be an integer");
            }
        }
        
        return new ProtocolMessage.ValidationResult(true, null);
    }
    
    /**
     * Validate error message.
     * 
     * This matches the Python _validate_error() method.
     * 
     * @param message Error message
     * @return Validation result
     */
    private ProtocolMessage.ValidationResult validateError(ProtocolMessage message) {
        Map<String, Object> data = message.getData();
        
        // Required fields (matching Python implementation)
        String[] requiredFields = {"error_code", "error_message"};
        
        for (String field : requiredFields) {
            if (!data.containsKey(field)) {
                return new ProtocolMessage.ValidationResult(false, 
                    "Missing required field: " + field);
            }
        }
        
        return new ProtocolMessage.ValidationResult(true, null);
    }
    
    /**
     * Validate session end request message.
     * 
     * @param message Session end request message
     * @return Validation result
     */
    private ProtocolMessage.ValidationResult validateSessionEndRequest(ProtocolMessage message) {
        Map<String, Object> data = message.getData();
        
        // Session end requests need minimal validation - just ensure game_id exists
        if (!data.containsKey("game_id")) {
            return new ProtocolMessage.ValidationResult(false, 
                "Missing required field: game_id");
        }
        
        return new ProtocolMessage.ValidationResult(true, null);
    }
    
    /**
     * Validate game ready response message.
     * 
     * @param message Game ready response message
     * @return Validation result
     */
    private ProtocolMessage.ValidationResult validateGameReadyResponse(ProtocolMessage message) {
        Map<String, Object> data = message.getData();
        
        // Required fields for game ready responses
        String[] requiredFields = {"success", "game_id"};
        
        for (String field : requiredFields) {
            if (!data.containsKey(field)) {
                return new ProtocolMessage.ValidationResult(false, 
                    "Missing required field: " + field);
            }
        }
        
        Object successObj = data.get("success");
        if (!(successObj instanceof Boolean)) {
            return new ProtocolMessage.ValidationResult(false, 
                "success must be a boolean");
        }
        
        return new ProtocolMessage.ValidationResult(true, null);
    }
    
    /**
     * Validate state update notification message.
     * 
     * @param message State update message
     * @return Validation result
     */
    private ProtocolMessage.ValidationResult validateStateUpdate(ProtocolMessage message) {
        Map<String, Object> data = message.getData();
        
        // State updates should have a game_id
        if (!data.containsKey("game_id")) {
            return new ProtocolMessage.ValidationResult(false, 
                "Missing required field: game_id");
        }
        
        return new ProtocolMessage.ValidationResult(true, null);
    }
    
    /**
     * Validate mana payment response message.
     * 
     * @param message Mana payment response message
     * @return Validation result
     */
    private ProtocolMessage.ValidationResult validateManaPaymentResponse(ProtocolMessage message) {
        Map<String, Object> data = message.getData();
        
        // Required fields for mana payment responses
        String[] requiredFields = {"success", "game_id", "tap_sources"};
        
        for (String field : requiredFields) {
            if (!data.containsKey(field)) {
                return new ProtocolMessage.ValidationResult(false, 
                    "Missing required field: " + field);
            }
        }
        
        Object successObj = data.get("success");
        if (!(successObj instanceof Boolean)) {
            return new ProtocolMessage.ValidationResult(false, 
                "success must be a boolean");
        }
        
        Object tapSourcesObj = data.get("tap_sources");
        if (!(tapSourcesObj instanceof List)) {
            return new ProtocolMessage.ValidationResult(false, 
                "tap_sources must be a list");
        }
        
        return new ProtocolMessage.ValidationResult(true, null);
    }
    
    /**
     * Validate target response message.
     * 
     * @param message Target response message
     * @return Validation result
     */
    private ProtocolMessage.ValidationResult validateTargetResponse(ProtocolMessage message) {
        Map<String, Object> data = message.getData();
        
        // Required fields for target responses
        String[] requiredFields = {"success", "game_id", "targets"};
        
        for (String field : requiredFields) {
            if (!data.containsKey(field)) {
                return new ProtocolMessage.ValidationResult(false, 
                    "Missing required field: " + field);
            }
        }
        
        Object successObj = data.get("success");
        if (!(successObj instanceof Boolean)) {
            return new ProtocolMessage.ValidationResult(false, 
                "success must be a boolean");
        }
        
        Object targetsObj = data.get("targets");
        if (!(targetsObj instanceof List)) {
            return new ProtocolMessage.ValidationResult(false, 
                "targets must be a list");
        }
        
        return new ProtocolMessage.ValidationResult(true, null);
    }
    
    /**
     * Validate message size limits for security.
     * 
     * @param jsonStr JSON representation of message
     * @return Validation result
     */
    public ProtocolMessage.ValidationResult validateMessageSize(String jsonStr) {
        if (jsonStr == null) {
            return new ProtocolMessage.ValidationResult(false, "JSON string cannot be null");
        }
        
        int jsonSize = jsonStr.getBytes().length;
        if (jsonSize > ProtocolMessage.MAX_MESSAGE_SIZE) {
            return new ProtocolMessage.ValidationResult(false, 
                "Message size " + jsonSize + " exceeds maximum " + ProtocolMessage.MAX_MESSAGE_SIZE);
        }
        
        return new ProtocolMessage.ValidationResult(true, null);
    }
}