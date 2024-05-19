package be.webtechie;

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
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.util.Duration;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.util.Duration;

import java.io.IOException;

public class GeoWarsApp extends GameApplication {

    public enum EntityType {
        PLAYER, BULLET, ENEMY
    }

    private final GeoWarsFactory geoWarsFactory = new GeoWarsFactory();
    private Entity player;
    private GameClient client;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setTitle("Geometry Wars");
    }

    @Override
    protected void initInput() {
        onKey(KeyCode.LEFT, () -> movePlayer("LEFT"));
        onKey(KeyCode.RIGHT, () -> movePlayer("RIGHT"));
        onKey(KeyCode.UP, () -> movePlayer("UP"));
        onKey(KeyCode.DOWN, () -> movePlayer("DOWN"));
        onBtnDown(MouseButton.PRIMARY, () -> shootBullet());
    }

    @Override
    protected void initGame() {
        getGameWorld().addEntityFactory(geoWarsFactory);
        player = spawn("player", getAppWidth() / 2 - 15, getAppHeight() / 2 - 15);
        run(() -> spawn("enemy"), Duration.seconds(1.0));

        try {
            client = new GameClient(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void initPhysics() {
        onCollisionBegin(EntityType.BULLET, EntityType.ENEMY, (bullet, enemy) -> {
            bullet.removeFromWorld();
            enemy.removeFromWorld();
        });

        onCollisionBegin(EntityType.ENEMY, EntityType.PLAYER, (enemy, player) -> {
            showMessage("You Died!", () -> {
                getGameController().startNewGame();
            });
        });
    }

    private void movePlayer(String direction) {
        switch (direction) {
            case "LEFT": player.translateX(-5); break;
            case "RIGHT": player.translateX(5); break;
            case "UP": player.translateY(-5); break;
            case "DOWN": player.translateY(5); break;
        }
        client.sendMessage("MOVE:" + direction);
    }

    private void shootBullet() {
        spawn("bullet", player.getCenter());
        client.sendMessage("SHOOT");
    }

    public void processServerMessage(String message) {
        Platform.runLater(() -> {
            if (message.startsWith("MOVE:")) {
                String direction = message.substring(5);
                movePlayer(direction);
            } else if (message.equals("SHOOT")) {
                shootBullet();
            }
        });
    }
}
