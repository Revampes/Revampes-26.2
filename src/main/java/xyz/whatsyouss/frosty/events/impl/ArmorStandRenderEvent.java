package xyz.whatsyouss.frosty.events.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import xyz.whatsyouss.frosty.events.Cancellable;

/**
 * Ported from IQAddons (net.iqaddons.mod.events.impl.ArmorStandRenderEvent).
 * Posted from {@code ArmorStandRenderer#submit}; cancelling hides the armor stand
 * (used to remove Kuudra build-progress name tags).
 */
public class ArmorStandRenderEvent extends Cancellable {

    private final ArmorStandRenderState renderState;
    private final PoseStack matrices;
    private final SubmitNodeCollector queue;
    private final CameraRenderState camera;

    public ArmorStandRenderEvent(ArmorStandRenderState renderState, PoseStack matrices,
                                 SubmitNodeCollector queue, CameraRenderState camera) {
        this.renderState = renderState;
        this.matrices = matrices;
        this.queue = queue;
        this.camera = camera;
    }

    public ArmorStandRenderState getRenderState() {
        return renderState;
    }

    public PoseStack getMatrices() {
        return matrices;
    }

    public SubmitNodeCollector getQueue() {
        return queue;
    }

    public CameraRenderState getCamera() {
        return camera;
    }
}
