package be.webtechie;

import java.io.*;
import java.net.*;

public class GameClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private GeoWarsApp game;

    public GameClient(GeoWarsApp game) throws IOException {
        this.game = game;
        socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        new Thread(this::listenForMessages).start();
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    private void listenForMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Server says: " + message);
                game.processServerMessage(message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

