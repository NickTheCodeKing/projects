import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.HashMap;

public class Server implements Runnable {

    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;
    private int port;
    private Deck deck;
    private ArrayList<Card> dealerHand;
    private HashMap<String, ArrayList<Card>> playerHands;
    private HashMap<String, Boolean> playerStatuses;
    private boolean gameStarted;

    public Server(int port) {
        connections = new ArrayList<>();
        playerHands = new HashMap<>();
        dealerHand = new ArrayList<>();
        playerStatuses = new HashMap<>();
        done = false;
        this.port = port;
        this.gameStarted = false;
    }

    @Override
    public void run() {
        try {
            server = new ServerSocket(port);
            pool = Executors.newCachedThreadPool();
            System.out.println("Server listening on port " + port);
            while (!gameStarted) {
                while (connections.size() < 2) {
                    System.out.println(gameStarted);
                    Socket client = server.accept();
                    ConnectionHandler handler = new ConnectionHandler(client);
                    connections.add(handler);
                    pool.execute(handler);

                    if (connections.size() == 2) {
                        broadcast("Lobby full! No more connections will be accepted.");
                        broadcast("Players, please enter \"READY\" once you are ready to start the game.");
                    }
                }

                if (checkPlayersReady()) {
                    this.gameStarted = true;
                }

                Thread.sleep(1000);
            }
            System.out.println("Both players ready. Game starting....");
            gameLoop();
        } catch (IOException | InterruptedException e) {
            System.out.println(e);
            System.exit(0);
        }

    }

    public boolean checkPlayersReady() {
        for (ConnectionHandler ch : connections) {
            if (ch.ready == false) {
                return false;
            }
        }
        return true;
    }

    public void setGameStarted(boolean gameStarted) {
        this.gameStarted = gameStarted;
    }

