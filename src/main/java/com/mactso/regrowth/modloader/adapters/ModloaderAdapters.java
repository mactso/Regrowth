package com.mactso.regrowth.modloader.adapters;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;

/**
 * Platform-agnostic modloader adapter.
 * 
 * Provides farmland hydration check across modloaders. Fabric has no hydration manager,
 * so this method always returns false. Used by common code to remain platform-independent.
 */
public class ModloaderAdapters {
	
    /**
     * Checks if a farmland block at the specified position is hydrated.
     *
     * @param level the world or level to query
     * @param pos the block position to check
     * @return true if the block is hydrated; false for Fabric (no hydration manager)
     */
	public static boolean isLandHydrated (LevelReader level, BlockPos pos) {

		return false;
	}
}
