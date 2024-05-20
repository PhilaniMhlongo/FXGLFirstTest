package be.webtechie;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static com.almasb.fxgl.dsl.FXGL.getAppHeight;
import static com.almasb.fxgl.dsl.FXGL.getAppWidth;
import static com.almasb.fxgl.dsl.FXGL.getGameController;
import static com.almasb.fxgl.dsl.FXGL.getGameWorld;
import static com.almasb.fxgl.dsl.FXGL.onBtnDown;
import static com.almasb.fxgl.dsl.FXGL.onCollisionBegin;
import static com.almasb.fxgl.dsl.FXGL.onKey;
import static com.almasb.fxgl.dsl.FXGL.run;
import static com.almasb.fxgl.dsl.FXGL.showMessage;
import static com.almasb.fxgl.dsl.FXGL.spawn;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.util.Duration;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.util.Duration;

import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javafx.geometry.Rectangle2D;
public class GeoWarsApp extends GameApplication {
    private final ConcurrentHashMap<Integer, Entity> players = new ConcurrentHashMap<>();
    private final ExecutorService playerExecutor = Executors.newCachedThreadPool();
    private final int MAX_PLAYERS = 4;
    private int health = 100;
    private int ammo = 10;
    private int playerId;
    public enum EntityType {
        PLAYER, BULLET, REPAIR, RELOAD,OBSTACLES
    }

