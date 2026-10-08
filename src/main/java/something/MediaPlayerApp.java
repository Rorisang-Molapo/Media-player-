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
import javafx.util.Duration;

public class MediaPlayerApp extends Application {

    // Global variables
    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    private ObservableList<File> playlist = FXCollections.observableArrayList();
    private ListView<File> playlistView;

    private Slider progressSlider;
    private Slider volumeSlider;
    private Label timeLabel;
    private Label statusBadge;
    private Button playPauseBtn;

    private boolean isMuted = false;
    private double lastVolume = 0.8;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Media Player");

        // Top title
        Label title = new Label("Media Player");
        title.setStyle("-fx-text-fill: #00e5ff; -fx-font-weight: bold; -fx-font-size: 11px;");

        // Video Player Box
        mediaView = new MediaView();
        mediaView.setFitWidth(380);
        mediaView.setFitHeight(200);
        mediaView.setPreserveRatio(true);

        statusBadge = new Label("READY");
        statusBadge.setStyle("-fx-text-fill: #00e5ff; -fx-background-color: rgba(0,0,0,0.6); -fx-padding: 2px 6px; -fx-background-radius: 4px; -fx-font-size: 9px; -fx-font-weight: bold;");

        StackPane videoPane = new StackPane();
        videoPane.getChildren().addAll(mediaView, statusBadge);
        videoPane.setPrefSize(380, 200);
        videoPane.setStyle("-fx-background-color: #080a0f; -fx-background-radius: 10px; -fx-border-color: #1e2638; -fx-border-radius: 10px;");
        StackPane.setAlignment(statusBadge, Pos.TOP_LEFT);
        StackPane.setMargin(statusBadge, new Insets(10));

        // Progress bar and time label
        timeLabel = new Label("00:00 / 00:00");
        timeLabel.setStyle("-fx-text-fill: #8b9bb4; -fx-font-size: 11px;");

        progressSlider = new Slider(0, 100, 0);
        progressSlider.setStyle("-fx-control-inner-background: #1e2638; -fx-accent: #00e5ff;");
        HBox.setHgrow(progressSlider, Priority.ALWAYS);

        // Click on slider track to jump to position directly
        progressSlider.setOnMousePressed(e -> seekToMousePosition(e.getX()));
        // Drag slider knob/track to update position continuously
        progressSlider.setOnMouseDragged(e -> seekToMousePosition(e.getX()));

        HBox progressBox = new HBox(10);
        progressBox.getChildren().addAll(timeLabel, progressSlider);
        progressBox.setAlignment(Pos.CENTER);

        // Control Buttons
        Button muteBtn = new Button("vol");
        muteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8b9bb4; -fx-font-size: 14px;");
        muteBtn.setOnAction(e -> toggleMute());

