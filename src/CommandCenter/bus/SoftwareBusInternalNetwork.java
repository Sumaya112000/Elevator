package CommandCenter.bus;

import CommandCenter.Messages.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class SoftwareBusInternalNetwork {

    // For server mode: all currently connected client sockets
    private final Set<Socket> clientSockets;

    private ServerSocket serverSocket;
    private Socket busSocket;
    private PrintWriter out;
    private MessageListener listener;

    private boolean isServer;

    private int port = 9999;

    public SoftwareBusInternalNetwork(boolean isServer) {
        this.isServer = isServer;
        this.clientSockets = new HashSet<>();

        if (isServer) {
            // Server mode: create a listening socket and start accept thread
            try {
                serverSocket = new ServerSocket(port);
                acceptThread();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            // Client mode: connect to the server
            try {
                busSocket = new Socket("localhost", port);
                out = new PrintWriter(busSocket.getOutputStream(), true);
                // Start listening for messages from server
                readerThread(busSocket);
            } catch (IOException e) {
                System.err.println("Please launch the Command Center first.");
                System.exit(1);
            }
        }
    }

    /**
     * Accepts incoming client connections in a background thread (server mode only).
     * For each accepted socket, a reader thread is created to handle incoming messages.
     */
    private void acceptThread() {
        Thread acceptThread = new Thread(() -> {
            try {
                while (true) {
                    Socket newSocket = serverSocket.accept();
                    clientSockets.add(newSocket);
                    System.out.println("Client connected: " + newSocket);
                    // Start listening for messages from this client
                    readerThread(newSocket);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        acceptThread.start();
    }

    /**
     * Starts a thread that listens for messages coming from a given socket.
     * Server mode:
     * - Rebroadcasts received messages to all other clients.
     * Client mode:
     * - Checks message topic/subtopic against subscriptions and stores it in the local queue if relevant.
     */
    private void readerThread(Socket socket) {
        Thread readerThread = new Thread(() -> {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    Message message = Message.parseStringToMsg(line);
                    System.out.println("received message: " + message);

                    if (isServer) {
                        synchronized (clientSockets) {
                            for (Socket client : clientSockets) {
                                if (client != socket) {
                                    PrintWriter temp = new PrintWriter(client.getOutputStream(), true);
                                    temp.println(line);
                                }
                            }
                        }
                    }

                    if (listener != null) {
                        listener.onMessage(message);
                    }
                }
            } catch (IOException e) {
                System.err.println("Connection error: " + e.getMessage());
                cleanupSocket(socket);
            }
        });
        readerThread.start();
    }

    /**
     * Removes and closes a disconnected socket in server mode.
     */
    private void cleanupSocket(Socket socket) {
        try {
            synchronized (clientSockets) {
                clientSockets.remove(socket);
            }
            socket.close();
            System.out.println("Closed socket: " + socket);
        } catch (IOException e) {
            System.err.println("Error closing socket: " + e.getMessage());
        }
    }

    /**
     * Sends a message into the network.
     * Server mode:
     * - Deliver message locally to server listener
     * - Broadcast message to all connected clients
     * Client mode:
     * - Send message upstream to server
     */
    public void broadcast(Message message) {
        if (isServer) {

            if (listener != null) {
                listener.onMessage(message);
            }

            synchronized (clientSockets) {
                for (Socket client : clientSockets) {
                    try {
                        PrintWriter temp = new PrintWriter(client.getOutputStream(), true);
                        temp.println(message.toString());
                    } catch (IOException e) {
                        System.err.println("Connection error: " + e.getMessage());
                        cleanupSocket(client);
                    }
                }
            }
        } else {
            out.println(message.toString());
        }
    }

    /**
     * Registers the callback used by SoftwareBus to receive incoming messages.
     */
    public void setMessageListener(MessageListener listener) {
        this.listener = listener;
    }

}