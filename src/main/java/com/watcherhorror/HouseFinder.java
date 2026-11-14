package com.watcherhorror;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.AbstractGlassBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** Ищет рядом с игроком (внутри дома) окна и двери, снаружи которых может встать Наблюдатель. */
public class HouseFinder {
    public record Spot(BlockPos opening, BlockPos stand) {}

    public static boolean isGlass(BlockState s) {
        Block b = s.getBlock();
        return b instanceof AbstractGlassBlock || b instanceof StainedGlassPaneBlock || b == Blocks.GLASS_PANE;
    }

    public static boolean isDoor(BlockState s) {
        return s.getBlock() instanceof DoorBlock && s.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER;
    }

    private static boolean free(ServerLevel l, BlockPos p) {
        return l.getBlockState(p).getCollisionShape(l, p).isEmpty();
    }

    public static Spot find(ServerLevel level, ServerPlayer p) {
        BlockPos c = p.blockPosition();
        if (level.canSeeSky(c.above())) return null; // игрок не под крышей

        List<Spot> list = new ArrayList<>();
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                for (int dy = -1; dy <= 3; dy++) {
                    BlockPos pos = c.offset(dx, dy, dz);
                    if (!level.hasChunkAt(pos)) continue;
                    BlockState s = level.getBlockState(pos);
                    boolean door = isDoor(s);
                    if (!door && !isGlass(s)) continue;

                    for (Direction d : Direction.Plane.HORIZONTAL) {
                        BlockPos inside = pos.relative(d.getOpposite());
                        if (!free(level, inside) || level.canSeeSky(inside)) continue;

                        BlockPos far = pos.relative(d, 2);
                        BlockPos stand = null;
                        for (int yy = pos.getY(); yy >= pos.getY() - 2; yy--) {
                            BlockPos cell = new BlockPos(far.getX(), yy, far.getZ());
                            BlockPos below = cell.below();
                            if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
                                    && free(level, cell) && free(level, cell.above())) {
                                stand = cell;
                                break;
                            }
                        }
                        if (stand == null) continue;
                        if (door && stand.getY() != pos.getY()) continue;

                        BlockPos near = new BlockPos(pos.relative(d).getX(), stand.getY(), pos.relative(d).getZ());
                        if (!free(level, near) || !free(level, near.above())) continue;
                        if (!level.canSeeSky(near.above())) continue; // снаружи дома
                        list.add(new Spot(pos, stand));
                    }
                }
            }
        }
        return list.isEmpty() ? null : list.get(level.getRandom().nextInt(list.size()));
    }
}
