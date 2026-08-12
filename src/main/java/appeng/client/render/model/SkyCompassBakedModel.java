/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.client.render.model;


import appeng.block.misc.BlockSkyCompass;
import appeng.hooks.CompassManager;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import net.minecraftforge.common.property.IExtendedBlockState;

import javax.annotation.Nullable;
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix4f;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * This baked model combines the quads of a compass base and the quads of a compass pointer, which will be rotated
 * around the Y-axis to get the compass to point in the right direction.
 */
public class SkyCompassBakedModel implements IBakedModel {

    // The square distance to be within range of a meteor
    private static final int MIN_DIST_SQ = 2;

    private final IBakedModel base;

    private final IBakedModel pointer;

    private float fallbackRotation = 0;

    public SkyCompassBakedModel(IBakedModel base, IBakedModel pointer) {
        this.base = base;
        this.pointer = pointer;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        float rotation = 0;
        // Get rotation from the special block state
        if (state instanceof IExtendedBlockState) {
            Float rotationOpt = ((IExtendedBlockState) state).getValue(BlockSkyCompass.ROTATION);
            if (rotationOpt != null) {
                rotation = rotationOpt;
            }
        } else if (state == null) {
            // This is used to render a compass pointing in a specific direction when being held in hand
            rotation = this.fallbackRotation;
        }

        // Pre-compute the quad count to avoid list resizes
        List<BakedQuad> quads = new ArrayList<>();

        quads.addAll(this.base.getQuads(state, side, rand));

        // We'll add the pointer as "sideless"
        if (side == null) {
            // Set up the rotation around the Y-axis for the pointer
            Matrix4f matrix = new Matrix4f();
            matrix.setIdentity();
            matrix.setRotation(new AxisAngle4f(0, 1, 0, rotation));

            MatrixVertexTransformer transformer = new MatrixVertexTransformer(matrix);
            for (BakedQuad bakedQuad : this.pointer.getQuads(state, side, rand)) {
                UnpackedBakedQuad.Builder builder = new UnpackedBakedQuad.Builder(bakedQuad.getFormat());

                transformer.setParent(builder);
                transformer.setVertexFormat(builder.getVertexFormat());
                bakedQuad.pipe(transformer);
                builder.setQuadOrientation(null); // After rotation, facing a specific side cannot be guaranteed
                // anymore
                BakedQuad q = builder.build();
                quads.add(q);
            }
        }

        return quads;
    }

    @Override
    public boolean isAmbientOcclusion() {
        return this.base.isAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean isBuiltInRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return this.base.getParticleTexture();
    }

    @Override
    public ItemCameraTransforms getItemCameraTransforms() {
        return this.base.getItemCameraTransforms();
    }

    @Override
    public ItemOverrideList getOverrides() {
        /*
         * This handles setting the rotation of the compass when being held in hand. If it's not held in hand, it'll
         * animate using the
         * spinning animation.
         */
        return new ItemOverrideList(Collections.emptyList()) {

            @Override
            public IBakedModel handleItemState(IBakedModel originalModel, ItemStack stack, World world, EntityLivingBase entity) {
                if (world != null && entity instanceof EntityPlayerSP) {
                    EntityPlayer player = (EntityPlayer) entity;

                    float offRads = (float) (player.rotationYaw / 180.0f * (float) Math.PI + Math.PI);

                    SkyCompassBakedModel.this.fallbackRotation = offRads + getAnimatedRotation(player.getPosition(), true);
                } else {
                    SkyCompassBakedModel.this.fallbackRotation = getAnimatedRotation(null, false);
                }

                return originalModel;
            }
        };
    }

    /**
     * Gets the effective, animated rotation for the compass given the current position of the compass.
     */
    public static float getAnimatedRotation(@Nullable BlockPos pos, boolean prefetch) {

        // Only query for a meteor position if we know our own position
        if (pos != null) {
            var ourChunkPos = new ChunkPos(pos);
            var closestMeteorite = CompassManager.INSTANCE.getClosestMeteorite(ourChunkPos, prefetch);

            // No close meteorite was found -> spin slowly
            if (closestMeteorite == null) {
                long timeMillis = System.currentTimeMillis();
                // .5 seconds per full rotation
                timeMillis %= 500;
                return timeMillis / 500.f * (float) Math.PI * 2;
            } else {
                var dx = pos.getX() - closestMeteorite.getX();
                var dz = pos.getZ() - closestMeteorite.getZ();
                var distanceSq = dx * dx + dz * dz;
                if (distanceSq > MIN_DIST_SQ) {
                    var x = closestMeteorite.getX();
                    var z = closestMeteorite.getZ();
                    return (float) rad(pos.getX(), pos.getZ(), x, z);
                }
            }
        }

        long timeMillis = System.currentTimeMillis();
        // 3 seconds per full rotation
        timeMillis %= 3000;
        return timeMillis / 3000.f * (float) Math.PI * 2;
    }

    private static double rad(int ax, int az, int bx, int bz) {
        var up = bz - az;
        var side = bx - ax;

        return Math.atan2(-up, side) - Math.PI / 2.0;
    }
}
