package net.scpfoundation.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;
import net.scpfoundation.ScpFoundation;
import net.scpfoundation.client.model.ScpBeastModel;
import net.scpfoundation.client.model.ScpBipedModel;
import net.scpfoundation.client.model.ScpModelData;
import net.scpfoundation.client.render.BeastRenderer;
import net.scpfoundation.client.render.ScpBipedRenderer;
import net.scpfoundation.entity.Scp0492Entity;
import net.scpfoundation.entity.Scp049Entity;
import net.scpfoundation.entity.Scp096Entity;
import net.scpfoundation.entity.Scp1048Entity;
import net.scpfoundation.entity.Scp106Entity;
import net.scpfoundation.entity.Scp131Entity;
import net.scpfoundation.entity.Scp173Entity;
import net.scpfoundation.entity.Scp529Entity;
import net.scpfoundation.entity.Scp682Entity;
import net.scpfoundation.entity.Scp939Entity;
import net.scpfoundation.entity.Scp999Entity;
import net.scpfoundation.registry.ModBlocks;
import net.scpfoundation.registry.ModEntities;

@Environment(EnvType.CLIENT)
public class ScpFoundationClient implements ClientModInitializer {
    public static final EntityModelLayer SCP_173 = layer("scp_173");
    public static final EntityModelLayer SCP_096 = layer("scp_096");
    public static final EntityModelLayer SCP_049 = layer("scp_049");
    public static final EntityModelLayer SCP_049_2 = layer("scp_049_2");
    public static final EntityModelLayer SCP_106 = layer("scp_106");
    public static final EntityModelLayer SCP_682 = layer("scp_682");
    public static final EntityModelLayer SCP_939 = layer("scp_939");
    public static final EntityModelLayer SCP_999 = layer("scp_999");
    public static final EntityModelLayer SCP_131 = layer("scp_131");
    public static final EntityModelLayer SCP_529 = layer("scp_529");
    public static final EntityModelLayer SCP_1048 = layer("scp_1048");

    private static EntityModelLayer layer(String name) {
        return new EntityModelLayer(ScpFoundation.id(name), "main");
    }

    private static Identifier tex(String name) {
        return ScpFoundation.id("textures/entity/" + name + ".png");
    }

    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(SCP_173, ScpBipedModel::plain);
        EntityModelLayerRegistry.registerModelLayer(SCP_096, ScpBipedModel::plain);
        EntityModelLayerRegistry.registerModelLayer(SCP_049, ScpBipedModel::withBeak);
        EntityModelLayerRegistry.registerModelLayer(SCP_049_2, ScpBipedModel::plain);
        EntityModelLayerRegistry.registerModelLayer(SCP_106, ScpBipedModel::plain);
        EntityModelLayerRegistry.registerModelLayer(SCP_682, ScpModelData::scp682);
        EntityModelLayerRegistry.registerModelLayer(SCP_939, ScpModelData::scp939);
        EntityModelLayerRegistry.registerModelLayer(SCP_999, ScpModelData::scp999);
        EntityModelLayerRegistry.registerModelLayer(SCP_131, ScpModelData::scp131);
        EntityModelLayerRegistry.registerModelLayer(SCP_529, ScpModelData::scp529);
        EntityModelLayerRegistry.registerModelLayer(SCP_1048, ScpModelData::scp1048);

        EntityRendererRegistry.register(ModEntities.SCP_173, ctx -> new ScpBipedRenderer<Scp173Entity>(ctx,
                new ScpBipedModel<Scp173Entity>(ctx.getPart(SCP_173), ScpBipedModel.Pose.STATUE), tex("scp_173"), 0.5f, 1.05f, 1.15f));
        EntityRendererRegistry.register(ModEntities.SCP_096, ctx -> new ScpBipedRenderer<Scp096Entity>(ctx,
                new ScpBipedModel<Scp096Entity>(ctx.getPart(SCP_096), ScpBipedModel.Pose.SHY), tex("scp_096"), 0.4f, 0.85f, 1.32f));
        EntityRendererRegistry.register(ModEntities.SCP_049, ctx -> new ScpBipedRenderer<Scp049Entity>(ctx,
                new ScpBipedModel<Scp049Entity>(ctx.getPart(SCP_049), ScpBipedModel.Pose.REACH), tex("scp_049"), 0.5f, 1.0f, 1.0f));
        EntityRendererRegistry.register(ModEntities.SCP_049_2, ctx -> new ScpBipedRenderer<Scp0492Entity>(ctx,
                new ScpBipedModel<Scp0492Entity>(ctx.getPart(SCP_049_2), ScpBipedModel.Pose.SHAMBLE), tex("scp_049_2"), 0.5f, 1.0f, 1.0f));
        EntityRendererRegistry.register(ModEntities.SCP_106, ctx -> new ScpBipedRenderer<Scp106Entity>(ctx,
                new ScpBipedModel<Scp106Entity>(ctx.getPart(SCP_106), ScpBipedModel.Pose.REACH), tex("scp_106"), 0.5f, 1.0f, 1.08f));

        EntityRendererRegistry.register(ModEntities.SCP_682, ctx -> new BeastRenderer<Scp682Entity>(ctx,
                new ScpBeastModel<Scp682Entity>(ctx.getPart(SCP_682), ScpModelData.SCP_682_PARTS), 1.0f, 1.45f, e -> tex("scp_682")));
        EntityRendererRegistry.register(ModEntities.SCP_939, ctx -> new BeastRenderer<Scp939Entity>(ctx,
                new ScpBeastModel<Scp939Entity>(ctx.getPart(SCP_939), ScpModelData.SCP_939_PARTS), 0.7f, 1.3f, e -> tex("scp_939")));
        EntityRendererRegistry.register(ModEntities.SCP_999, ctx -> new BeastRenderer<Scp999Entity>(ctx,
                new ScpBeastModel<Scp999Entity>(ctx.getPart(SCP_999), ScpModelData.SCP_999_PARTS), 0.5f, 1.0f, e -> tex("scp_999")));
        EntityRendererRegistry.register(ModEntities.SCP_131, ctx -> new BeastRenderer<Scp131Entity>(ctx,
                new ScpBeastModel<Scp131Entity>(ctx.getPart(SCP_131), ScpModelData.SCP_131_PARTS), 0.3f, 1.0f,
                (Scp131Entity e) -> tex(e.getVariant() == 0 ? "scp_131_a" : "scp_131_b")));
        EntityRendererRegistry.register(ModEntities.SCP_529, ctx -> new BeastRenderer<Scp529Entity>(ctx,
                new ScpBeastModel<Scp529Entity>(ctx.getPart(SCP_529), ScpModelData.SCP_529_PARTS), 0.3f, 1.0f, e -> tex("scp_529")));
        EntityRendererRegistry.register(ModEntities.SCP_1048, ctx -> new BeastRenderer<Scp1048Entity>(ctx,
                new ScpBeastModel<Scp1048Entity>(ctx.getPart(SCP_1048), ScpModelData.SCP_1048_PARTS), 0.3f, 1.0f, e -> tex("scp_1048")));
        EntityRendererRegistry.register(ModEntities.SCP_018, FlyingItemEntityRenderer::new);

        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.CONTAINMENT_GLASS, RenderLayer.getTranslucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.SCP_330, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.SCP_458, RenderLayer.getCutout());
    }
}
