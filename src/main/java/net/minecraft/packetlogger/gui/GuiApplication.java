package net.minecraft.packetlogger.gui;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import net.minecraft.packetlogger.packet.PacketModel;

public class GuiApplication extends Application {
    public static ObservableList<PacketModel> packets = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());

    public static void run(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/scene.fxml"));

        Scene scene = new Scene(loader.load());

        primaryStage.setTitle("Minecraft Packet Logger");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
