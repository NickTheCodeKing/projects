import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.BufferedReader;
import java.io.PrintWriter;

public class Server implements Runnable {

    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;
    private int port;

    public Server(int port) {
        connections = new ArrayList<>();
        done = false;
        this.port = port;
    }

    @Override
    public void run() {
        try {
            server = new ServerSocket(port);
            pool = Executors.newCachedThreadPool();
            System.out.println("Server listening on port " + port);
            while (!done) {

                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }
        } catch (IOException e) {
            System.out.println(e);
            System.exit(0);
        }

    }

    public void broadcast(String message) {
        for (ConnectionHandler ch : connections) {
            if (ch != null) {
                ch.sendMessage(message);
            }
        }
    }

    public ArrayList<String> activeUsers() {
        ArrayList<String> activeUsers = new ArrayList<>();
        for (ConnectionHandler ch : connections) {
            if (ch != null) {
                activeUsers.add(ch.username);
            }
        }
        return activeUsers;
    }

    class ConnectionHandler implements Runnable {

        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String username;

        public ConnectionHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            String username = "";
            String usernameInput = "";
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                while (username.equals("")) {
                    out.println("Waiting for input: ");
                    usernameInput = in.readLine();
                    if (usernameInput.startsWith("username =")) {
                        username = usernameInput.split(" ")[2];
                    } else {
                        out.println(
                                "Invalid username provided. Please enter username in the format: \"username = {username}\"");
                        username = "";
                    }
                }
                System.out.println(username + " connected!");
                broadcast("Server: Welcome " + username);
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.equalsIgnoreCase("bye")) {
                        broadcast(username + ": " + message);
                        broadcast("Server: Goodbye " + username);
                    } else if (message.equalsIgnoreCase("allusers")) {
                        ArrayList<String> activeUserList = activeUsers();
                        String userListString = "Active Users: \n";
                        for (String user : activeUserList) {
                            userListString += user + "\n";
                        }
                        out.println(userListString);
                    } else {
                        broadcast(username + ": " + message);
                    }
                }
            } catch (IOException e) {
                broadcast("Server: " + username + " has left the chat.");
            } finally {
                try {
                    if (client != null) {
                        connections.remove(this);
                        client.close();
                    }
                } catch (IOException e) {
                    System.out.println("Error closing client connection: " + e);
                }
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }
    }

    public static void main(String[] args) {
        try {
            int port = Integer.parseInt(args[0]);
            Server server = new Server(port);
            server.run();
        } catch (Exception e) {
            System.out.println(
                    "Port not provided. Please run program in the following format: java Client {Port #}");
        }
    }
}