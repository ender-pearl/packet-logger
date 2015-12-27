package net.minecraft.packetlogger.info;

import lombok.Data;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@Data
public class ProtocolInfo {
    private final int version;

    @Setter(PRIVATE)
    private List<StateInfo> states = new ArrayList<>();

    public void addState(StateInfo state) {
        states.add(state);
    }

    public StateInfo getStateInfo(int id) {
        return states.stream()
                    .filter(state -> state.getId() == id)
                    .findAny().get();
    }
}
