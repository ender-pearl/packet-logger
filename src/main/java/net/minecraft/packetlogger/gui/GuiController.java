package net.minecraft.packetlogger.gui;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import lombok.Setter;
import net.minecraft.packetlogger.hook.Hooks;
import net.minecraft.packetlogger.info.ProtocolInfo;
import net.minecraft.packetlogger.info.StateInfo;
import net.minecraft.packetlogger.packet.PacketDirection;
import net.minecraft.packetlogger.packet.PacketModel;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

public class GuiController {
    @FXML private TableView<PacketModel> packetTable;

    @FXML private TableColumn<PacketModel, LocalDateTime> timeColumn;
    @FXML private TableColumn<PacketModel, String> addressColumn;
    @FXML private TableColumn<PacketModel, PacketDirection> directionColumn;
    @FXML private TableColumn<PacketModel, String> stateColumn;
    @FXML private TableColumn<PacketModel, Number> opcodeColumn;
    @FXML private TableColumn<PacketModel, String> nameColumn;
    @FXML private TableColumn<PacketModel, ByteBuf> contentColumn;

    @FXML private ListView<FilterEntry> filterList;
    @FXML private TextField filterSearch;
    @FXML private Button filterCheckAll;
    @FXML private Button filterUncheckAll;

    @Setter
    private Scene scene;

    private FilteredList<PacketModel> filteredPackets = new FilteredList<>(GuiApplication.packets);

    private ObservableList<FilterEntry> filterEntries = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
    private FilteredList<FilterEntry> filteredFilterEntries = new FilteredList<>(filterEntries, s -> true);

    private void updateFilterEntries() {
        filterEntries.clear();
        ProtocolInfo info = Hooks.protocolInfo;
        for (StateInfo stateInfo : info.getStates()) {
            filterEntries.addAll(stateInfo.getServerBound().stream()
                    .map(packetInfo -> new FilterEntry(stateInfo, packetInfo, PacketDirection.OUT))
                    .filter((entry) -> {
                        entry.checkedProperty().addListener((observable) -> {
                            refilterPackets();
                        });
                        return true;
                    })
                    .collect(Collectors.toList()));

            filterEntries.addAll(stateInfo.getClientBound().stream()
                    .map(packetInfo -> new FilterEntry(stateInfo, packetInfo, PacketDirection.IN))
                    .filter((entry) -> {
                        entry.checkedProperty().addListener((observable) -> {

                            refilterPackets();
                        });
                        return true;
                    })
                    .collect(Collectors.toList()));
        }
    }

    @FXML
    private void initialize() {
        updateFilterEntries();

        timeColumn.setCellValueFactory(data -> data.getValue().timestampProperty());
        addressColumn.setCellValueFactory(data -> data.getValue().addressProperty());
        directionColumn.setCellValueFactory(data -> data.getValue().directionProperty());
        stateColumn.setCellValueFactory(data -> data.getValue().stateProperty());
        opcodeColumn.setCellValueFactory(data -> data.getValue().opcodeProperty());
        opcodeColumn.setCellFactory(column -> new TableCell<PacketModel, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                if (item == null || empty) {
                    setText("");
                    return;
                }

                setText(String.format("0x%02x", item.intValue()));
            }
        });
        nameColumn.setCellValueFactory(data -> data.getValue().nameProperty());
        contentColumn.setCellValueFactory(data -> data.getValue().contentProperty());
        contentColumn.setCellFactory(column -> new TableCell<PacketModel, ByteBuf>() {
            @Override
            protected void updateItem(ByteBuf item, boolean empty) {
                if (item == null || empty) {
                    setText("");
                    return;
                }

                setText(ByteBufUtil.hexDump(item));
            }

        });

        packetTable.setItems(filteredPackets);

        filterSearch.textProperty().addListener((observable -> {
            String filter = filterSearch.getText();
            if (filter == null || filter.length() == 0) {
                filteredFilterEntries.setPredicate(s -> true);
            } else {
                filteredFilterEntries.setPredicate(s -> s.toString().toLowerCase().contains(filter.toLowerCase()));
            }
        }));

        filterCheckAll.setOnAction((event) -> filteredFilterEntries.forEach(entry -> entry.setChecked(true)));
        filterUncheckAll.setOnAction((event) -> filteredFilterEntries.forEach(entry -> entry.setChecked(false)));

        filterList.setCellFactory(CheckBoxListCell.forListView(FilterEntry::checkedProperty));
        filterList.setItems(filteredFilterEntries);

        refilterPackets();
    }

    private void refilterPackets() {
        GuiApplication.packetsLock.readLock().lock();
        try {
            filteredPackets.setPredicate(this::filterPacket);
        } finally {
            GuiApplication.packetsLock.readLock().unlock();
        }
    }

    private boolean filterPacket(PacketModel packet) {
        for (FilterEntry entry : filterEntries) {
            if (entry.getPacketInfo().equals(packet.getPacketInfo()) &&
                    entry.getStateInfo().equals(packet.getStateInfo()) &&
                    entry.getDirection().equals(packet.getDirection())) {

                return entry.isChecked();
            }
        }
        return false;
    }

    public void registerAccelerators() {
        KeyCodeCombination keyCodeCombination = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_ANY);
        scene.getAccelerators().put(keyCodeCombination, () -> {
            PacketModel packet = packetTable.getSelectionModel().getSelectedItem();
            if (packet != null) {
                ByteBuf content = packet.contentProperty().get();

                ClipboardContent clipboardContent = new ClipboardContent();
                clipboardContent.putString(ByteBufUtil.hexDump(content));
                Clipboard.getSystemClipboard().setContent(clipboardContent);
            }
        });
    }
}
