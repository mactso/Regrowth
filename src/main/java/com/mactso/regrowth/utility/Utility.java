package com.mactso.regrowth.utility;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mactso.regrowth.config.MyConfig;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.tag.BiomeTags;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public class Utility {
	
	public static String NONE = "none";
	public static String BEACH = "beach";
	public static String BADLANDS = "badlands";
	public static String DESERT = "desert";
	public static String EXTREME_HILLS = "extreme_hills";
	public static String ICY = "icy";
	public static String JUNGLE = "jungle";
	public static String THEEND = "the_end";
	public static String FOREST = "forest";
	public static String MESA = "mesa";
	public static String MUSHROOM = "mushroom";
	public static String MOUNTAIN = "mountain";
	public static String NETHER = "nether";
	public static String OCEAN = "ocean";
	public static String PLAINS = "plains";
	public static String RIVER = "river";
	public static String SAVANNA = "savanna";
	public static String SWAMP = "swamp";
	public static String TAIGA = "taiga";
	public static String UNDERGROUND = "underground";
	
	public final static int FOUR_SECONDS = 80;
	public final static int TWO_SECONDS = 40;
	public final static float Pct00 = 0.00f;
	public final static float Pct02 = 0.02f;
	public final static float Pct05 = 0.05f;
	public final static float Pct09 = 0.09f;
	public final static float Pct16 = 0.16f;
	public final static float Pct25 = 0.25f;
	public final static float Pct50 = 0.50f;
	public final static float Pct75 = 0.75f;
	public final static float Pct84 = 0.84f;
	public final static float Pct89 = 0.89f;
	public final static float Pct91 = 0.91f;
	public final static float Pct95 = 0.95f;
	public final static float Pct99 = 0.99f;
	public final static float Pct100 = 1.0f;

	private static final Logger LOGGER = LogManager.getLogger();
	
		public static void debugMsg(int level, BlockPos pos, String dMsg) {

			if (MyConfig.getDebugLevel() > level - 1) {
				LOGGER.info("L" + level + " (" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + "): " + dMsg);
			}

		}
	
	public static void debugMsg(int level, LivingEntity le, String dMsg) {

		if (MyConfig.getDebugLevel() > level - 1) {
			LOGGER.info("L" + level + " (" 
					+ le.getBlockPos().getX() + "," 
					+ le.getBlockPos().getY() + ","
					+ le.getBlockPos().getZ() + "): " + dMsg);
		}

	}

	public static void debugMsg(int level, String dMsg) {

		if (MyConfig.getDebugLevel() > level - 1) {
			LOGGER.info("L" + level + ":" + dMsg);
		}

	}

	public static String getMyBC(RegistryEntry<Biome> testBiome) {
	
	if (testBiome.isIn(BiomeTags.VILLAGE_DESERT_HAS_STRUCTURE))
		return Utility.DESERT;
	if (testBiome.isIn(BiomeTags.IS_FOREST))
		return Utility.FOREST;
	if (testBiome.isIn(BiomeTags.IS_BEACH))
		return Utility.BEACH;
	if (testBiome.isIn(BiomeTags.VILLAGE_SNOWY_HAS_STRUCTURE))
		return Utility.ICY;		
	if (testBiome.isIn(BiomeTags.IS_JUNGLE))
		return Utility.JUNGLE;		
	if (testBiome.isIn(BiomeTags.IS_OCEAN))
		return Utility.OCEAN;		
	if (testBiome.isIn(BiomeTags.IS_DEEP_OCEAN))
		return Utility.OCEAN;		
	if (testBiome.isIn(BiomeTags.VILLAGE_PLAINS_HAS_STRUCTURE))
		return Utility.PLAINS;		
	if (testBiome.isIn(BiomeTags.IS_RIVER))
		return Utility.RIVER;		
	if (testBiome.isIn(BiomeTags.IS_SAVANNA))
		return Utility.SAVANNA;		
	if (testBiome.isIn(BiomeTags.ALLOWS_SURFACE_SLIME_SPAWNS))
		return Utility.SWAMP;		
	if (testBiome.isIn(BiomeTags.IS_TAIGA))
		return Utility.TAIGA;		
	if (testBiome.isIn(BiomeTags.IS_BADLANDS))
		return Utility.BADLANDS;		
	if (testBiome.isIn(BiomeTags.IS_MOUNTAIN))
		return Utility.EXTREME_HILLS;		
	return NONE;

}

	@SuppressWarnings("deprecation")
	public static String getResourceLocationString(Block block) {
		return block.getRegistryEntry().registryKey().getValue().toString();
	}

	public static String getResourceLocationString( BlockState blockState) {
		return getResourceLocationString(blockState.getBlock());
		}

	@SuppressWarnings("deprecation")
	public static String getResourceLocationString(Entity entity) {
		return entity.getType().getRegistryEntry().registryKey().getValue().toString();
	}
	
	@SuppressWarnings("deprecation")
	public static String getResourceLocationString(Item item) {
		return item.getRegistryEntry().registryKey().getValue().toString();
	}

	public static String getResourceLocationString(World world) {
		return world.getRegistryKey().getValue().toString();
	}
	
	public static int helperCountBlocksBB(Block searchBlock, int maxCount, World w, BlockPos bPos, int boxSize) {
		return helperCountBlocksBB(searchBlock, maxCount, w, bPos, boxSize, boxSize); // "square" box subcase
	}

	public static int helperCountBlocksBB(Block searchBlock, int maxCount, World w, BlockPos bPos, int boxSize,
			int ySize) {
		int count = 0;
		int minX = bPos.getX() - boxSize;
		int maxX = bPos.getX() + boxSize;
		int minZ = bPos.getZ() - boxSize;
		int maxZ = bPos.getZ() + boxSize;
		int minY = bPos.getY() - ySize;
		int maxY = bPos.getY() + ySize;


		Mutable mPos = new Mutable();
		for (int dx = minX; dx <= maxX; dx++) {
			for (int dz = minZ; dz <= maxZ; dz++) {
				for (int dy = minY; dy <= maxY; dy++) {
					mPos.set(dx, dy, dz);
					if (w.getBlockState(mPos).getBlock() == searchBlock) {
						if (++count >= maxCount)
							return count;
					}
				}
			}
		}

		Utility.debugMsg(1, bPos,
				Utility.getResourceLocationString(searchBlock) + " Sparse count:" + count + " countBlockBB ");

		return count;
	}
	
	public static int helperCountBlocksBB(Class<? extends Block> searchBlock, int maxCount, World w, BlockPos bPos,
			int boxSize) {
		return helperCountBlocksBB(searchBlock, maxCount, w, bPos, boxSize, 0);
	}
	
	public static int helperCountBlocksBB(Class<? extends Block> searchBlock, int maxCount, World w, BlockPos bPos,
			int boxSize, int ySize) {
		int count = 0;
		int minX = bPos.getX() - boxSize;
		int maxX = bPos.getX() + boxSize;
		int minZ = bPos.getZ() - boxSize;
		int maxZ = bPos.getZ() + boxSize;
		int minY = bPos.getY() - ySize;
		int maxY = bPos.getY() + ySize;

		Mutable mPos = new Mutable();

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

		Utility.debugMsg(1, bPos, searchBlock.getSimpleName() + " Sparse count:" + count + " countBlockBB ");

		return count;
	}
	
	public static int helperCountBlocksOrthogonalBB(Block searchBlock, int maxCount, World w, BlockPos bPos,
			int lowerBoundY, int upperBoundY) {
		int count = 0;
		for (int j = lowerBoundY; j <= upperBoundY; j++) {
			if (w.getBlockState(bPos.up(j).east()).getBlock() == searchBlock)
				count++;
			if (w.getBlockState(bPos.up(j).west()).getBlock() == searchBlock)
				count++;
			if (w.getBlockState(bPos.up(j).north()).getBlock() == searchBlock)
				count++;
			if (w.getBlockState(bPos.up(j).south()).getBlock() == searchBlock)
				count++;
			if (count >= maxCount)
				return count;
		}

		return count;

	}
	
	// this routine returns a count of the searchBlock immediately orthogonal to
	// BlockPos, exiting if a max count is exceeded.
	public static int helperCountBlocksOrthogonalBB(Block searchBlock, int maxCount, World w, BlockPos bPos,
			int boundY) {
		return Utility.helperCountBlocksOrthogonalBB(searchBlock, maxCount, w, bPos, 0 - boundY, 0 + boundY);
	}
	
	public static boolean isNotNearWebs(BlockPos pos, ServerWorld serverworld) {

		if (serverworld.getBlockState(pos).getBlock() == Blocks.COBWEB)
			return true;
		if (serverworld.getBlockState(pos.up()).getBlock() == Blocks.COBWEB)
			return true;
		if (serverworld.getBlockState(pos.down()).getBlock() == Blocks.COBWEB)
			return true;
		if (serverworld.getBlockState(pos.north()).getBlock() == Blocks.COBWEB)
			return true;
		if (serverworld.getBlockState(pos.south()).getBlock() == Blocks.COBWEB)
			return true;
		if (serverworld.getBlockState(pos.east()).getBlock() == Blocks.COBWEB)
			return true;
		if (serverworld.getBlockState(pos.west()).getBlock() == Blocks.COBWEB)
			return true;

		return false;
	}

	public static boolean isOutside(BlockPos pos, ServerWorld serverworld) {

		return serverworld.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos) == pos;
	}

	public static boolean populateEntityType(EntityType<?> et, ServerWorld level, BlockPos savePos, int range,
			int modifier) {
		boolean isBaby = false;
		return populateEntityType(et, level, savePos, range, modifier, isBaby);
	}

	
	public static boolean populateEntityType(EntityType<?> et, ServerWorld level, BlockPos savePos, int range,
			int modifier, boolean isBaby) {
		boolean persistant = false;
		return populateEntityType(et, level, savePos, range, modifier, persistant, isBaby);
	}
	
	
	public static boolean populateEntityType(EntityType<?> et, ServerWorld level, BlockPos savePos, int range,
			int modifier, boolean persistant, boolean isBaby) {
		int numZP;
		HostileEntity e;
		numZP = level.random.nextInt(range) - modifier;
		if (numZP < 0)
			return false;
		for (int i = 0; i <= numZP; i++) {
			
			e = ( HostileEntity) et.spawn(level, null, null, null, savePos.north().west(), SpawnReason.NATURAL, true, true);

			if (persistant) 
				e.setPersistent();
			e.setBaby(isBaby);
		}
		return true;
	}
	
	public static boolean populateXEntityType(EntityType<?> et, ServerWorld level, BlockPos savePos, int X,  boolean isBaby) {
		HostileEntity e;

		for (int i = 0; i < X; i++) {
			e = (HostileEntity) et.spawn(level, null, null, null, savePos.north().west(), SpawnReason.NATURAL, true, true);
			e.setBaby(isBaby);
		}
		return true;
	}

	public static void sendBoldChat(PlayerEntity p, String chatMessage, Formatting textColor) {

		MutableText component = Text.literal(chatMessage);
		component.setStyle(component.getStyle().withBold(true));
		component.setStyle(component.getStyle().withColor(textColor));
		p.sendMessage(component);


	}

	public static void sendChat(PlayerEntity p, String chatMessage, Formatting textColor) {

		MutableText component = Text.literal(chatMessage);
		component.setStyle(component.getStyle().withColor(textColor));
		p.sendMessage(component);

	}	
	
	public static void setLore(ItemStack stack, String inString)
	{
		NbtCompound tag = stack.getOrCreateSubNbt("display");
		NbtList list = new NbtList();
		list.add(NbtString.of(inString));
		tag.put("Lore", list);
	}


	public static void setName(ItemStack stack, String inString)
	{
		NbtCompound tag = stack.getOrCreateSubNbt("display");
		NbtList list = new NbtList();
		list.add(NbtString.of(inString));
		tag.put("Name", list);
	}

	public static void slowFlyingMotion(LivingEntity le) {

		if ((le instanceof PlayerEntity) && (le.isFallFlying())) {
			PlayerEntity cp = (PlayerEntity) le;
			Vec3d vec = cp.getVelocity();
			Vec3d slowedVec;
			if (vec.y > 0) {
				slowedVec = vec.multiply(0.17, -0.75, 0.17);
			} else {
				slowedVec = vec.multiply(0.17, 1.001, 0.17);
			}
			cp.setVelocity(slowedVec);
		}
	}

	public static void updateEffect(LivingEntity e, int amplifier, StatusEffect mobEffect, int duration) {
		StatusEffectInstance ei = e.getStatusEffect(mobEffect);
		if (amplifier == 10) {
			amplifier = 20; // player "plaid" speed.
		}
		if (ei != null) {
			if (amplifier > ei.getAmplifier()) {
				e.removeStatusEffect(mobEffect);
			}
			if (amplifier == ei.getAmplifier() && ei.getDuration() > 10) {
				return;
			}
			if (ei.getDuration() > 10) {
				return;
			}
			e.removeStatusEffect(mobEffect);
		}
		e.addStatusEffect(new StatusEffectInstance(mobEffect, duration, amplifier, true, true));
		return;
	}

	public static void warn (String dMsg) {
		LOGGER.warn(dMsg);
	}



}
