package something;

import java.io.File;
import java.util.List;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class MediaPlayerApp extends Application {

    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    private final ObservableList<File> playlist = FXCollections.observableArrayList();
    private final ListView<File> playlistView = new ListView<>(playlist);

    private Slider progressSlider;
    private Slider volumeSlider;
    private Label timeLabel;
    private Label statusBadge;
    private Button playPauseBtn;

    private boolean isMuted = false;
    private double lastVolume = 0.8;

    // Styling Constants
    private static final String CARD_BG = "-fx-background-color: #121621; -fx-background-radius: 12px; -fx-border-color: #1e2638; -fx-border-radius: 12px;";
    private static final String BTN_CYAN = "-fx-background-color: #00e5ff; -fx-text-fill: #090c10; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-cursor: hand;";
    private static final String BTN_ICON = "-fx-background-color: transparent; -fx-text-fill: #8b9bb4; -fx-font-size: 14px; -fx-cursor: hand;";
    private static final String TEXT_CYAN = "-fx-text-fill: #00e5ff; -fx-font-weight: bold;";
    private static final String TEXT_MUTED = "-fx-text-fill: #8b9bb4; -fx-font-size: 11px;";

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Media Player");

        // Top Status Header
        Label headerTitle = new Label("Media Player");
        headerTitle.setStyle("-fx-text-fill: #00e5ff; -fx-font-weight: bold; -fx-font-size: 11px;");

        HBox topHeader = new HBox(8, headerTitle);
        topHeader.setAlignment(Pos.CENTER_LEFT);

        
        Region spacerHeader = new Region();
        HBox.setHgrow(spacerHeader, Priority.ALWAYS);
        HBox engineBar = new HBox(spacerHeader);
        engineBar.setAlignment(Pos.CENTER_LEFT);

        // Media Container & Overlay
        mediaView = new MediaView();
        mediaView.setFitWidth(380);
        mediaView.setFitHeight(200);
        mediaView.setPreserveRatio(true);

        StackPane videoPane = new StackPane(mediaView);
        videoPane.setPrefSize(380, 200);
        videoPane.setStyle("-fx-background-color: #080a0f; -fx-background-radius: 10px; -fx-border-color: #1e2638; -fx-border-radius: 10px;");

        statusBadge = createBadge("1080p 60FPS", "#00e5ff", "rgba(0,0,0,0.6)");
        StackPane.setAlignment(statusBadge, Pos.TOP_LEFT);
        StackPane.setMargin(statusBadge, new Insets(10));

        Button switchVisualizer = new Button("⇆ Switch to Visualizer");
        switchVisualizer.setStyle("-fx-background-color: rgba(0,229,255,0.15); -fx-text-fill: #00e5ff; -fx-border-color: #00e5ff; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-font-size: 10px;");
        StackPane.setAlignment(switchVisualizer, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(switchVisualizer, new Insets(10));

        videoPane.getChildren().addAll(statusBadge, switchVisualizer);

        // Progress Slider Section
        progressSlider = new Slider(0, 100, 0);
        progressSlider.setStyle("-fx-control-inner-background: #1e2638; -fx-accent: #00e5ff;");
        HBox.setHgrow(progressSlider, Priority.ALWAYS);

        timeLabel = new Label("00:00 / 00:00");
        timeLabel.setStyle(TEXT_MUTED);

        HBox progressBox = new HBox(10, timeLabel, progressSlider);
        progressBox.setAlignment(Pos.CENTER);

        // Control Toolbar
        Button muteBtn = new Button("🔊");
        muteBtn.setStyle(BTN_ICON);
        muteBtn.setOnAction(e -> toggleMute());

        volumeSlider = new Slider(0, 1, 0.8);
        volumeSlider.setPrefWidth(70);
        volumeSlider.setStyle("-fx-accent: #00e5ff;");
        volumeSlider.valueProperty().addListener((obs, oldV, newV) -> {
            if (mediaPlayer != null) mediaPlayer.setVolume(newV.doubleValue());
        });

        Button prevBtn = new Button("⏮");
        playPauseBtn = new Button("⏸");
        Button nextBtn = new Button("⏭");
        Button stopBtn = new Button("⏹");

        prevBtn.setStyle(BTN_ICON);
        nextBtn.setStyle(BTN_ICON);
        stopBtn.setStyle(BTN_ICON);
        playPauseBtn.setStyle("-fx-background-color: #00e5ff; -fx-text-fill: #090c10; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 10px; -fx-pref-width: 42px; -fx-pref-height: 42px;");

        playPauseBtn.setOnAction(e -> togglePlayPause());
        stopBtn.setOnAction(e -> stopMedia());
        prevBtn.setOnAction(e -> playPrevious());
        nextBtn.setOnAction(e -> playNext());

        HBox transportBar = new HBox(8, muteBtn, volumeSlider, stopBtn, prevBtn, playPauseBtn, nextBtn);
        transportBar.setAlignment(Pos.CENTER);
        transportBar.setPadding(new Insets(8));
        transportBar.setStyle(CARD_BG);

        // Keyboard Shortcut Hints Bar
        HBox shortcutBar = new HBox(12,
            createKeyHint("SPACE", "Play"),
            createKeyHint("S", "Stop"),
            createKeyHint("N", "Next"),
            createKeyHint("P", "Prev"),
            createKeyHint("M", "Mute")
        );
        shortcutBar.setAlignment(Pos.CENTER);

        // Queue / Playlist Section
        Label queueTitle = new Label("Queue Session");
        queueTitle.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 14px;");

        Button addBtn = new Button("+ Add Media");
        addBtn.setStyle(BTN_CYAN);
        addBtn.setOnAction(e -> addFiles(primaryStage));

        Region queueSpacer = new Region();
        HBox.setHgrow(queueSpacer, Priority.ALWAYS);
        HBox queueHeader = new HBox(queueTitle, queueSpacer, addBtn);
        queueHeader.setAlignment(Pos.CENTER_LEFT);

        playlistView.setPrefHeight(180);
        playlistView.setStyle("-fx-background-color: transparent; -fx-control-inner-background: #090c10;");
        playlistView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    Label nameLbl = new Label(item.getName());
                    nameLbl.setStyle(getItem() == playlistView.getSelectionModel().getSelectedItem() ? TEXT_CYAN : "-fx-text-fill: #e1e8f0;");
                    
                    Label extBadge = createBadge(getFileExtension(item).toUpperCase(), "#00e5ff", "#002a3a");
                    Button removeBtn = new Button("✕");
                    removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8b9bb4; -fx-cursor: hand;");
                    removeBtn.setOnAction(e -> removeFile(item));

                    Region sp = new Region();
                    HBox.setHgrow(sp, Priority.ALWAYS);
                    
                    HBox cellBox = new HBox(8, nameLbl, extBadge, sp, removeBtn);
                    cellBox.setAlignment(Pos.CENTER_LEFT);
                    cellBox.setPadding(new Insets(6, 10, 6, 10));
                    cellBox.setStyle("-fx-background-color: #121621; -fx-background-radius: 8px;");

                    setGraphic(cellBox);
                    setStyle("-fx-background-color: transparent;");
                }
            }
        });

        playlistView.getSelectionModel().selectedIndexProperty().addListener((obs, oldIdx, newIdx) -> {
            if (newIdx != null && newIdx.intValue() >= 0) {
                loadAndPlay(newIdx.intValue());
            }
        });

        VBox playlistPane = new VBox(10, queueHeader, playlistView);
        playlistPane.setPadding(new Insets(12));
        playlistPane.setStyle(CARD_BG);

        // Layout Assembly
        VBox root = new VBox(12, topHeader, engineBar, videoPane, progressBox, transportBar, shortcutBar, playlistPane);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: #090c10;");

        Scene scene = new Scene(root, 420, 720);
        scene.setOnKeyPressed(this::handleKeyPressed);

        primaryStage.setScene(scene);
        primaryStage.show();
        root.requestFocus();
    }

    // Helper UI Factories
    private Label createBadge(String text, String color, String bgColor) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: " + color + "; -fx-background-color: " + bgColor + "; -fx-padding: 2px 6px; -fx-background-radius: 4px; -fx-font-size: 9px; -fx-font-weight: bold;");
        return lbl;
    }

    private HBox createKeyHint(String key, String action) {
        Label keyLbl = new Label(key);
        keyLbl.setStyle("-fx-background-color: #1e2638; -fx-text-fill: #00e5ff; -fx-font-weight: bold; -fx-padding: 1px 4px; -fx-background-radius: 3px; -fx-font-size: 9px;");
        Label actLbl = new Label(action);
        actLbl.setStyle(TEXT_MUTED);
        return new HBox(4, keyLbl, actLbl);
    }

    // Controls & Logic
    private void handleKeyPressed(KeyEvent event) {
        KeyCode code = event.getCode();
        switch (code) {
            case SPACE -> togglePlayPause();
            case S -> stopMedia();
            case N -> playNext();
            case P -> playPrevious();
            case UP -> volumeSlider.setValue(Math.min(1.0, volumeSlider.getValue() + 0.1));
            case DOWN -> volumeSlider.setValue(Math.max(0.0, volumeSlider.getValue() - 0.1));
            case M -> toggleMute();
            default -> {}
        }
    }

    private void loadAndPlay(int index) {
        if (index < 0 || index >= playlist.size()) return;

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }

        File file = playlist.get(index);
        Media media = new Media(file.toURI().toString());
        mediaPlayer = new MediaPlayer(media);
        mediaView.setMediaPlayer(mediaPlayer);

        mediaPlayer.setVolume(volumeSlider.getValue());
        mediaPlayer.setOnEndOfMedia(this::playNext);

        mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (!progressSlider.isValueChanging() && mediaPlayer.getTotalDuration() != null) {
                double current = newTime.toSeconds();
                double total = mediaPlayer.getTotalDuration().toSeconds();
                progressSlider.setValue((current / total) * 100.0);
                timeLabel.setText(formatTime(current) + " / " + formatTime(total));
            }
        });

        progressSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (progressSlider.isValueChanging() && mediaPlayer != null && mediaPlayer.getTotalDuration() != null) {
                mediaPlayer.seek(mediaPlayer.getTotalDuration().multiply(newVal.doubleValue() / 100.0));
            }
        });

        mediaPlayer.play();
        playPauseBtn.setText("⏸");
        statusBadge.setText(getFileExtension(file).toUpperCase() + " • ACTIVE");
    }

    private void togglePlayPause() {
        if (mediaPlayer == null && !playlist.isEmpty()) {
            playlistView.getSelectionModel().select(0);
            return;
        }
        if (mediaPlayer != null) {
            if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                mediaPlayer.pause();
                playPauseBtn.setText("▶");
            } else {
                mediaPlayer.play();
                playPauseBtn.setText("⏸");
            }
        }
    }

    private void stopMedia() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            playPauseBtn.setText("▶");
        }
    }

    private void playNext() {
        if (playlist.isEmpty()) return;
        int next = (playlistView.getSelectionModel().getSelectedIndex() + 1) % playlist.size();
        playlistView.getSelectionModel().select(next);
    }

    private void playPrevious() {
        if (playlist.isEmpty()) return;
        int current = playlistView.getSelectionModel().getSelectedIndex();
        int prev = (current - 1 + playlist.size()) % playlist.size();
        playlistView.getSelectionModel().select(prev);
    }

    private void toggleMute() {
        if (isMuted) {
            volumeSlider.setValue(lastVolume);
            isMuted = false;
        } else {
            lastVolume = volumeSlider.getValue();
            volumeSlider.setValue(0);
            isMuted = true;
        }
    }

    private void addFiles(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Media");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Media Files", "*.mp4", "*.mp3", "*.wav", "*.m4a")
        );
        List<File> selected = fileChooser.showOpenMultipleDialog(stage);
        if (selected != null) {
            playlist.addAll(selected);
            if (playlistView.getSelectionModel().getSelectedIndex() < 0) {
                playlistView.getSelectionModel().select(0);
            }
        }
    }

    private void removeFile(File file) {
        int index = playlist.indexOf(file);
        if (index >= 0) {
            boolean isPlaying = playlistView.getSelectionModel().getSelectedIndex() == index;
            playlist.remove(file);
            if (isPlaying) {
                stopMedia();
                if (mediaPlayer != null) {
                    mediaPlayer.dispose();
                    mediaPlayer = null;
                }
            }
        }
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        return lastDot > 0 ? name.substring(lastDot + 1) : "MEDIA";
    }

    private String formatTime(double seconds) {
        int mins = (int) seconds / 60;
        int secs = (int) seconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }

    public static void main(String[] args) {
        launch(args);
    }
}