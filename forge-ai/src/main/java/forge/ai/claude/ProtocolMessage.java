package forge.ai.claude;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Base class for all Claude Plays MTG protocol messages.
 * 
 * This is the Java equivalent of the Python ProtocolMessage class,
 * providing identical field structure and JSON serialization for
 * cross-language protocol compatibility.
 * 
 * The protocol uses JSON-over-TCP with 4-byte length prefixes.
 * All messages share this base structure with message-specific data.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProtocolMessage {
    
    // Protocol constants matching Python implementation
    public static final String PROTOCOL_VERSION = "1.0";
    public static final int DEFAULT_PORT = 8889;
    public static final int DEFAULT_TIMEOUT_NETWORK_SECONDS = 5;
    public static final int DEFAULT_TIMEOUT_CLAUDE_SECONDS = 30;
    public static final int MAX_MESSAGE_SIZE = 100 * 1024; // 100KB maximum message size
    
    // Message types (exact match with Python Literal types)
    public enum MessageType {
        @JsonProperty("request") REQUEST("request"),
        @JsonProperty("response") RESPONSE("response"), 
        @JsonProperty("notification") NOTIFICATION("notification"),
        @JsonProperty("error") ERROR("error");
        
        private final String value;
        
        MessageType(String value) {
            this.value = value;
        }
        
        @Override
        public String toString() {
            return value;
        }
    }
    
    // Request data types matching Python implementation
    public enum RequestDataType {
        @JsonProperty("initialize_game") INITIALIZE_GAME("initialize_game"),
        @JsonProperty("get_action") GET_ACTION("get_action"),
        @JsonProperty("session_end") SESSION_END("session_end");
        
        private final String value;
        
        RequestDataType(String value) {
            this.value = value;
        }
        
        @Override
        public String toString() {
            return value;
        }
    }
    
    // Response data types matching Python implementation  
    public enum ResponseDataType {
        @JsonProperty("game_ready") GAME_READY("game_ready"),
        @JsonProperty("action_response") ACTION_RESPONSE("action_response"),
        @JsonProperty("session_ended") SESSION_ENDED("session_ended");
        
        private final String value;
        
        ResponseDataType(String value) {
            this.value = value;
        }
        
        @Override
        public String toString() {
            return value;
        }
    }
    
    // Notification data types matching Python implementation
    public enum NotificationDataType {
        @JsonProperty("welcome") WELCOME("welcome"),
        @JsonProperty("state_update") STATE_UPDATE("state_update");
        
        private final String value;
        
        NotificationDataType(String value) {
            this.value = value;
        }
        
        @Override
        public String toString() {
            return value;
        }
    }
    
    // Error codes matching Python ErrorCode Literal
    public enum ErrorCode {
        @JsonProperty("CLAUDE_API_TIMEOUT") CLAUDE_API_TIMEOUT("CLAUDE_API_TIMEOUT"),
        @JsonProperty("CLAUDE_API_OVERLOADED") CLAUDE_API_OVERLOADED("CLAUDE_API_OVERLOADED"),
        @JsonProperty("INVALID_GAME_STATE") INVALID_GAME_STATE("INVALID_GAME_STATE"),
        @JsonProperty("ILLEGAL_ACTION") ILLEGAL_ACTION("ILLEGAL_ACTION"),
        @JsonProperty("CONNECTION_LOST") CONNECTION_LOST("CONNECTION_LOST"),
        @JsonProperty("PROTOCOL_VERSION_MISMATCH") PROTOCOL_VERSION_MISMATCH("PROTOCOL_VERSION_MISMATCH"),
        @JsonProperty("JSON_PARSE_ERROR") JSON_PARSE_ERROR("JSON_PARSE_ERROR"),
        @JsonProperty("MESSAGE_TOO_LARGE") MESSAGE_TOO_LARGE("MESSAGE_TOO_LARGE"),
        @JsonProperty("INTERNAL_ERROR") INTERNAL_ERROR("INTERNAL_ERROR");
        
        private final String value;
        
        ErrorCode(String value) {
            this.value = value;
        }
        
        @Override
        public String toString() {
            return value;
        }
    }
    
    // Jackson ObjectMapper for JSON serialization (thread-safe, singleton)
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    // Message fields matching Python implementation exactly
    @JsonProperty("protocol_version")
    private String protocolVersion = PROTOCOL_VERSION;
    
    @JsonProperty("message_type") 
    private MessageType messageType = MessageType.NOTIFICATION;
    
    @JsonProperty("request_id")
    private String requestId = null;
    
    @JsonProperty("timestamp")
    private String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    
    @JsonProperty("data")
    private Map<String, Object> data = new HashMap<>();
    
    /**
     * Default constructor for Jackson deserialization.
     */
    public ProtocolMessage() {
        // Jackson requires default constructor
    }
    
    /**
     * Constructor for creating new protocol messages.
     * 
     * @param messageType Type of message (request, response, notification, error)
     * @param data Message-specific payload content
     */
    public ProtocolMessage(MessageType messageType, Map<String, Object> data) {
        this.messageType = messageType;
        this.data = new HashMap<>(data);
        // timestamp is set by field initializer
        // protocolVersion is set by field initializer  
        // requestId remains null for notifications
    }
    
    /**
     * Constructor for creating request/response messages with correlation ID.
     * 
     * @param messageType Type of message 
     * @param requestId Correlation ID for request/response pairing
     * @param data Message-specific payload content
     */
    public ProtocolMessage(MessageType messageType, String requestId, Map<String, Object> data) {
        this(messageType, data);
        this.requestId = requestId;
    }
    
    // Getters and setters matching Python field names
    
    public String getProtocolVersion() {
        return protocolVersion;
    }
    
    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion = protocolVersion;
    }
    
    public MessageType getMessageType() {
        return messageType;
    }
    
    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
    public Map<String, Object> getData() {
        return data;
    }
    
    public void setData(Map<String, Object> data) {
        this.data = data;
    }
    
    /**
     * Convert message to JSON string for transmission.
     * 
     * This matches the Python to_json() method functionality,
     * producing compact JSON (no indentation) for network efficiency.
     * 
     * @return JSON string representation
     * @throws IllegalArgumentException if message cannot be serialized
     */
    public String toJson() throws IllegalArgumentException {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize message to JSON: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parse JSON string into ProtocolMessage.
     * 
     * This matches the Python from_json() class method functionality,
     * providing complete validation of required fields.
     * 
     * @param jsonStr JSON string to parse
     * @return Parsed ProtocolMessage instance
     * @throws IllegalArgumentException if JSON is malformed or missing required fields
     */
    public static ProtocolMessage fromJson(String jsonStr) throws IllegalArgumentException {
        try {
            // First parse as generic JsonNode for validation
            JsonNode jsonNode = OBJECT_MAPPER.readTree(jsonStr);
            
            // Validate required fields (matching Python implementation)
            String[] requiredFields = {"protocol_version", "message_type", "timestamp", "data"};
            for (String field : requiredFields) {
                if (!jsonNode.has(field)) {
                    throw new IllegalArgumentException("Missing required field: " + field);
                }
            }
            
            // Parse into ProtocolMessage if validation passes
            return OBJECT_MAPPER.treeToValue(jsonNode, ProtocolMessage.class);
            
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON parse error: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw e; // Re-throw validation errors
        } catch (Exception e) {
            throw new IllegalArgumentException("Message structure error: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate message structure and content.
     * 
     * This matches the Python validate() method functionality,
     * performing comprehensive validation including protocol compatibility.
     * 
     * @return Validation result with success flag and optional error message
     */
    public ValidationResult validate() {
        // Check protocol version compatibility
        if (!isVersionCompatible(this.protocolVersion)) {
            return new ValidationResult(false, 
                "Incompatible protocol version: " + this.protocolVersion);
        }
        
        // Check message type validity (enum ensures valid values)
        if (this.messageType == null) {
            return new ValidationResult(false, "Invalid message type: null");
        }
        
        // Check data structure
        if (this.data == null) {
            return new ValidationResult(false, "Data cannot be null");
        }
        
        // Check for required data type field
        if (!this.data.containsKey("type")) {
            return new ValidationResult(false, "Missing 'type' field in data");
        }
        
        // All basic validation passed
        return new ValidationResult(true, null);
    }
    
    /**
     * Check if protocol version is compatible.
     * 
     * Matches Python _is_version_compatible() method.
     * Currently requires exact match; future versions could implement
     * semantic versioning logic.
     * 
     * @param version Version string to check (e.g., "1.0")
     * @return True if compatible, false otherwise
     */
    private boolean isVersionCompatible(String version) {
        // For now, exact match required (matching Python implementation)
        return PROTOCOL_VERSION.equals(version);
    }
    
    /**
     * Validation result tuple equivalent for Java.
     * Matches Python tuple[bool, Optional[str]] return type.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        public ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
    
    /**
     * Helper method to get data type as string.
     * 
     * @return The "type" field from data, or null if not present
     */
    public String getDataType() {
        Object type = data.get("type");
        return type != null ? type.toString() : null;
    }
    
    /**
     * Helper method to check if this is a specific data type.
     * 
     * @param expectedType Expected type string
     * @return True if data contains matching type field
     */
    public boolean hasDataType(String expectedType) {
        return Objects.equals(expectedType, getDataType());
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProtocolMessage that = (ProtocolMessage) o;
        return Objects.equals(protocolVersion, that.protocolVersion) &&
               messageType == that.messageType &&
               Objects.equals(requestId, that.requestId) &&
               Objects.equals(timestamp, that.timestamp) &&
               Objects.equals(data, that.data);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(protocolVersion, messageType, requestId, timestamp, data);
    }
    
    @Override
    public String toString() {
        return "ProtocolMessage{" +
               "protocolVersion='" + protocolVersion + '\'' +
               ", messageType=" + messageType +
               ", requestId='" + requestId + '\'' +
               ", timestamp='" + timestamp + '\'' +
               ", dataType='" + getDataType() + '\'' +
               '}';
    }
}