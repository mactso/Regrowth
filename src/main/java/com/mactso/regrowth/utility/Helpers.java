package com.mactso.regrowth.utility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class Helpers {
	public static int helperCountBlocksBB(Block searchBlock, int maxCount, ServerLevel serverLevel, BlockPos bPos,
			int boxSize) {
		return helperCountBlocksBB(searchBlock, maxCount, serverLevel, bPos, boxSize, boxSize); // "square" box subcase
	}

	public static int helperCountBlocksBB(Block searchBlock, int maxCount, ServerLevel serverLevel, BlockPos bPos,
			int boxSize, int ySize) {
		int count = 0;
		int minX = bPos.getX() - boxSize;
		int maxX = bPos.getX() + boxSize;
		int minZ = bPos.getZ() - boxSize;
		int maxZ = bPos.getZ() + boxSize;
		int minY = bPos.getY() - ySize;
		int maxY = bPos.getY() + ySize;

		MutableBlockPos mPos = new MutableBlockPos();
		for (int dx = minX; dx <= maxX; dx++) {
			for (int dz = minZ; dz <= maxZ; dz++) {
				for (int dy = minY; dy <= maxY; dy++) {
					mPos.set(dx, dy, dz);
					if (serverLevel.getBlockState(mPos).getBlock() == searchBlock) {
						if (++count >= maxCount)
							return count;
					}
				}
			}
		}

		Utility.debugMsg(2, bPos, Utility.getResourceLocationString(serverLevel, searchBlock) + " Sparse count:" + count
				+ " countBlockBB ");

		return count;
	}

	public static int helperCountBlocksBB(Class<? extends Block> searchBlock, int maxCount, Level w, BlockPos bPos,
			int boxSize) {
		return helperCountBlocksBB(searchBlock, maxCount, w, bPos, boxSize, 0);
	}

	public static int helperCountBlocksBB(Class<? extends Block> searchBlock, int maxCount, Level w, BlockPos bPos,
			int boxSize, int ySize) {
		int count = 0;
		int minX = bPos.getX() - boxSize;
		int maxX = bPos.getX() + boxSize;
		int minZ = bPos.getZ() - boxSize;
		int maxZ = bPos.getZ() + boxSize;
		int minY = bPos.getY() - ySize;
		int maxY = bPos.getY() + ySize;

		MutableBlockPos mPos = new MutableBlockPos();

		for (int dx = minX; dx <= maxX; dx++) {
			for (int dz = minZ; dz <= maxZ; dz++) {
				for (int dy = minY; dy <= maxY; dy++) {
					mPos.set(dx, dy, dz);
					if (searchBlock.isInstance(w.getBlockState(mPos).getBlock())) {
						if (++count >= maxCount) {
							return count;
						}
					}
				}
			}
		}

		Utility.debugMsg(2, bPos, searchBlock.getSimpleName() + " Sparse count:" + count + " countBlockBB ");

		return count;
	}

	// this routine returns a count of the searchBlock immediately orthogonal to
	// BlockPos, exiting if a max count is exceeded.
	public static int helperCountBlocksOrthogonalBB(Block searchBlock, int maxCount, Level w, BlockPos bPos,
			int boundY) {
		return helperCountBlocksOrthogonalBB(searchBlock, maxCount, w, bPos, 0 - boundY, 0 + boundY);
	}

	public static int helperCountBlocksOrthogonalBB(Block searchBlock, int maxCount, Level w, BlockPos bPos,
			int lowerBoundY, int upperBoundY) {
		int count = 0;
		for (int j = lowerBoundY; j <= upperBoundY; j++) {
			if (w.getBlockState(bPos.above(j).east()).getBlock() == searchBlock)
				count++;
			if (w.getBlockState(bPos.above(j).west()).getBlock() == searchBlock)
				count++;
			if (w.getBlockState(bPos.above(j).north()).getBlock() == searchBlock)
				count++;
			if (w.getBlockState(bPos.above(j).south()).getBlock() == searchBlock)
				count++;
			if (count >= maxCount)
				return count;
		}

		return count;

	}
}
