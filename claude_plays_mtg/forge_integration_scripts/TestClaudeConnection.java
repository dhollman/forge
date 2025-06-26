import forge.ai.claude.ClaudeProtocolClient;
import forge.ai.claude.ProtocolMessage;

public class TestClaudeConnection {
    public static void main(String[] args) {
        System.out.println("Testing Claude Protocol Connection...");
        
        ClaudeProtocolClient client = new ClaudeProtocolClient("localhost", 8889);
        
        // Set up callbacks
        client.setConnectionCallback(new ClaudeProtocolClient.ConnectionCallback() {
            @Override
            public void onConnected(ProtocolMessage welcomeMessage) {
                System.out.println("✓ Connected successfully!");
                System.out.println("Welcome message: " + welcomeMessage.toString());
                System.out.println("Server version: " + welcomeMessage.getData().get("server_version"));
            }
            
            @Override
            public void onDisconnected(String reason, boolean canRetry) {
                System.err.println("✗ Disconnected: " + reason);
            }
            
            @Override
            public void onConnectionUnhealthy(double timeSinceLastHeartbeat) {
                System.out.println("Connection unhealthy: " + timeSinceLastHeartbeat + "s since heartbeat");
            }
        });
        
        client.setMessageCallback(new ClaudeProtocolClient.MessageCallback() {
            @Override
            public void onMessageReceived(ProtocolMessage message) {
                System.out.println("Received message: " + message.getMessageType());
            }
            
            @Override
            public void onMessageError(String error, Throwable cause) {
                System.err.println("Message error: " + error);
                if (cause != null) {
                    cause.printStackTrace();
                }
            }
        });
        
        try {
            // Connect to server
            System.out.println("Connecting to server...");
            client.connect();
            
            // Wait a bit to see if we get the welcome message
            Thread.sleep(2000);
            
            // Send a test heartbeat
            System.out.println("Sending heartbeat...");
            ProtocolMessage heartbeat = client.getProtocolHandler().createHeartbeat();
            client.sendMessage(heartbeat);
            
            // Wait a bit more
            Thread.sleep(2000);
            
            // Disconnect
            System.out.println("Disconnecting...");
            client.disconnect();
            
            System.out.println("Test completed successfully!");
            
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            client.shutdown();
        }
    }
}