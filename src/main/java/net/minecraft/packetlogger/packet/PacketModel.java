package net.minecraft.packetlogger.packet;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import javafx.beans.property.*;
import net.minecraft.packetlogger.info.PacketInfo;
import net.minecraft.packetlogger.info.StateInfo;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;

public class PacketModel {
    private ObjectProperty<LocalDateTime> timestamp;
    private StringProperty address;
    private ObjectProperty<PacketDirection> direction;
    private StringProperty state;
    private IntegerProperty opcode;
    private StringProperty name;

    private final ChannelHandlerContext ctx;
    private final ByteBuf content;
    private final Object packet;

    private final StateInfo stateInfo;
    private final PacketInfo packetInfo;

    public PacketModel(LocalDateTime timestamp, ChannelHandlerContext ctx, PacketDirection direction, StateInfo state, int opcode, ByteBuf content, Object packet) {
        this.ctx = ctx;
        this.content = content;
        this.packet = packet;

        this.stateInfo = state;
        this.packetInfo = state.getPacketInfo(direction, opcode);
        String name = packetInfo.getName();

        InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();

        this.timestamp = new SimpleObjectProperty<>(timestamp);
        this.address = new SimpleStringProperty(address.getHostString() + ":" + address.getPort());
        this.direction = new SimpleObjectProperty<>(direction);
        this.state = new SimpleStringProperty(stateInfo.getName());
        this.opcode = new SimpleIntegerProperty(opcode);
        this.name = new SimpleStringProperty(name);
    }

    public ObjectProperty<LocalDateTime> timestampProperty() {
        return timestamp;
    }

    public StringProperty addressProperty() {
        return address;
    }

    public ObjectProperty<PacketDirection> directionProperty() {
        return direction;
    }

    public StringProperty stateProperty() {
        return state;
    }

    public IntegerProperty opcodeProperty() {
        return opcode;
    }

    public StringProperty nameProperty() {
        return name;
    }

    public StateInfo getStateInfo() {
        return stateInfo;
    }

    public PacketInfo getPacketInfo() {
        return packetInfo;
    }

    public PacketDirection getDirection() {
        return direction.get();
    }
}
