package xyz.revampes.Revamp.utility;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.Packet;
import xyz.revampes.Revamp.events.impl.SendPacketEvent;

import java.util.concurrent.ConcurrentLinkedQueue;

public class BlinkUtils {
    private static ConcurrentLinkedQueue<Packet> blinkedPackets = new ConcurrentLinkedQueue();

    @EventHandler
    public void onSendPacket(SendPacketEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }
    }
}
