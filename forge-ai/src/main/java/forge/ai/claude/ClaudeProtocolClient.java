package forge.ai.claude;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * TCP socket client for Claude Plays MTG protocol communication.
 * 
 * This class handles the networking layer of the Java client,
 * providing robust TCP connection management with:
 * - Connection establishment and lifecycle management
 * - Message framing (4-byte length prefix + JSON content)
 * - Async message sending/receiving with callbacks
 * - Connection health monitoring and recovery
 * - Thread-safe operation for concurrent use
 * 
 * This integrates with the Python server running on port 8889,
 * enabling the Java Forge client to communicate with Claude
 * for decision-making assistance.
 */
public class ClaudeProtocolClient {
    
    // Connection configuration matching Python server defaults
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = ProtocolMessage.DEFAULT_PORT; // 8889
    private static final int CONNECT_TIMEOUT_MS = 5000; // 5 seconds
    private static final int SOCKET_TIMEOUT_MS = 30000; // 30 seconds for socket operations
    
    // Connection state
    private Socket socket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private final String host;
    private final int port;
    private boolean connected = false;
    private String connectionId;
    
    // Protocol handling
    private final ProtocolHandler protocolHandler;
    private final MessageValidator messageValidator;
    
    // Threading for async operations
    private ExecutorService executor;
    private Future<?> receiveTask;
    
    // Message callbacks
    private MessageCallback messageCallback;
    private ConnectionCallback connectionCallback;
    
    /**
     * Callback interface for receiving messages.
     */
    public interface MessageCallback {
        /**
         * Called when a complete message is received.
         * 
         * @param message Received protocol message
         */
        void onMessageReceived(ProtocolMessage message);
        
        /**
         * Called when message parsing fails.
         * 
         * @param error Error description
         * @param cause Exception that caused the error (may be null)
         */
        void onMessageError(String error, Throwable cause);
    }
    
    /**
     * Callback interface for connection events.
     */
    public interface ConnectionCallback {
        /**
         * Called when connection is established successfully.
         * 
         * @param welcomeMessage Server welcome message with capabilities
         */
        void onConnected(ProtocolMessage welcomeMessage);
        
        /**
         * Called when connection is lost or fails.
         * 
         * @param reason Reason for disconnection
         * @param canRetry Whether reconnection is possible
         */
        void onDisconnected(String reason, boolean canRetry);
        
    }
    
    /**
     * Create client with default connection settings.
     */
    public ClaudeProtocolClient() {
        this(DEFAULT_HOST, DEFAULT_PORT);
    }
    
    /**
     * Create client with custom host and port.
     * 
     * @param host Server hostname or IP address
     * @param port Server TCP port
     */
    public ClaudeProtocolClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.protocolHandler = new ProtocolHandler("client");
        this.messageValidator = new MessageValidator();
        
