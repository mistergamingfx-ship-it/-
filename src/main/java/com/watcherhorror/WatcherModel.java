package com.watcherhorror;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.util.Mth;

/** Гуманоид с подвижной нижней челюстью (пасть раскрывается). */
public class WatcherModel extends HumanoidModel<WatcherEntity> {
    private final ModelPart jaw;

    public WatcherModel(ModelPart root) {
        super(root);
        this.jaw = root.getChild("head").getChild("jaw");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        mesh.getRoot().getChild("head").addOrReplaceChild("jaw",
                CubeListBuilder.create().texOffs(0, 32).addBox(-4.0F, 0.0F, -7.0F, 8.0F, 3.0F, 8.0F),
                PartPose.offset(0.0F, -1.0F, 3.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(WatcherEntity e, float limbSwing, float limbSwingAmount,
                          float age, float netHeadYaw, float headPitch) {
        super.setupAnim(e, limbSwing, limbSwingAmount, age, netHeadYaw, headPitch);

        int state = e.getState();
        float t = e.getStateTicks();

        // руки по умолчанию висят
        this.rightArm.xRot = 0.04F; this.leftArm.xRot = 0.04F;
        this.rightArm.zRot = 0.06F; this.leftArm.zRot = -0.06F;
        this.rightLeg.xRot = 0.0F;  this.leftLeg.xRot = 0.0F;
        float open = 0.0F;

        if (state == WatcherEntity.PEEK) {
            this.head.zRot = 0.18F; // жутко склонённая голова
        } else if (state == WatcherEntity.SMASH) {
            // удар кулаками по стеклу
            this.rightArm.xRot = -2.6F + Mth.cos(t * 0.9F) * 0.9F;
            this.leftArm.xRot = -2.2F + Mth.cos(t * 0.9F + 1.5F) * 0.9F;
            this.rightArm.zRot = 0.0F; this.leftArm.zRot = 0.0F;
            open = 0.3F;
        } else if (state == WatcherEntity.GRAB) {
            // тянется руками вперёд и раскрывает пасть
            this.rightArm.xRot = -1.45F + Mth.sin(age * 2.0F) * 0.05F;
            this.leftArm.xRot = -1.45F + Mth.sin(age * 2.0F + 1.0F) * 0.05F;
            this.rightArm.yRot = -0.2F; this.leftArm.yRot = 0.2F;
            this.rightArm.zRot = 0.0F;  this.leftArm.zRot = 0.0F;
            this.head.xRot = -0.35F;    // голова запрокинута
            this.head.zRot = Mth.sin(age * 3.0F) * 0.04F;
            open = Mth.clamp(t / 25.0F, 0.0F, 1.0F) * 1.25F;
        }
        this.jaw.xRot = open;
        this.hat.copyFrom(this.head);
    }
}