    public void gameLoop() {
        deck = new Deck();
        deck.shuffle();
        dealInitialCards();

        for (ConnectionHandler ch : connections) {
            playerStatuses.put(ch.getUsername(), false);
        }

        boolean roundInProgress = true;
        while (roundInProgress) {
            for (ConnectionHandler player : connections) {
                String username = player.getUsername();
                ArrayList<Card> hand = playerHands.get(username);

                if (playerStatuses.get(username)) {
                    continue;
                }

                player.sendMessage("Your current hand: " + hand);
                try {
                    String command = player.getCommand();
                    if (command.equalsIgnoreCase("hit")) {
                        handleHit(player);
                        if (calculateHandValue(playerHands.get(username)) > 21) {
                            player.sendMessage("You've busted!");
                            playerStatuses.put(username, true);
                            roundInProgress = false;
                            break;
                        }
                    } else if (command.equalsIgnoreCase("stand")) {
                        player.sendMessage("You chose to stand.");
                        playerStatuses.put(username, true);
                    }
                } catch (IOException e) {
                    System.out.println("Error reading command from " + username);
                }
            }

            if (allPlayersDone()) {
                playDealerHand();
                calculateResults();
                roundInProgress = false;
            }
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

    public void dealInitialCards() {
        for (ConnectionHandler ch : connections) {
            String username = ch.getUsername();
            ArrayList<Card> hand = new ArrayList<>();
            hand.add(deck.drawCard());
            hand.add(deck.drawCard());
            playerHands.put(username, hand);
            playerStatuses.put(username, false);
            ch.sendMessage("Your hand: " + printHand(hand));
        }
        dealerHand.clear();
        dealerHand.add(deck.drawCard());
        dealerHand.add(deck.drawCard());
        broadcast("Dealer's visible card: " + dealerHand.get(0));
    }

    public void handleHit(ConnectionHandler player) {
        String username = player.getUsername();
        Card newCard = deck.drawCard();
        playerHands.get(username).add(newCard);
        player.sendMessage("You drew: " + newCard);
        broadcast(username + " now has: " + playerHands.get(username));
        if (calculateHandValue(playerHands.get(username)) > 21) {
            broadcast(username = " has busted!");
            playerStatuses.put(username, true);
        }
    }

    public void playDealerHand() {
        while (calculateHandValue(dealerHand) < 17) {
            Card newCard = deck.drawCard();
            dealerHand.add(newCard);
            broadcast("Dealer draws: " + newCard);
            broadcast("Dealer's hand: " + dealerHand + " (value: " + calculateHandValue(dealerHand) + ")");
        }
        broadcast("Dealer's final hand: " + dealerHand + " (value: " + calculateHandValue(dealerHand) + ")");
    }

    public void calculateResults() {
        int dealerValue = calculateHandValue(dealerHand);
        broadcast("Dealer's hand value: " + dealerValue);

        String bestPlayer = null;
        int bestPlayerValue = 0;
        for (String username : playerHands.keySet()) {
            ArrayList<Card> playerHand = playerHands.get(username);
            int playerValue = calculateHandValue(playerHand);

            if (playerValue <= 21 && playerValue > bestPlayerValue) {
                bestPlayer = username;
                bestPlayerValue = playerValue;
            }
        }

        if (bestPlayer == null) {
            broadcast("All players have busted! Dealer wins.");
        } else {
            if (dealerValue > 21) {
                broadcast("Dealer has busted! " + bestPlayer + " wins with a hand value of " + bestPlayerValue + "!");
            } else if (bestPlayerValue > dealerValue) {
                broadcast(bestPlayer + " wins with a hand value of " + bestPlayerValue + "!");
            } else if (bestPlayerValue < dealerValue) {
                broadcast("Dealer wins with a hand value of " + dealerValue + ". " + bestPlayer + " loses.");
            } else {
                broadcast(bestPlayer + " ties with the dealer. Both have a hand value of " + bestPlayerValue + ".");
            }
        }
    }

    public int calculateHandValue(ArrayList<Card> hand) {
        int value = 0;
        int aces = 0;

        for (Card card : hand) {
            value += card.getValue();
            if (card.getRank().equals("Ace")) {
                aces++;
            }
        }

        while (value > 21 && aces > 0) {
            value -= 10;
            aces--;
        }

        return value;
    }

    public String printHand(ArrayList<Card> hand) {
        String s = "[";
        int handSize = hand.size();
        int index = 1;
        for (Card card : hand) {
            if (index < handSize) {
                s += card.toString() + ", ";
            } else if (index == handSize) {
                s += card.toString() + "]";
            }

            index++;
        }
        return s;
    }

    public boolean allPlayersDone() {
        for (boolean isDone : playerStatuses.values()) {
            if (!isDone) {
                return false;
            }
        }
        return true;
    }

    class ConnectionHandler implements Runnable {

        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String username;
        private boolean ready;

        public ConnectionHandler(Socket client) {
            this.client = client;
            this.ready = false;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                while (username == null || username.isEmpty()) {
                    out.println("Waiting for input: ");
                    String usernameInput = in.readLine();
                    if (usernameInput.startsWith("username =")) {
                        username = usernameInput.split(" ")[2];
                    } else {
                        out.println(
                                "Invalid username provided. Please enter username in the format: \"username = {username}\"");
                    }
                }
                System.out.println(username + " connected!");
                broadcast("Server: Welcome " + username);

                String message;
                while ((message = in.readLine()) != null) {
                    handleMessage(message);
                }
            } catch (IOException e) {
                if (this.username != null) {
                    System.out.println("Server: " + username + " has left the chat.");
                }
            } finally {
                closeConnection();
            }

        }

        public String getCommand() throws IOException {
            out.println("Enter your move (hit/stand): ");
            return in.readLine();
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        public String getUsername() {
            return username;
        }

        public boolean isReady() {
            return ready;
        }

        public void handleMessage(String message) {
            if (message.equalsIgnoreCase("bye")) {
                broadcast(username + ": " + message);
                broadcast("Server: Goodbye " + username);
            } else if (message.equalsIgnoreCase("allusers")) {
                out.println("Active Users: " + String.join(", ", activeUsers()));
            } else if (message.equals("READY")) {
                if (!gameStarted) {
                    broadcast(username + " is ready!");
                    this.ready = true;
                }
            } else {
                broadcast(username + ": " + message);
            }
        }

        public void closeConnection() {
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

    public static void main(String[] args) {
        try {
            int port = Integer.parseInt(args[0]);
            Server server = new Server(port);
            server.run();
        } catch (Exception e) {
            System.out.println(
                    "Port not provided. Please run program in the following format: java Server {Port #}");
        }
    }
}