        // Create thread pool for async operations
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "claude-client-worker");
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * Set message callback for asynchronous message handling.
     * 
     * @param callback Callback for received messages
     */
    public void setMessageCallback(MessageCallback callback) {
        this.messageCallback = callback;
    }
    
    /**
     * Set connection callback for connection events.
     * 
     * @param callback Callback for connection events
     */
    public void setConnectionCallback(ConnectionCallback callback) {
        this.connectionCallback = callback;
    }
    
    /**
     * Connect to the Python server and perform handshake.
     * 
     * @throws IOException if connection fails
     * @throws IllegalStateException if already connected
     */
    public void connect() throws IOException, IllegalStateException {
        if (connected) {
            throw new IllegalStateException("Client is already connected");
        }
        
        System.out.println("[ClaudeProtocolClient] Connecting to " + host + ":" + port);
        
        try {
            // Create socket with timeout
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(SOCKET_TIMEOUT_MS);
            
            // Set up streams
            inputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            outputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            
            connected = true;
            
            // Start async message receiving
            startReceiving();
            
            
            System.out.println("[ClaudeProtocolClient] Connected successfully");
            
            // Wait for welcome message to confirm protocol compatibility
            // This is handled asynchronously by the receive loop
            
        } catch (IOException e) {
            // Clean up on failure
            cleanup();
            throw new IOException("Failed to connect to server: " + e.getMessage(), e);
        }
    }
    
    /**
     * Disconnect from the server and clean up resources.
     */
    public void disconnect() {
        if (!connected) {
            return;
        }
        
        System.out.println("[ClaudeProtocolClient] Disconnecting");
        
        // Stop background tasks
        if (receiveTask != null) {
            receiveTask.cancel(true);
        }
        
        // Clean up connection
        cleanup();
        
        // Notify callback
        if (connectionCallback != null) {
            connectionCallback.onDisconnected("Client disconnected", false);
        }
    }
    
    /**
     * Clean up socket and streams.
     */
    private void cleanup() {
        connected = false;
        
        try {
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("[ClaudeProtocolClient] Error during cleanup: " + e.getMessage());
        }
        
        inputStream = null;
        outputStream = null;
        socket = null;
    }
    
    /**
     * Start async message receiving loop.
     */
    private void startReceiving() {
        receiveTask = executor.submit(() -> {
            try {
                receiveLoop();
            } catch (Exception e) {
                System.err.println("[ClaudeProtocolClient] Receive loop error: " + e.getMessage());
                e.printStackTrace();
                
                // Notify connection callback
                if (connectionCallback != null) {
                    connectionCallback.onDisconnected("Receive loop failed: " + e.getMessage(), true);
                }
            }
        });
    }
    
    
    /**
     * Message receiving loop (runs in background thread).
     */
    private void receiveLoop() throws IOException {
        System.out.println("[ClaudeProtocolClient] Receive loop started");
        while (connected && !Thread.currentThread().isInterrupted()) {
            try {
                System.out.println("[ClaudeProtocolClient] Waiting for message...");
                ProtocolMessage message = receiveMessage();
                if (message == null) {
                    // Connection closed by server
                    System.out.println("[ClaudeProtocolClient] Server closed connection");
                    break;
                }
                
                System.out.println("[ClaudeProtocolClient] Processing received message");
                // Process the message
                processReceivedMessage(message);
                
            } catch (SocketTimeoutException e) {
                // Timeout is expected for non-blocking operation, continue
                continue;
                
            } catch (IOException e) {
                // Real I/O error
                System.err.println("[ClaudeProtocolClient] Receive error: " + e.getMessage());
                throw e;
            }
        }
        
        // Clean up when loop exits
        cleanup();
        
        if (connectionCallback != null) {
            connectionCallback.onDisconnected("Server closed connection", true);
        }
    }
    
    /**
     * Process a received message based on its type.
     * 
     * @param message Received message
     */
    private void processReceivedMessage(ProtocolMessage message) {
        System.out.println("[ClaudeProtocolClient] Received: " + message.toString());
        
        // Validate message first
        ProtocolMessage.ValidationResult validation = messageValidator.validateMessage(message);
        if (!validation.isValid()) {
            System.err.println("[ClaudeProtocolClient] Invalid message: " + validation.getErrorMessage());
            if (messageCallback != null) {
                messageCallback.onMessageError("Invalid message: " + validation.getErrorMessage(), null);
            }
            return;
        }
        
        // Handle special message types
        if (message.hasDataType("welcome")) {
            handleWelcomeMessage(message);
        }
        
        // Always forward to callback for application-level handling
        if (messageCallback != null) {
            messageCallback.onMessageReceived(message);
        }
    }
    
    /**
     * Handle welcome message from server.
     * 
     * @param message Welcome message
     */
    private void handleWelcomeMessage(ProtocolMessage message) {
        // Extract connection information
        Object connectionIdObj = message.getData().get("connection_id");
        if (connectionIdObj != null) {
            connectionId = connectionIdObj.toString();
            System.out.println("[ClaudeProtocolClient] Server connection ID: " + connectionId);
        }
        
        // Notify callback
        if (connectionCallback != null) {
            connectionCallback.onConnected(message);
        }
    }
    
    
    /**
     * Send protocol message to server.
     * 
     * This implements the 4-byte length prefix + JSON framing
     * that matches the Python server's receive_message() function.
     * 
     * @param message Message to send
     * @throws IOException if sending fails
     */
    public void sendMessage(ProtocolMessage message) throws IOException {
        if (!connected) {
            throw new IllegalStateException("Client is not connected");
        }
        
        try {
            // Serialize message to JSON
            String jsonStr = message.toJson();
            byte[] jsonBytes = jsonStr.getBytes("UTF-8");
            
            // Check size limit
            if (jsonBytes.length > ProtocolMessage.MAX_MESSAGE_SIZE) {
                throw new IOException("Message size " + jsonBytes.length + 
                                    " exceeds maximum " + ProtocolMessage.MAX_MESSAGE_SIZE);
            }
            
            // Send with 4-byte big-endian length prefix
            ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
            lengthBuffer.putInt(jsonBytes.length);
            byte[] lengthPrefix = lengthBuffer.array();
            
            // Write both parts atomically
            synchronized (outputStream) {
                outputStream.write(lengthPrefix);
                outputStream.write(jsonBytes);
                outputStream.flush();
            }
            
            System.out.println("[ClaudeProtocolClient] Sent: " + message.getMessageType() + 
                              " (size: " + jsonBytes.length + ")");
            
        } catch (IOException e) {
            System.err.println("[ClaudeProtocolClient] Failed to send message: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Receive protocol message from server.
     * 
     * This implements the 4-byte length prefix + JSON framing
     * that matches the Python server's send_message() function.
     * 
     * @return Received message, or null if connection closed
     * @throws IOException if receiving fails
     */
    private ProtocolMessage receiveMessage() throws IOException {
        if (!connected) {
            throw new IllegalStateException("Client is not connected");
        }
        
        try {
            System.out.println("[ClaudeProtocolClient] Reading length prefix...");
            // Read 4-byte length prefix (big-endian)
            byte[] lengthPrefix = new byte[4];
            int bytesRead = 0;
            while (bytesRead < 4) {
                int read = inputStream.read(lengthPrefix, bytesRead, 4 - bytesRead);
                if (read == -1) {
                    System.out.println("[ClaudeProtocolClient] Connection closed while reading length");
                    // Connection closed
                    return null;
                }
                bytesRead += read;
            }
            
            // Parse message length
            ByteBuffer lengthBuffer = ByteBuffer.wrap(lengthPrefix);
            int messageLength = lengthBuffer.getInt();
            
            // Check size limit
            if (messageLength > ProtocolMessage.MAX_MESSAGE_SIZE) {
                throw new IOException("Message size " + messageLength + 
                                    " exceeds maximum " + ProtocolMessage.MAX_MESSAGE_SIZE);
            }
            
            // Read message content
            byte[] jsonBytes = new byte[messageLength];
            bytesRead = 0;
            while (bytesRead < messageLength) {
                int read = inputStream.read(jsonBytes, bytesRead, messageLength - bytesRead);
                if (read == -1) {
                    // Connection closed mid-message
                    throw new IOException("Connection closed while reading message content");
                }
                bytesRead += read;
            }
            
            // Parse JSON content
            String jsonStr = new String(jsonBytes, "UTF-8");
            ProtocolMessage message = ProtocolMessage.fromJson(jsonStr);
            
            System.out.println("[ClaudeProtocolClient] Received: " + message.getMessageType());
            
            return message;
            
        } catch (IOException e) {
            System.err.println("[ClaudeProtocolClient] Failed to receive message: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Send message synchronously and wait for response with timeout.
     * 
     * @param request Request message to send
     * @param timeoutMs Timeout in milliseconds
     * @return Response message
     * @throws IOException if sending fails
     * @throws TimeoutException if no response within timeout
     */
    public ProtocolMessage sendRequest(ProtocolMessage request, int timeoutMs) 
            throws IOException, TimeoutException {
        
        if (request.getRequestId() == null) {
            // Assign request ID if not already set
            request.setRequestId("sync-req-" + UUID.randomUUID().toString().substring(0, 8));
        }
        
        String requestId = request.getRequestId();
        
        // Set up response listener
        CompletableFuture<ProtocolMessage> responseFuture = new CompletableFuture<>();
        MessageCallback originalCallback = messageCallback;
        
        messageCallback = new MessageCallback() {
            @Override
            public void onMessageReceived(ProtocolMessage message) {
                // Check if this is our response
                if (requestId.equals(message.getRequestId())) {
                    responseFuture.complete(message);
                    // Restore original callback
                    messageCallback = originalCallback;
                } else if (originalCallback != null) {
                    // Forward other messages to original callback
                    originalCallback.onMessageReceived(message);
                }
            }
            
            @Override
            public void onMessageError(String error, Throwable cause) {
                if (originalCallback != null) {
                    originalCallback.onMessageError(error, cause);
                }
            }
        };
        
        // Send request
        sendMessage(request);
        
        try {
            // Wait for response with timeout
            return responseFuture.get(timeoutMs, TimeUnit.MILLISECONDS);
            
        } catch (ExecutionException e) {
            throw new IOException("Request processing failed", e.getCause());
        } catch (InterruptedException e) {
            // Restore interrupted status
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted", e);
        } finally {
            // Always restore original callback
            messageCallback = originalCallback;
        }
    }
    
    // Status getters
    
    public boolean isConnected() {
        return connected;
    }
    
    public String getHost() {
        return host;
    }
    
    public int getPort() {
        return port;
    }
    
    public String getConnectionId() {
        return connectionId;
    }
    
    public ProtocolHandler getProtocolHandler() {
        return protocolHandler;
    }
    
    /**
     * Shutdown client and release all resources.
     */
    public void shutdown() {
        disconnect();
        
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}