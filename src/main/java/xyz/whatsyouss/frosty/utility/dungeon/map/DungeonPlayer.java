package xyz.whatsyouss.frosty.utility.dungeon.map;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.Player;
import xyz.whatsyouss.frosty.utility.dungeon.map.DungeonPositions.PlayerComponent;

public class DungeonPlayer {

    public final String name;
    public PlayerInfo profileInfo;
    public DungeonClass role;
    public int classLevel;
    public boolean isDead;
    public boolean isDisconnected;

    public Player entity;
    public PlayerComponent position;
    public double updateTime;
    public PlayerComponent lastPosition;
    public double lastUpdateTime;

    public DungeonPlayer(String name, PlayerInfo profileInfo, DungeonClass role, int classLevel, boolean isDead) {
        this.name = name;
        this.profileInfo = profileInfo;
        this.role = role;
        this.classLevel = classLevel;
        this.isDead = isDead;
    }

    public void tick() {
        if (entity != null && (entity.isDeadOrDying() || entity.isRemoved())) entity = null;
        if (entity == null) return;

        updatePosition(PlayerComponent.fromWorld(
                entity.getX(),
                entity.getZ(),
                -Math.toRadians(entity.getYRot() + 90)));
    }

    public void updatePosition(PlayerComponent pos) {
        double time = System.nanoTime() * 1.0e-6;
        if (position == null) {
            position = pos;
            updateTime = time;
            return;
        }
        lastPosition = position;
        lastUpdateTime = updateTime;
        position = pos;
        updateTime = time;
    }

    public PlayerComponent getLerpedPosition() {
        PlayerComponent pos = position;
        if (pos == null) return null;
        PlayerComponent lastPos = lastPosition;
        if (lastPos == null) return pos;
        double time = System.nanoTime() * 1.0e-6;
        double frame = (time - updateTime) / (updateTime - lastUpdateTime);
        if (Double.isNaN(frame) || Double.isInfinite(frame)) frame = 1.0;
        // Never extrapolate past the newest sample, otherwise markers run ahead of the player
        // while the next position update is still in flight.
        frame = MapMath.clamp(frame, 0.0, 1.0);

        return new PlayerComponent(
                lerp(frame, lastPos.x(), pos.x()),
                lerp(frame, lastPos.z(), pos.z()),
                lerpAngle(frame, lastPos.r(), pos.r()));
    }

    private static double lerp(double delta, double start, double end) {
        return start + delta * (end - start);
    }

    private static double lerpAngle(double delta, double start, double end) {
        double diff = end - start;
        while (diff > Math.PI) diff -= Math.PI * 2;
        while (diff < -Math.PI) diff += Math.PI * 2;
        return start + delta * diff;
    }
}
