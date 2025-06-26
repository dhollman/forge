import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

public class TestJsonParsing {
    public static void main(String[] args) {
        String welcomeJson = "{\"protocol_version\": \"1.0\", \"message_type\": \"notification\", \"request_id\": null, \"timestamp\": \"2025-06-24T20:12:17.737251Z\", \"data\": {\"type\": \"welcome\", \"server_version\": \"0.1.0\", \"protocol_version\": \"1.0\", \"supported_protocol_versions\": [\"1.0\"], \"capabilities\": [\"game_state_full\", \"action_reasoning\", \"deck_conversion\", \"error_recovery\"], \"max_concurrent_games\": 1, \"connection_id\": \"test-ed2a1892\"}}";
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(welcomeJson);
            System.out.println("✓ Successfully parsed JSON");
            System.out.println("Message type: " + node.get("message_type").asText());
            System.out.println("Data type: " + node.get("data").get("type").asText());
        } catch (Exception e) {
            System.err.println("✗ Failed to parse JSON: " + e.getMessage());
            e.printStackTrace();
        }
    }
}