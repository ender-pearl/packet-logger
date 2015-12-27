package net.minecraft.packetlogger.hook;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import net.minecraft.packetlogger.gui.GuiApplication;
import net.minecraft.packetlogger.info.ProtocolInfo;
import net.minecraft.packetlogger.info.StateInfo;
import net.minecraft.packetlogger.packet.PacketDirection;
import net.minecraft.packetlogger.packet.PacketModel;
import net.minecraft.packetlogger.util.ByteBufUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static net.minecraft.packetlogger.packet.PacketDirection.IN;
import static net.minecraft.packetlogger.packet.PacketDirection.OUT;

public class Hooks {
    public static AttributeKey attributeProtocol;
    public static ProtocolInfo protocolInfo;

    public static boolean isReady() {
        return attributeProtocol != null && protocolInfo != null;
    }

    public static void onEncode(Object codec, Object ctxObject, Object packet, Object bufferObject) {
        if (!isReady()) {
            return;
        }

        try {
            ByteBuf buffer = (ByteBuf) bufferObject;
            ChannelHandlerContext ctx = (ChannelHandlerContext) ctxObject;

            GuiApplication.packets.add(createPacketModel(ctx, OUT, packet, buffer));
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    public static void onDecode(Object codec, Object ctxObject, Object bufferObject, Object packets) {
        if (!isReady()) {
            return;
        }

        try {
            ByteBuf buffer = (ByteBuf) bufferObject;
            ChannelHandlerContext ctx = (ChannelHandlerContext) ctxObject;
            Object packet = ((List) packets).get(0);

            GuiApplication.packets.add(createPacketModel(ctx, IN, packet, buffer));
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    private static PacketModel createPacketModel(ChannelHandlerContext ctx, PacketDirection direction, Object packet, ByteBuf buffer) throws IOException {
        int oldReaderIndex = buffer.readerIndex();

        buffer.resetReaderIndex();
        int opcode = ByteBufUtils.readVarInt(buffer);

        ByteBuf content = Unpooled.buffer();
        content.writeBytes(buffer, buffer.readableBytes());

        buffer.readerIndex(oldReaderIndex);

        int stateId = getStateId(ctx.channel().attr(attributeProtocol).get());

        StateInfo state = protocolInfo.getStateInfo(stateId);

        return new PacketModel(LocalDateTime.now(), ctx, direction, state, opcode, content, packet);
    }

    private static int getStateId(Object state) {
        Field idField = Arrays.asList(state.getClass().getSuperclass().getDeclaredFields())
                .stream()
                .filter((field) -> !Modifier.isStatic(field.getModifiers()))
                .findFirst()
                .get();

        idField.setAccessible(true);
        try {
            return (int) idField.get(state);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return -1;
    }
}
