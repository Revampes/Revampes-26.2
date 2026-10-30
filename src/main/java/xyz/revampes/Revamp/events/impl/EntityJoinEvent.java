package xyz.revampes.Revamp.events.impl;

import net.minecraft.world.entity.Entity;

public class EntityJoinEvent {
    public Entity entity;

    public EntityJoinEvent(Entity entity) {
        this.entity = entity;
    }
}
