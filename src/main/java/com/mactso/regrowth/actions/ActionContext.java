package com.mactso.regrowth.actions;

import java.util.Locale;

import com.mactso.regrowth.managers.RegrowthEntitiesManager;
import com.mactso.regrowth.managers.RegrowthEntitiesManager.RegrowthMobItem;
import com.mactso.regrowth.managers.WallBiomeDataManager;
import com.mactso.regrowth.modloader.config.MyConfig;
import com.mactso.regrowth.utilities.MyUtilities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;

public class ActionContext {
	private LivingEntity entity;
	private Villager ve;
	private ServerLevel serverLevel;
	private final MinecraftServer server;
	private final Block biomeRoadBlock;
	private RandomSource rand;
	private Boolean doDebug;
	private String key;
	private String regrowthActions; // e.g., "stumble grow eat"
	private GlobalPos villagerMeetingPointPos = null;
	private boolean hasMeetingPoint = false;
	private boolean isAtAValidWallSpot = false;
	private boolean isAtAValidGateSpot = false;
	private BlockPos adjustedPos;
	private BlockPos footBlockPos;
	private BlockState groundBlockState;
	private Block groundBlock;
	private BlockState footBlockState;
	private Block footBlock;
	private Holder<Biome> biomeHolder;
	private Biome localBiome;
	private String biomeCategory;
	private WallBiomeDataManager.WallBiomeDataItem wallBiomeDataItem = null;
	private Registry<Block> blockRegistry;
	private Registry<Item> itemRegistry;
	private Registry<Biome> biomeRegistry;


	public ActionContext(LivingEntity entity, Villager ve, ServerLevel serverLevel, MinecraftServer server,
			Boolean doDebug, String key, String regrowthActions, BlockPos adjustedPos, BlockPos footBlockPos,
			BlockState groundBlockState, Block groundBlock, BlockState footBlockState, Block footBlock,
			Holder<Biome> biomeHolder, Biome localBiome, Block biomeRoadBlock, String biomeCategory) {
		this.entity = entity;
		this.ve = ve;
		this.serverLevel = serverLevel;
		this.server = server;
		this.rand = serverLevel.getRandom();
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

	public MinecraftServer server() {
		return server;
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

	public GlobalPos villageMeetingPointPos() {

		if (villagerMeetingPointPos != null)
			return villagerMeetingPointPos;

		// Load the villager's meeting point
		villagerMeetingPointPos = ve.getBrain().getMemory(MemoryModuleType.MEETING_POINT).orElse(null);

		// If we got a meeting point, compute wall/gate distances
		if (villagerMeetingPointPos == null)
			return null;

		BlockPos mpPos = villagerMeetingPointPos.pos();
		if (serverLevel.getBlockState(mpPos).getBlock() != Blocks.BELL)
			return null;

		hasMeetingPoint = true;

		BlockPos controlWallPos = villagerMeetingPointPos.pos().above();
		Block configuredWallControlBlock = ActionUtilities.getPlayerWallControlBlockFromConfig();
		if (ActionTests.isNewChunk(serverLevel, villagerMeetingPointPos.pos())) {
			serverLevel.setBlockAndUpdate(controlWallPos, configuredWallControlBlock.defaultBlockState());
		}

		Holder<Biome> vmpBiomeHolder = serverLevel.getBiome(mpPos);
		this.biomeCategory = MyUtilities.getMyBiomeCategory(vmpBiomeHolder);
		
		if (serverLevel.getBlockState(controlWallPos).getBlock() != configuredWallControlBlock)
			return villagerMeetingPointPos;


		if (!ActionTests.isWallBuildingOn())
			return villagerMeetingPointPos;

		BlockPos vePos = adjustedPos();

		int wallRadius = wallBiomeDataItem().getWallRadius(); // (wallLength / 2) + 1;

		// Overall wall validity
		isAtAValidWallSpot = WallActionHelpers.isAtValidWallSpot(vePos, mpPos, wallRadius);

		isAtAValidGateSpot = WallActionHelpers.isAtValidGateSpot(vePos, villagerMeetingPointPos.pos(), wallBiomeDataItem.getWallRadius());

		return villagerMeetingPointPos;
	}

	public Registry<Block> blockRegistry() {
	    if (blockRegistry == null) {
	        if (serverLevel != null) {
	            blockRegistry = MyUtilities.getRegistrySafe(serverLevel.registryAccess(), Registries.BLOCK);
	        }
	        if (blockRegistry == null) {
	            MyUtilities.debugMsg(0, "Error: Block registry is not available.");
	        }
	    }
	    return blockRegistry;
	}

	public Registry<Item> itemRegistry() {
	    if (itemRegistry == null) {
	        if (serverLevel != null) {
	            itemRegistry = MyUtilities.getRegistrySafe(serverLevel.registryAccess(), Registries.ITEM);
	        }
	        if (itemRegistry == null) {
	            MyUtilities.debugMsg(0, "Error: Item registry is not available.");
	        }
	    }
	    return itemRegistry;
	}

	public Registry<Biome> biomeRegistry() {
	    if (biomeRegistry == null) {
	        if (serverLevel != null) {
	            biomeRegistry = MyUtilities.getRegistrySafe(serverLevel.registryAccess(), Registries.BIOME);
	        }
	        if (biomeRegistry == null) {
	            MyUtilities.debugMsg(0, "Error: Biome registry is not available.");
	        }
	    }
	    return biomeRegistry;
	}

	public boolean hasMeetingPoint() {
		return hasMeetingPoint;
	}

	public boolean isAtAValidWallSpot() {
		return isAtAValidWallSpot;
	}

	public boolean isAtAValidGateSpot() {
		return isAtAValidGateSpot;
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

	// lazy initialization; villagers rarely on wall perimeter.
	public WallBiomeDataManager.WallBiomeDataItem wallBiomeDataItem() {
		if (wallBiomeDataItem == null) {
			// Use Locale.ROOT to ensure consistent lower-casing
			this.villageMeetingPointPos();
			Holder<Biome> vmpBiomeHolder = serverLevel.getBiome(this.villageMeetingPointPos().pos());
			String vmpBiomeCategory = MyUtilities.getMyBiomeCategory(vmpBiomeHolder);
			String wallKey = ("minecraft:" + vmpBiomeCategory.toLowerCase(Locale.ROOT))  ;
			wallBiomeDataItem = WallBiomeDataManager.getWallBiomeDataItem(this.server(), wallKey);
		}
		return wallBiomeDataItem;
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
		MinecraftServer server = serverLevel.getServer();
		boolean doDebug = (MyConfig.getaDebugLevel() > 0);
		String key = MyUtilities.getResourceLocationString(le).toString();
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
		String biomeCategory = MyUtilities.getMyBiomeCategory(biomeHolder);
		Block biomeRoadBlock = ActionUtilities.getBiomeRoadBlockType(MyUtilities.GetBiomeName(localBiome)).getBlock();

		return new ActionContext(le, ve, serverLevel, server, doDebug, key, regrowthActions, adjustedPos, footPos,
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
