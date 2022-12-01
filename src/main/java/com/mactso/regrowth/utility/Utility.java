package com.mactso.regrowth.utility;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mactso.regrowth.config.MyConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec3;

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
	
	public static String getMyBC (Holder<Biome> testBiome) {
		
		if (testBiome.is(BiomeTags.HAS_VILLAGE_DESERT))
			return Utility.DESERT;
		if (testBiome.is(BiomeTags.IS_FOREST))
			return Utility.FOREST;
		if (testBiome.is(BiomeTags.IS_BEACH))
			return Utility.BEACH;
		if (testBiome.is(BiomeTags.HAS_VILLAGE_SNOWY))
			return Utility.ICY;		
		if (testBiome.is(BiomeTags.IS_JUNGLE))
			return Utility.JUNGLE;		
		if (testBiome.is(BiomeTags.IS_OCEAN))
			return Utility.OCEAN;		
		if (testBiome.is(BiomeTags.IS_DEEP_OCEAN))
			return Utility.OCEAN;		
		if (testBiome.is(BiomeTags.HAS_VILLAGE_PLAINS))
			return Utility.PLAINS;		
		if (testBiome.is(BiomeTags.IS_RIVER))
			return Utility.RIVER;		
		if (testBiome.is(BiomeTags.IS_SAVANNA))
			return Utility.SAVANNA;		
		if (testBiome.is(BiomeTags.ALLOWS_SURFACE_SLIME_SPAWNS))
			return Utility.SWAMP;		
		if (testBiome.is(BiomeTags.IS_TAIGA))
			return Utility.TAIGA;		
		if (testBiome.is(BiomeTags.IS_BADLANDS))
			return Utility.BADLANDS;		
		if (testBiome.is(BiomeTags.IS_MOUNTAIN))
			return Utility.EXTREME_HILLS;		
		if (testBiome.is(BiomeTags.IS_NETHER))
			return Utility.NETHER;	
		return NONE;

	}
	

	public static void debugMsg(int level, String dMsg) {

		if (MyConfig.getaDebugLevel() > level - 1) {
			LOGGER.info("L" + level + ":" + dMsg);
		}

	}

	public static void debugMsg(int level, BlockPos pos, String dMsg) {

		if (MyConfig.getaDebugLevel() > level - 1) {
			LOGGER.info("L" + level + " (" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + "): " + dMsg);
		}

	}

	public static void debugMsg(int level, LivingEntity le, String dMsg) {

		if (MyConfig.getaDebugLevel() > level - 1) {
			LOGGER.info("L" + level + " (" 
					+ le.blockPosition().getX() + "," 
					+ le.blockPosition().getY() + ","
					+ le.blockPosition().getZ() + "): " + dMsg);
		}

	}

	public static void sendBoldChat(Player p, String chatMessage, ChatFormatting textColor) {

		MutableComponent component = Component.literal(chatMessage);
		component.setStyle(component.getStyle().withBold(true));
		component.setStyle(component.getStyle().withColor(textColor));
		p.sendSystemMessage(component);


	}

	public static void sendChat(Player p, String chatMessage, ChatFormatting textColor) {

		MutableComponent component = Component.literal(chatMessage);
		component.setStyle(component.getStyle().withColor(textColor));
		p.sendSystemMessage(component);

	}
	
	public static void updateEffect(LivingEntity e, int amplifier, MobEffect mobEffect, int duration) {
		MobEffectInstance ei = e.getEffect(mobEffect);
		if (amplifier == 10) {
			amplifier = 20; // player "plaid" speed.
		}
		if (ei != null) {
			if (amplifier > ei.getAmplifier()) {
				e.removeEffect(mobEffect);
			}
			if (amplifier == ei.getAmplifier() && ei.getDuration() > 10) {
				return;
			}
			if (ei.getDuration() > 10) {
				return;
			}
			e.removeEffect(mobEffect);
		}
		e.addEffect(new MobEffectInstance(mobEffect, duration, amplifier, true, true));
		return;
	}

	public static boolean populateEntityType(EntityType<?> et, ServerLevel level, BlockPos savePos, int range,
			int modifier) {
		boolean isBaby = false;
		return populateEntityType(et, level, savePos, range, modifier, isBaby);
	}

	public static boolean populateEntityType(EntityType<?> et, ServerLevel level, BlockPos savePos, int range,
			int modifier, boolean isBaby) {
		boolean persistant = false;
		return populateEntityType(et, level, savePos, range, modifier, persistant, isBaby);
	}

	public static boolean populateEntityType(EntityType<?> et, ServerLevel level, BlockPos savePos, int range,
			int modifier, boolean persistant, boolean isBaby) {
		int numZP;
		Mob e;
		numZP = level.random.nextInt(range) - modifier;
		if (numZP < 0)
			return false;
		for (int i = 0; i <= numZP; i++) {

			e = (Mob) et.spawn(level, null, null, null, savePos.north().west(), MobSpawnType.NATURAL, true, true);

			if (persistant) 
				e.setPersistenceRequired();
			e.setBaby(isBaby);
		}
		return true;
	}

	public static boolean populateXEntityType(EntityType<?> et, ServerLevel level, BlockPos savePos, int X,  boolean isBaby) {
		Mob e;

		for (int i = 0; i < X; i++) {
			e = (Mob) et.spawn(level, null, null, null, savePos.north().west(), MobSpawnType.NATURAL, true, true);
			e.setBaby(isBaby);
		}
		return true;
	}

	
	public static void setName(ItemStack stack, String inString)
	{
		CompoundTag tag = stack.getOrCreateTagElement("display");
		ListTag list = new ListTag();
		list.add(StringTag.valueOf(inString));
		tag.put("Name", list);
	}
	
	
	public static void setLore(ItemStack stack, String inString)
	{
		CompoundTag tag = stack.getOrCreateTagElement("display");
		ListTag list = new ListTag();
		list.add(StringTag.valueOf(inString));
		tag.put("Lore", list);
	}
	
	public static boolean isNotNearWebs(BlockPos pos, ServerLevel serverLevel) {

		if (serverLevel.getBlockState(pos).getBlock() == Blocks.COBWEB)
			return true;
		if (serverLevel.getBlockState(pos.above()).getBlock() == Blocks.COBWEB)
			return true;
		if (serverLevel.getBlockState(pos.below()).getBlock() == Blocks.COBWEB)
			return true;
		if (serverLevel.getBlockState(pos.north()).getBlock() == Blocks.COBWEB)
			return true;
		if (serverLevel.getBlockState(pos.south()).getBlock() == Blocks.COBWEB)
			return true;
		if (serverLevel.getBlockState(pos.east()).getBlock() == Blocks.COBWEB)
			return true;
		if (serverLevel.getBlockState(pos.west()).getBlock() == Blocks.COBWEB)
			return true;

		return false;
	}

	public static boolean isOutside(BlockPos pos, ServerLevel serverLevel) {
		return serverLevel.getHeightmapPos(Types.MOTION_BLOCKING_NO_LEAVES, pos) == pos;
	}

	public static void slowFlyingMotion(LivingEntity le) {

		if ((le instanceof Player) && (le.isFallFlying())) {
			Player cp = (Player) le;
			Vec3 vec = cp.getDeltaMovement();
			Vec3 slowedVec;
			if (vec.y > 0) {
				slowedVec = vec.multiply(0.17, -0.75, 0.17);
			} else {
				slowedVec = vec.multiply(0.17, 1.001, 0.17);
			}
			cp.setDeltaMovement(slowedVec);
		}
	}	
	
	public static String getResourceLocationString(ServerLevel serverLevel, BlockState blockState) {
		return getResourceLocationString(serverLevel,blockState.getBlock());
		}


	@SuppressWarnings("deprecation")
	public static String getResourceLocationString(ServerLevel serverLevel, Block block) {
		int id = Block.getId(block.defaultBlockState());
		Optional<? extends Registry<Block>> opt = serverLevel.registryAccess().registry(Registry.BLOCK_REGISTRY);
		String result = "BlockRegMissing";
		if (opt.isPresent()) {
			ResourceLocation rl = opt.get().getKey(block);
			result = rl.toString();
        }
		return result;
	}

	@SuppressWarnings("deprecation")
	public static String getResourceLocationString(ServerLevel serverLevel, Item item) {
		int id = Item.getId(item);
		String result = "ItemRegMissing";
		Optional<? extends Registry<Item>> opt = serverLevel.registryAccess().registry(Registry.ITEM_REGISTRY);
		if (opt.isPresent()) {
			ResourceLocation rl = opt.get().getKey(item);
			result = rl.toString();
        }
		return result;
	}

	public static String getResourceLocationString(Entity entity) {
		EntityType<?> et = entity.getType();
		return 	EntityType.getKey(et).toString();
	}

	public static String GetBiomeName(Biome b) {
		return b.toString();
	}


	


}
