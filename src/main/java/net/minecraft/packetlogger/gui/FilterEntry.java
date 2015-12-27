package net.minecraft.packetlogger.gui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.Data;
import lombok.Getter;
import net.minecraft.packetlogger.info.PacketInfo;
import net.minecraft.packetlogger.info.StateInfo;
import net.minecraft.packetlogger.packet.PacketDirection;

@Data
public class FilterEntry {
    private final StateInfo stateInfo;
    private final PacketInfo packetInfo;
    private final PacketDirection direction;

    private final BooleanProperty checked = new SimpleBooleanProperty();

    @Override
    public String toString() {
        return String.format("%s %s 0x%02x %s", stateInfo.getName(), direction.equals(PacketDirection.OUT) ? "S" : "C", packetInfo.getId(), packetInfo.getName());
    }

    public BooleanProperty checkedProperty() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked.set(checked);
    }

    public boolean isChecked() {
        return checked.get();
    }
}
