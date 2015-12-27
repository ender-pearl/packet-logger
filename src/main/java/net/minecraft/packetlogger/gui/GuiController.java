package net.minecraft.packetlogger.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
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
    @FXML private TableColumn<PacketModel, String> contentColumn;

    @FXML private ListView<FilterEntry> filterList;
    @FXML private TextField filterSearch;
    @FXML private Button filterCheckAll;
    @FXML private Button filterUncheckAll;

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
                            filteredPackets.setPredicate(this::filterPacket);
                        });
                        return true;
                    })
                    .collect(Collectors.toList()));

            filterEntries.addAll(stateInfo.getClientBound().stream()
                    .map(packetInfo -> new FilterEntry(stateInfo, packetInfo, PacketDirection.IN))
                    .filter((entry) -> {
                        entry.checkedProperty().addListener((observable) -> {
                            filteredPackets.setPredicate(this::filterPacket);
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
                if (item == null) {
                    return;
                }

                setText(String.format("0x%02x", item.intValue()));
            }
        });
        nameColumn.setCellValueFactory(data -> data.getValue().nameProperty());

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

        filteredPackets.setPredicate(this::filterPacket);
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
}