    private final GeoWarsFactory geoWarsFactory = new GeoWarsFactory();
    private Entity player;
    private Entity player1;
    private GameClient client;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setTitle("Robots World");
    }

    @Override
    protected void initInput() {
        onKey(KeyCode.LEFT, () -> movePlayer(1,"LEFT"));
        onKey(KeyCode.RIGHT, () -> movePlayer(1,"RIGHT"));
        onKey(KeyCode.UP, () -> movePlayer(1,"UP"));
        onKey(KeyCode.DOWN, () -> movePlayer(1,"DOWN"));
        onBtnDown(MouseButton.PRIMARY, () -> shootBullet(1));
    }

    @Override
    protected void initGame() {
        getGameWorld().addEntityFactory(geoWarsFactory);
        for (int i = 0; i < MAX_PLAYERS; i++) {
            createPlayer(i + 1);
        }
//        player = spawn("player", getAppWidth() / 2 - 15, getAppHeight() / 2 - 15);
//        player1 = spawn("player", Math.random() * getAppWidth(), Math.random() * getAppHeight());
        spawn("obstacles", new SpawnData(100, 100));
        spawn("obstacles", new SpawnData(300, 200));
        spawn("obstacles", new SpawnData(500, 300));
        run(() -> spawn("repair", Math.random() * getAppWidth(), Math.random() * getAppHeight()), Duration.seconds(60));
        run(() -> spawn("reload", Math.random() * getAppWidth(), Math.random() * getAppHeight()), Duration.seconds(5));


        try {
            client = new GameClient(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createPlayer(int playerId) {
        Entity player = spawn("player", getAppWidth() / 2 - 15, getAppHeight() / 2 - 15);
        player.setProperty("playerId", playerId); // Set the playerId property
        player.setProperty("health", 100); // Initialize health property
        players.put(playerId, player);
        playerExecutor.execute(new PlayerThread(playerId, player));
    }
    private class PlayerThread implements Runnable {
        private final int playerId;
        private final Entity player;

        public PlayerThread(int playerId, Entity player) {
            this.playerId = playerId;
            this.player = player;
        }

        @Override
        public void run() {
            // Example: handle player actions
            while (true) {
                try {
                    // Simulate player actions (e.g., movement, shooting)
                    Thread.sleep(100); // Example delay
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }


    @Override
    protected void initPhysics() {
        onCollisionBegin(EntityType.BULLET, EntityType.OBSTACLES, (bullet, enemy) -> {
            bullet.removeFromWorld();
//            enemy.removeFromWorld();
            client.sendMessage("DAMAGE:10");
        });

        onCollisionBegin(EntityType.OBSTACLES, EntityType.PLAYER, (obstacle, player) -> {
            handlePlayerObstacleCollision(player, obstacle);
        });

        onCollisionBegin(EntityType.PLAYER, EntityType.REPAIR, (player, repair) -> {
            repair.removeFromWorld();
            int playerId = player.getInt("playerId");
            repairPlayer(playerId );
        });

        onCollisionBegin(EntityType.PLAYER, EntityType.RELOAD, (player, reload) -> {
            reload.removeFromWorld();
            int playerId = player.getInt("playerId");
            reloadPlayer(playerId);
        });
    }

    private void movePlayer(int playerId, String direction) {
        Entity player = players.get(playerId);

        if (player != null) {
            synchronized (player) {
                switch (direction) {
                    case "LEFT":
                        player.translateX(-5);
                        player.setRotation(-90);
                        break;
                    case "RIGHT":
                        player.translateX(5);
                        player.setRotation(90);
                        break;
                    case "UP":
                        player.translateY(-5);
                        player.setRotation(0);
                        break;
                    case "DOWN":
                        player.translateY(5);
                        player.setRotation(180);
                        break;
                }
            }
            client.sendMessage("MOVE:" + direction);
        }
    }
    private void handlePlayerObstacleCollision(Entity player, Entity obstacle) {
        // Get the bounding box dimensions
        double playerWidth = player.getBoundingBoxComponent().getWidth();
        double playerHeight = player.getBoundingBoxComponent().getHeight();

        double obstacleWidth = obstacle.getBoundingBoxComponent().getWidth();
        double obstacleHeight = obstacle.getBoundingBoxComponent().getHeight();

        // Calculate the player and obstacle positions
        double playerX = player.getPosition().getX();
        double playerY = player.getPosition().getY();

        double obstacleX = obstacle.getPosition().getX();
        double obstacleY = obstacle.getPosition().getY();

        // Calculate the half-widths and half-heights of the player and obstacle
        double playerHalfWidth = playerWidth / 2.0;
        double playerHalfHeight = playerHeight / 2.0;

        double obstacleHalfWidth = obstacleWidth / 2.0;
        double obstacleHalfHeight = obstacleHeight / 2.0;


        // Calculate the distance between the centers of the player and obstacle
        double dx = (playerX + playerHalfWidth) - (obstacleX + obstacleHalfWidth);
        double dy = (playerY + playerHalfHeight) - (obstacleY + obstacleHalfHeight);


        // Apply the translation to the player to separate it from the obstacle
        player.translateX(dx);
        player.translateY(dy);
    }


    private void shootBullet(int playerId) {
        Entity player = players.get(playerId);
        if (player != null && ammo > 0) {
            spawn("bullet", player.getCenter());
            client.sendMessage("SHOOT:" + playerId);
            ammo--;
        } else {
            System.out.println("No ammo left for player " + playerId + "!");
        }
    }

    private void repairPlayer(int playerId) {
        health = Math.min(100, health + 20);  // Increase health by 20 up to a max of 100
        client.sendMessage("REPAIR:" + playerId);
    }

    private void reloadPlayer(int playerId) {
        ammo = 10;  // Refill ammo
        client.sendMessage("RELOAD:" + playerId);
    }


    private void damagePlayer(int playerId, int amount) {
        health = Math.max(0, health - amount);  // Decrease health by the given amount
        if (health == 0) {
            showMessage("Player " + playerId + " Died!", () -> {
                getGameController().startNewGame();
            });
        }
    }

    public void processServerMessage(String message) {
        Platform.runLater(() -> {
            String[] parts = message.split(":");
            int playerId = Integer.parseInt(parts[1].trim()); // Extract player ID from the message
//            System.out.println(message);
            switch (parts[0]) {
                case "MOVE":
                    String direction = parts[2];
                    System.out.println(message);
                    movePlayer(playerId, direction);
                    break;
                case "SHOOT":
                    shootBullet(playerId);
                    break;
                case "REPAIR":
                    // Assume the player ID is part of the message
                    String playerIdStr = message.substring(7);
                    playerId = Integer.parseInt(playerIdStr);
                    repairPlayer(playerId);
                    break;
                case "RELOAD":
                    reloadPlayer(playerId);
                    break;
                case "DAMAGE":
                    int amount = Integer.parseInt(parts[2]);
                    damagePlayer(playerId, amount);
                    break;
            }
        });
    }

}
