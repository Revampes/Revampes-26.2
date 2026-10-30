package xyz.revampes.Revamp.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.revampes.Revamp.Frosty;
import xyz.revampes.Revamp.events.impl.EntityJoinEvent;
import xyz.revampes.Revamp.events.impl.ParticleEvent;
import xyz.revampes.Revamp.modules.ModuleManager;

import static xyz.revampes.Revamp.Frosty.mc;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {
    @Inject(method = "addEntity", at = @At("HEAD"))
    private void onEntityJoin(Entity entity, CallbackInfo ci) {
        Frosty.EVENT_BUS.post(new EntityJoinEvent(entity));
    }

    @Inject(method = "addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V", at = @At("HEAD"), cancellable = true)
    private void onAddParticle(ParticleOptions type, double x, double y, double z,
                               double xSpeed, double ySpeed, double zSpeed, CallbackInfo ci) {
        if (ModuleManager.fireVeilOverlay == null || !ModuleManager.fireVeilOverlay.isEnabled()) return;

        if (Frosty.EVENT_BUS.post(new ParticleEvent(type, x, y, z)).isCancelled()) {
            ci.cancel();
        }
    }
}