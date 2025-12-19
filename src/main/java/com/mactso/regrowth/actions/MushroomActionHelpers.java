package com.mactso.regrowth.actions;

import com.mactso.regrowth.config.MyConfig;
import com.mactso.regrowth.utility.Utility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.phys.Vec3;

/**
 * Provides helper methods for actions that modify the world or entities.
 * Focuses on huge mushroom-specific operations and includes related tests
 * such as temperature, proximity, and density checks for mushroom growth.
 */

public class MushroomActionHelpers {

	static boolean tryDustMushroomTop(ActionContext rgCtx) {
		ServerLevel serverLevel = rgCtx.serverLevel();
		BlockPos pos = rgCtx.livingEntity().blockPosition();
		Block groundBlock = rgCtx.groundBlock();
	
		// Count nearby small mushrooms
		int smallMushroomCount = ActionUtilities.countBlocksInBox(MushroomBlock.class, 4, serverLevel, pos, 4, 1);
		if (smallMushroomCount > 3)
			return false;
	
		// Place the small mushroom matching the type of the top block
		if (groundBlock == Blocks.RED_MUSHROOM_BLOCK) {
			serverLevel.setBlockAndUpdate(pos, Blocks.RED_MUSHROOM.defaultBlockState());
			return true;
		}
	
		if (groundBlock == Blocks.BROWN_MUSHROOM_BLOCK) {
			serverLevel.setBlockAndUpdate(pos, Blocks.BROWN_MUSHROOM.defaultBlockState());
			return true;
		}
	
		return false;
	}

	static void lightRedMushroomStem(ServerLevel serverLevel, BlockPos pos, Block mushroomBlock) {
		if (mushroomBlock != Blocks.RED_MUSHROOM)
			return;
	
		for (int y = 9; y > 3; y--) {
			BlockPos targetPos = pos.above(y);
			Block b = serverLevel.getBlockState(targetPos).getBlock();
			if (b == Blocks.MUSHROOM_STEM) {
				serverLevel.setBlockAndUpdate(targetPos, Blocks.SHROOMLIGHT.defaultBlockState());
				break;
			}
		}
	}

	static void tryGrowHugeMushroom(ActionContext rgCtx, double growthFilterValue) {
		LivingEntity le = rgCtx.livingEntity();
		ServerLevel serverLevel = rgCtx.serverLevel();
		BlockPos pos = le.blockPosition();
	
		// Adjust entity motion slightly toward block center
		double vx = le.position().x() - (pos.getX() + 0.5);
		double vz = le.position().z() - (pos.getZ() + 0.5);
		Vec3 vM = new Vec3(vx, 0, vz).normalize().scale(1.0).add(0, 0.5, 0);
		le.setDeltaMovement(le.getDeltaMovement().add(vM));
	
		// Occasionally place mycelium below
		if (growthFilterValue > 0.9) {
			serverLevel.setBlockAndUpdate(pos.below(), Blocks.MYCELIUM.defaultBlockState());
		}
	
		// Decide mushroom type
		Block mushroomBlock = (serverLevel.random.nextDouble() * 100.0 > 75.0) ? Blocks.RED_MUSHROOM
				: Blocks.BROWN_MUSHROOM;
		serverLevel.setBlockAndUpdate(pos, mushroomBlock.defaultBlockState());
	
		// Grow the mushroom
		try {
			((MushroomBlock) mushroomBlock).growMushroom(serverLevel, pos, mushroomBlock.defaultBlockState(),
					serverLevel.random);
		} catch (Exception ignored) {
			// technically an "impossible" error but it's happened so this should
			// bulletproof it.
		}
	
		// Light red mushroom stem if applicable
		if (mushroomBlock == Blocks.RED_MUSHROOM)
			lightRedMushroomStem(serverLevel, pos, mushroomBlock);
	}

	static boolean isValidMushroomPosition(ActionContext rgCtx, double growthFilterValue) {
		LivingEntity le = rgCtx.livingEntity();
		ServerLevel serverLevel = rgCtx.serverLevel();
		BlockPos pos = le.blockPosition();
		String key = rgCtx.key();
	
		// Must not be exposed to sky
		if (serverLevel.canSeeSky(pos))
			return false;
	
		if (!rgCtx.groundBlockState().is(BlockTags.BASE_STONE_OVERWORLD)
				&& !(rgCtx.groundBlock() instanceof HugeMushroomBlock)) {
			return false;
		}
	
		// Fertility check
		if (growthFilterValue < 0.75) {
			Utility.debugMsg(1, pos, key + " Mushroom fertility (" + growthFilterValue + ") non-growing spot.");
			return false;
		}
	
		// Already a mushroom
		if (serverLevel.getBlockState(pos).getBlock() instanceof MushroomBlock)
			return false;
	
		// Temperature must be suitable
		if (!MushroomActionHelpers.isGoodMushroomTemperature(rgCtx))
			return false;
	
		// Check for nearby mushroom stems in a 7x7 horizontal box, 1 block tall
		if (MushroomActionHelpers.isNearbyMushroomStem(serverLevel, pos)) {
			return false;
		}
	
		if (serverLevel.hasNearbyAlivePlayer((double) pos.getX(), (double) pos.getY(), (double) pos.getZ(), 12.0)) {
			return false;
		}
	
		return true;
	}

	// check for another mushroom stem nearby
	public static boolean isNearbyMushroomStem(ServerLevel level, BlockPos center) {
	
		// 3D slice: 7x7 horizontally, 3 blocks tall from entity's feet
		int radius = 3;
		BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
	
		for (int x = -radius; x <= radius; x++) {
			for (int y = 0; y <= 2; y++) { // y = entity level to +2
				for (int z = -radius; z <= radius; z++) {
					mutablePos.set(center.getX() + x, center.getY() + y, center.getZ() + z);
					Block block = level.getBlockState(mutablePos).getBlock();
					// Count stems only: HugeMushroomBlock that is NOT a cap
					if (block == Blocks.MUSHROOM_STEM) {
						return true;
					}
				}
			}
		}
	
		return false;
	}

	public static boolean isGoodMushroomTemperature(ActionContext rgCtx) {
	
		LivingEntity le = rgCtx.livingEntity();
		ServerLevel serverLevel = rgCtx.serverLevel();
	
		Holder<Biome> biome = serverLevel.getBiome(le.blockPosition());
		float biomeTemp = biome.value().getBaseTemperature();
	
		if (rgCtx.doDebug()) {
			Utility.debugMsg(1, le, "Mushroom Biome temp: " + biomeTemp + ".");
		}
	
		if (biomeTemp < MyConfig.getMushroomMinTemp())
			return false;
		if (biomeTemp > MyConfig.getMushroomMaxTemp())
			return false;
	
		return true;
	}

}
