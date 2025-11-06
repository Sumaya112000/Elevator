package bus;

import Message.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;


public class SoftwareBus {

    private record Subscription(int topic, int subtopic) {
    }

    // Local queue for received messages that match this processor's subscriptions
    private final LinkedList<Message> queue;

    // List of all subscriptions for this bus
    private final Set<Subscription> subscriptions;

    // For server mode: all currently connected client sockets
    private final Set<Socket> clientSockets;

    private ServerSocket serverSocket;
    private Socket busSocket;
    private PrintWriter out;
    private BufferedReader in;

    private boolean isServer;
    private int port = 9999;

    public SoftwareBus(boolean isServer) {
        this.isServer = isServer;
        queue = new LinkedList<>();
        subscriptions = new HashSet<>();
        clientSockets = new HashSet<>();

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
                in = new BufferedReader(new InputStreamReader(busSocket.getInputStream()));
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
     * - In server mode: rebroadcasts received messages to all other clients.
     * - In client mode: checks message topic/subtopic against subscriptions,
     * and stores it in the local queue if relevant.
     */
    private void readerThread(Socket socket) {
        Thread readerThread = new Thread(() -> {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    Message message = Message.parseStringToMsg(line);
                    if (isServer) {
                        System.out.println("Bus (Server) received message:" + message);
                        // Forward to all connected clients except the sender
                        synchronized (clientSockets) {
                            for (Socket client : clientSockets) {
                                if (client != socket) {
                                    PrintWriter temp = new PrintWriter(client.getOutputStream(), true);
                                    temp.println(line);
                                }
                            }
                        }
                        synchronized (queue) {
                            queue.add(message);
                        }
                    } else {
                        System.out.println("Bus (Client) received message:" + message);
                        // Client mode: filter and enqueue matching messages
                        synchronized (subscriptions) {
                            for (Subscription s : subscriptions) {
                                if (s.topic() == message.getTopic() &&
                                        (s.subtopic() == 0 || s.subtopic() == message.getSubTopic())) {
                                    synchronized (queue) {
                                        queue.add(message);
                                        System.out.println("Client saved message\n" + queue.size() + " " + message);
                                    }
                                    // stop checking once matched
                                    break;
                                }
                            }
                        }
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
     * Publishes a message to the bus.
     * - In server mode: broadcast the message to all connected clients.
     * - In client mode: send the message to the central server.
     */
    public void publish(Message message) {
        if (isServer) {
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
     * Registers a subscription to a given topic and subtopic.
     */
    public void subscribe(int topic, int subtopic) {
        subscriptions.add(new Subscription(topic, subtopic));
    }

    /**
     * Retrieves and removes the first message in the queue matching the given topic/subtopic.
     * If subtopic = 0, matches all subtopics.
     * Returns null if no matching message is found.
     */
    public Message get(int topic, int subtopic) {
        synchronized (queue) {
            if (isServer) {
                if (queue.isEmpty()) {
                    return null;
                }
                Message m = queue.getFirst();
                queue.removeFirst();
                return m;
            }

            Iterator<Message> queue_iter = queue.iterator();
            while (queue_iter.hasNext()) {
                Message m = queue_iter.next();
                System.out.println(m.toString());
                if (m.getTopic() == topic && (subtopic == 0 || m.getSubTopic() == subtopic)) {
                    queue_iter.remove();
                    return m;
                }
            }
            return null;
        }
    }

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
}