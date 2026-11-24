package com.mandelahorror.client;

import com.mandelahorror.entity.MimicEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * Renders the mimic exactly like a player.
 * Skin = a random default skin (Steve, Alex, Ari, Kai, Noor, Sunny, Zuri, Efe, Makena)
 * or the real skin of the player it is copying. Picks wide/slim arms to match.
 */
public class MimicRenderer extends EntityRenderer<MimicEntity> {
    private final Inner wide;
    private final Inner slim;

    public MimicRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.5F;
        this.wide = new Inner(ctx, false);
        this.slim = new Inner(ctx, true);
    }

    private Inner pick(MimicEntity mimic) {
        return isSlim(mimic) ? this.slim : this.wide;
    }

    @Override
    public void render(MimicEntity mimic, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pick(mimic).render(mimic, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(MimicEntity mimic) {
        return skin(mimic);
    }

    // ------------------------------------------------------------------ skins

    static ResourceLocation skin(MimicEntity mimic) {
        if (ClientHorror.isSkinGlitching()) {
            return DefaultPlayerSkin.getDefaultSkin(new UUID(mimic.getId() + ClientHorror.ticks / 6, 0x5EEDL));
        }
        AbstractClientPlayer copy = copyTarget(mimic);
        if (copy != null) return copy.getSkinTextureLocation();
        return DefaultPlayerSkin.getDefaultSkin(mimic.getSkinSeed());
    }

    static boolean isSlim(MimicEntity mimic) {
        if (ClientHorror.isSkinGlitching()) {
            return "slim".equals(DefaultPlayerSkin.getSkinModelName(new UUID(mimic.getId() + ClientHorror.ticks / 6, 0x5EEDL)));
        }
        AbstractClientPlayer copy = copyTarget(mimic);
        if (copy != null) return "slim".equals(copy.getModelName());
        return "slim".equals(DefaultPlayerSkin.getSkinModelName(mimic.getSkinSeed()));
    }

    private static AbstractClientPlayer copyTarget(MimicEntity mimic) {
        UUID id = mimic.getCopyTarget();
        if (id == null || Minecraft.getInstance().level == null) return null;
        Player p = Minecraft.getInstance().level.getPlayerByUUID(id);
        return p instanceof AbstractClientPlayer acp ? acp : null;
    }

    /** The real player-shaped renderer (one for wide arms, one for slim arms). */
    static class Inner extends HumanoidMobRenderer<MimicEntity, PlayerModel<MimicEntity>> {
        Inner(EntityRendererProvider.Context ctx, boolean slim) {
            super(ctx, new PlayerModel<>(ctx.bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim), 0.5F);
            this.addLayer(new HumanoidArmorLayer<>(this,
                    new HumanoidModel<>(ctx.bakeLayer(slim ? ModelLayers.PLAYER_SLIM_INNER_ARMOR : ModelLayers.PLAYER_INNER_ARMOR)),
                    new HumanoidModel<>(ctx.bakeLayer(slim ? ModelLayers.PLAYER_SLIM_OUTER_ARMOR : ModelLayers.PLAYER_OUTER_ARMOR)),
                    ctx.getModelManager()));
        }

        @Override
        public ResourceLocation getTextureLocation(MimicEntity mimic) {
            return skin(mimic);
        }
    }
}