        volumeSlider = new Slider(0, 1, 0.8);
        volumeSlider.setPrefWidth(70);
        volumeSlider.setStyle("-fx-accent: #00e5ff;");
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(newVal.doubleValue());
            }
        });

        Button stopBtn = new Button("stp");
        stopBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8b9bb4; -fx-font-size: 14px;");
        stopBtn.setOnAction(e -> stopMedia());

        Button prevBtn = new Button("prev");
        prevBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8b9bb4; -fx-font-size: 14px;");
        prevBtn.setOnAction(e -> playPrevious());

        playPauseBtn = new Button("P");
        playPauseBtn.setStyle("-fx-background-color: #00e5ff; -fx-text-fill: #090c10; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 10px; -fx-pref-width: 42px; -fx-pref-height: 42px;");
        playPauseBtn.setOnAction(e -> togglePlayPause());

        Button nextBtn = new Button("next");
        nextBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8b9bb4; -fx-font-size: 14px;");
        nextBtn.setOnAction(e -> playNext());

        HBox controlBar = new HBox(8);
        controlBar.getChildren().addAll(muteBtn, volumeSlider, stopBtn, prevBtn, playPauseBtn, nextBtn);
        controlBar.setAlignment(Pos.CENTER);
        controlBar.setPadding(new Insets(8));
        controlBar.setStyle("-fx-background-color: #121621; -fx-background-radius: 12px; -fx-border-color: #1e2638; -fx-border-radius: 12px;");

        // Key shortcuts label bar
        HBox shortcutBar = new HBox(12);
        shortcutBar.getChildren().addAll(
            createKeyHint("SPACE", "Play"),
            createKeyHint("S", "Stop"),
            createKeyHint("N", "Next"),
            createKeyHint("P", "Prev"),
            createKeyHint("M", "Mute")
        );
        shortcutBar.setAlignment(Pos.CENTER);

        // Playlist section
        Label queueTitle = new Label("Queue Session");
        queueTitle.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 14px;");

        Button addBtn = new Button("+ Add Media");
        addBtn.setStyle("-fx-background-color: #00e5ff; -fx-text-fill: #090c10; -fx-font-weight: bold; -fx-background-radius: 8px;");
        addBtn.setOnAction(e -> addFiles(stage));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox playlistHeader = new HBox();
        playlistHeader.getChildren().addAll(queueTitle, spacer, addBtn);
        playlistHeader.setAlignment(Pos.CENTER_LEFT);

        playlistView = new ListView<>(playlist);
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
                    Label name = new Label(item.getName());
                    if (getItem() == playlistView.getSelectionModel().getSelectedItem()) {
                        name.setStyle("-fx-text-fill: #00e5ff; -fx-font-weight: bold;");
                    } else {
                        name.setStyle("-fx-text-fill: #e1e8f0;");
                    }

                    Label ext = new Label(getFileExtension(item).toUpperCase());
                    ext.setStyle("-fx-text-fill: #00e5ff; -fx-background-color: #002a3a; -fx-padding: 2px 6px; -fx-background-radius: 4px; -fx-font-size: 9px; -fx-font-weight: bold;");

                    Button removeBtn = new Button("✕");
                    removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8b9bb4;");
                    removeBtn.setOnAction(e -> removeFile(item));

                    Region cellSpacer = new Region();
                    HBox.setHgrow(cellSpacer, Priority.ALWAYS);

                    HBox row = new HBox(8);
                    row.getChildren().addAll(name, ext, cellSpacer, removeBtn);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(6, 10, 6, 10));
                    row.setStyle("-fx-background-color: #121621; -fx-background-radius: 8px;");

                    setGraphic(row);
                    setStyle("-fx-background-color: transparent;");
                }
            }
        });

        playlistView.getSelectionModel().selectedIndexProperty().addListener((obs, oldIndex, newIndex) -> {
            if (newIndex != null && newIndex.intValue() >= 0) {
                loadAndPlay(newIndex.intValue());
            }
        });

        VBox playlistPane = new VBox(10);
        playlistPane.getChildren().addAll(playlistHeader, playlistView);
        playlistPane.setPadding(new Insets(12));
        playlistPane.setStyle("-fx-background-color: #121621; -fx-background-radius: 12px; -fx-border-color: #1e2638; -fx-border-radius: 12px;");

        // Main layout
        VBox root = new VBox(12);
        root.getChildren().addAll(title, videoPane, progressBox, controlBar, shortcutBar, playlistPane);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: #090c10;");

        Scene scene = new Scene(root, 420, 680);
        scene.setOnKeyPressed(this::handleKeyPressed);

        stage.setScene(scene);
        stage.show();
        root.requestFocus();
    }

    // Direct slider position calculator for clicks and drags
    private void seekToMousePosition(double mouseX) {
        if (mediaPlayer != null && mediaPlayer.getTotalDuration() != null) {
            double sliderWidth = progressSlider.getWidth();
            double percentage = mouseX / sliderWidth;

            if (percentage < 0) percentage = 0;
            if (percentage > 1) percentage = 1;

            progressSlider.setValue(percentage * 100.0);
            double targetSeconds = mediaPlayer.getTotalDuration().toSeconds() * percentage;
            mediaPlayer.seek(Duration.seconds(targetSeconds));
        }
    }

    // Skip forward or backward by specific seconds
    private void seekRelative(double seconds) {
        if (mediaPlayer != null && mediaPlayer.getTotalDuration() != null) {
            double newTime = mediaPlayer.getCurrentTime().toSeconds() + seconds;
            double maxTime = mediaPlayer.getTotalDuration().toSeconds();

            if (newTime < 0) newTime = 0;
            if (newTime > maxTime) newTime = maxTime;

            mediaPlayer.seek(Duration.seconds(newTime));
        }
    }

    // Key shortcut helper
    private HBox createKeyHint(String key, String action) {
        Label k = new Label(key);
        k.setStyle("-fx-background-color: #1e2638; -fx-text-fill: #00e5ff; -fx-font-weight: bold; -fx-padding: 1px 4px; -fx-background-radius: 3px; -fx-font-size: 9px;");
        Label a = new Label(action);
        a.setStyle("-fx-text-fill: #8b9bb4; -fx-font-size: 11px;");
        return new HBox(4, k, a);
    }

    // Media player functions
    private void loadAndPlay(int index) {
        if (index < 0 || index >= playlist.size()) {
            return;
        }

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }

        File file = playlist.get(index);
        Media media = new Media(file.toURI().toString());
        mediaPlayer = new MediaPlayer(media);
        mediaView.setMediaPlayer(mediaPlayer);

        mediaPlayer.setVolume(volumeSlider.getValue());
        mediaPlayer.setOnEndOfMedia(() -> playNext());

        mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (!progressSlider.isValueChanging() && mediaPlayer.getTotalDuration() != null) {
                double current = newTime.toSeconds();
                double total = mediaPlayer.getTotalDuration().toSeconds();
                progressSlider.setValue((current / total) * 100.0);
                timeLabel.setText(formatTime(current) + " / " + formatTime(total));
            }
        });

        mediaPlayer.play();
        playPauseBtn.setText("⏸");
        statusBadge.setText(getFileExtension(file).toUpperCase() + " • ACTIVE");
    }

    private void togglePlayPause() {
        if (mediaPlayer == null && playlist.size() > 0) {
            playlistView.getSelectionModel().select(0);
            return;
        }
        if (mediaPlayer != null) {
            if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                mediaPlayer.pause();
                playPauseBtn.setText("next");
            } else {
                mediaPlayer.play();
                playPauseBtn.setText("P");
            }
        }
    }

    private void stopMedia() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            playPauseBtn.setText("next");
        }
    }

    private void playNext() {
        if (playlist.isEmpty()) {
            return;
        }
        int next = (playlistView.getSelectionModel().getSelectedIndex() + 1) % playlist.size();
        playlistView.getSelectionModel().select(next);
    }

    private void playPrevious() {
        if (playlist.isEmpty()) {
            return;
        }
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
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Media");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Media Files", "*.mp4", "*.mp3", "*.wav", "*.m4a")
        );
        List<File> files = chooser.showOpenMultipleDialog(stage);
        if (files != null) {
            playlist.addAll(files);
            if (playlistView.getSelectionModel().getSelectedIndex() < 0) {
                playlistView.getSelectionModel().select(0);
            }
        }
    }

    private void removeFile(File file) {
        int index = playlist.indexOf(file);
        if (index >= 0) {
            boolean isPlaying = (playlistView.getSelectionModel().getSelectedIndex() == index);
            playlist.remove(file);
            if (isPlaying && mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
                mediaPlayer = null;
            }
        }
    }

    private void handleKeyPressed(KeyEvent event) {
        KeyCode code = event.getCode();
        if (code == KeyCode.SPACE) {
            togglePlayPause();
        } else if (code == KeyCode.S) {
            stopMedia();
        } else if (code == KeyCode.N) {
            playNext();
        } else if (code == KeyCode.P) {
            playPrevious();
        } else if (code == KeyCode.M) {
            toggleMute();
        } else if (code == KeyCode.UP) {
            volumeSlider.setValue(Math.min(1.0, volumeSlider.getValue() + 0.1));
        } else if (code == KeyCode.DOWN) {
            volumeSlider.setValue(Math.max(0.0, volumeSlider.getValue() - 0.1));
        } else if (code == KeyCode.LEFT) {
            seekRelative(-5.0); // Rewind 5s
        } else if (code == KeyCode.RIGHT) {
            seekRelative(5.0);  // Fast-forward 5s
        }
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0) {
            return name.substring(lastDot + 1);
        } else {
            return "MEDIA";
        }
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