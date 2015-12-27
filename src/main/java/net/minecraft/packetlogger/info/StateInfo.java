package net.minecraft.packetlogger.info;

import lombok.Data;
import lombok.Setter;
import net.minecraft.packetlogger.packet.PacketDirection;

import java.util.ArrayList;
import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@Data
public class StateInfo {
    private final int id;
    private final String name;

    @Setter(PRIVATE)
    private List<PacketInfo> serverBound = new ArrayList<>();
    @Setter(PRIVATE)
    private List<PacketInfo> clientBound = new ArrayList<>();

    public void addServerBound(PacketInfo packet) {
        serverBound.add(packet);
    }

    public void addClientBound(PacketInfo packet) {
        clientBound.add(packet);
    }

    public PacketInfo getPacketInfo(PacketDirection direction, int id) {
        return (direction == PacketDirection.OUT ? this.serverBound : this.clientBound).stream()
                .filter(info -> info.getId() == id)
                .findAny().get();
    }
}
