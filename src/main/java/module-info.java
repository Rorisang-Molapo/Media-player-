module something {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;

    opens something to javafx.fxml;
    exports something;
}