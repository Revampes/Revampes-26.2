package xyz.whatsyouss.frosty.mixin.accessor;

import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.ResourceLoadStateTracker;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.resources.SkinManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.net.Proxy;
import java.util.concurrent.CompletableFuture;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    @Accessor("missTime")
    int frosty$getMissTime();

    @Accessor("missTime")
    void frosty$setMissTime(int attackCooldown);

    @Invoker("startAttack")
    boolean frosty$leftClick();

    @Accessor("rightClickDelay")
    int frosty$getRightClickDelay();

    @Accessor("rightClickDelay")
    void frosty$setRightClickDelay(int itemUseCooldown);
}