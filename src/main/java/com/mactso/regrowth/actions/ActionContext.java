package com.mactso.regrowth.actions;

import com.mactso.regrowth.config.MyConfig;
import com.mactso.regrowth.config.RegrowthEntitiesManager;
import com.mactso.regrowth.config.RegrowthEntitiesManager.RegrowthMobItem;
import com.mactso.regrowth.utility.Utility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;

public class ActionContext {
	private LivingEntity entity;
	private Villager ve;
	private ServerLevel serverLevel;
	private final Block biomeRoadBlock;
	private RandomSource rand;
	private Boolean doDebug;
	private String key;
	private String regrowthActions; // e.g., "stumble grow eat"
	private BlockPos adjustedPos;
	private BlockPos footBlockPos;
	private BlockState groundBlockState;
	private Block groundBlock;
	private BlockState footBlockState;
	private Block footBlock;
	private Holder<Biome> biomeHolder;
	private Biome localBiome;
	private String biomeCategory;

	public ActionContext(LivingEntity entity, Villager ve, ServerLevel serverlevel, Boolean doDebug, String key,
			String regrowthActions, BlockPos adjustedPos, BlockPos footBlockPos, BlockState groundBlockState,
			Block groundBlock, BlockState footBlockState, Block footBlock, Holder<Biome> biomeHolder, Biome localBiome,
			Block biomeRoadBlock, String biomeCategory) {
		this.entity = entity;
		this.ve = ve;
		this.serverLevel = serverlevel;
		this.rand = serverlevel.getRandom();
		this.doDebug = doDebug;
		this.key = key;
		this.regrowthActions = regrowthActions;
		this.adjustedPos = adjustedPos;
		this.footBlockPos = footBlockPos;
		this.groundBlockState = groundBlockState;
		this.groundBlock = groundBlock;
		this.footBlockState = footBlockState;
		this.footBlock = footBlock;
		this.biomeHolder = biomeHolder;
		this.localBiome = localBiome;
		this.biomeRoadBlock = biomeRoadBlock;
		this.biomeCategory = biomeCategory;
	}

// ---------------------
// Getters
// ---------------------
	public LivingEntity livingEntity() {
		return entity;
	}

	public Villager ve() {
		return ve;
	}

	public ServerLevel serverLevel() {
		return serverLevel;
	}

	public Holder<Biome> getBiomeHolder() {
		return biomeHolder;
	}

	public RandomSource getRand() {
		return rand;
	}

	public Boolean doDebug() {
		return doDebug;
	}

	public String key() {
		return key;
	}

	public String regrowthActions() {
		return regrowthActions;
	}

	public BlockPos adjustedPos() {
		return adjustedPos;
	}

	public BlockPos footBlockPos() {
		return footBlockPos;
	}

	public BlockState groundBlockState() {
		return groundBlockState;
	}

	public Block groundBlock() {
		return groundBlock;
	}

	public BlockState footBlockState() {
		return footBlockState;
	}

	public Block footBlock() {
		return footBlock;
	}

	public Holder<Biome> biomeHolder() {
		return biomeHolder;
	}

	public Biome localBiome() {
		return localBiome;
	}

	public Block biomeRoadBlock() {
		return biomeRoadBlock;
	}

	public String biomeCategory() {
		return biomeCategory;
	}

// ---------------------
// Setters for mutable fields
// ---------------------
	public void setFootBlockState(BlockState footBlockState) {
		this.footBlockState = footBlockState;
	}

	public void setFootBlock(Block footBlock) {
		this.footBlock = footBlock;
	}

	public void setGroundBlockState(BlockState groundBlockState) {
		this.groundBlockState = groundBlockState;
	}

	public void setGroundBlock(Block groundBlock) {
		this.groundBlock = groundBlock;
	}

	public void setAdjustedPos(BlockPos adjustedPos) {
		this.adjustedPos = adjustedPos;
	}

	public static ActionContext buildRgCtx(LivingEntity le) {

		Villager ve = null;
		if (le instanceof Villager) {
			ve = (Villager) le;
		}

		ServerLevel serverLevel = (ServerLevel) le.level();
		boolean doDebug = (MyConfig.getaDebugLevel() > 0);
		String key = Utility.getResourceLocationString(le).toString();
		RegrowthMobItem mobInfo = RegrowthEntitiesManager.getRegrowthMobInfo(key);
		if (mobInfo == null)
			return null;

		String regrowthActions = mobInfo.getRegrowthActions();

		BlockPos adjustedPos = ActionUtilities.getAdjustedPos(le);
		BlockPos footPos = adjustedPos; // air, partial blocks, blocks with no hit box (like tall_grass, flowers)
		BlockPos groundPos = footPos.below();
		
		BlockState footBlockState = serverLevel.getBlockState(adjustedPos);
		Block footBlock = footBlockState.getBlock();
		
		BlockState groundBlockState = serverLevel.getBlockState(groundPos);
		Block groundBlock = groundBlockState.getBlock();


		// Early exit checks
		if ((groundBlockState.isAir()) && !(le instanceof Bat))
			return null;
		if (!ActionTests.isFootblockValid(footBlockState))
			return null;
		if (ActionTests.isImpossibleRegrowthEvent(regrowthActions, footBlockState, footBlock))
			return null;

		Holder<Biome> biomeHolder = serverLevel.getBiome(le.blockPosition());
		Biome localBiome = biomeHolder.value();
		Block biomeRoadBlock = ActionUtilities.getBiomeRoadBlockType(Utility.GetBiomeName(localBiome)).getBlock();
		String biomeCategory = Utility.getMyBiomeCategory(biomeHolder);

		return new ActionContext(le, ve, serverLevel, doDebug, key, regrowthActions, adjustedPos, footPos,
				groundBlockState, groundBlock, footBlockState, footBlock, biomeHolder, localBiome, biomeRoadBlock,
				biomeCategory);
	}

	// ---------------------
	// Utility methods
	// ---------------------

	// is in bed.
	public boolean isInTorchBlock() {
		if (this.footBlock instanceof TorchBlock)
			return true;
		if (this.footBlock instanceof WallTorchBlock)
			return true;

		return false;
	}

	// is in bed.
	public boolean isInBed() {
		if (this.footBlock instanceof BedBlock)
			return true;
		return false;
	}

	// is inside road after fixing road?
	public boolean isInsideRoadBlock() {

		if (this.footBlock == this.biomeRoadBlock)
			return true;

		return false;
	}

	// is inside wall (after building wall or just pressed tightly against it)
	public boolean isInsideWallBlock() {

		if (this.footBlock instanceof WallBlock)
			return true;

		return false;
	}

	public boolean isStandingOnDirtOrGrass() {

		// Check if the block state is considered grass by ActionTests
		if (ActionTests.isKindOfGrassBlock(this.groundBlockState())) {
			return true;
		}

		if (ActionTests.isKindOfDirtBlock(this.groundBlockState())) {
			return true;
		}

		return false;
	}

	public boolean isStandingOnRoad() {
		if (groundBlock == biomeRoadBlock)
			return true;
		return false;
	}

	/** Villager-style single-letter action */
	public boolean hasVillagerAction(char action) {
		return regrowthActions != null && regrowthActions.indexOf(action) >= 0;
	}

	/** Mob-style single-word action, strict equality */
	public boolean hasMobAction(String action) {
		return regrowthActions != null && regrowthActions.equals(action);
	}

}
