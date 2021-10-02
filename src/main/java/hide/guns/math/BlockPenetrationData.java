package hide.guns.math;

import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;

public class BlockPenetrationData {


    private static float getThickness(Block b, Material mat, float liquidMultiplier) {
        if (mat == Material.IRON) {
            //金属
            if (b == Blocks.LAPIS_BLOCK)
                return 150;
            if (b == Blocks.GOLD_BLOCK)
                return 300;
            if (b == Blocks.IRON_BLOCK)
                return 1000;
            if (b == Blocks.DIAMOND_BLOCK)
                return 250;
            if (b == Blocks.IRON_DOOR)
                return 200;
            if (b == Blocks.IRON_BARS)
                return 100;
            if (b == Blocks.BREWING_STAND)
                return 50;
            if (b == Blocks.CAULDRON)
                return 30;
            if (b == Blocks.EMERALD_BLOCK)
                return 140;
            if (b == Blocks.IRON_TRAPDOOR)
                return 400;
            if (b == Blocks.HOPPER)
                return 100;
            if (b == Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE)
                return 50;
            if (b == Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE)
                return 30;
            return 200;
        }
        if (mat == Material.ROCK) {
            if (b == Blocks.COBBLESTONE)
                return 350;
            if (b == Blocks.BEDROCK)
                return 2000;
            if (b == Blocks.DISPENSER || b == Blocks.DROPPER || b == Blocks.FURNACE || b == Blocks.LIT_FURNACE || b == Blocks.OBSERVER)
                return 100;
            if (b == Blocks.SANDSTONE || b == Blocks.RED_SANDSTONE)
                return 150;
            if (b == Blocks.BRICK_BLOCK)
                return 100;
            if (b == Blocks.MOSSY_COBBLESTONE)
                return 300;
            if (b == Blocks.OBSIDIAN)
                return 80;
            if (b == Blocks.STONE_STAIRS)
                return 360;
            if (b == Blocks.STONE_PRESSURE_PLATE)
                return 40;
            if (b == Blocks.COAL_ORE || b == Blocks.DIAMOND_ORE || b == Blocks.EMERALD_ORE || b == Blocks.GOLD_ORE ||
                    b == Blocks.IRON_ORE || b == Blocks.LAPIS_ORE || b == Blocks.LIT_REDSTONE_ORE || b == Blocks.REDSTONE_ORE)
                return 375;
            if (b == Blocks.NETHERRACK)
                return 40;
            if (b == Blocks.STONEBRICK)
                return 375;
            if (b == Blocks.STONE_BRICK_STAIRS)
                return 340;
            if (b == Blocks.BRICK_STAIRS)
                return 90;
            if (b == Blocks.NETHER_BRICK || b == Blocks.RED_NETHER_BRICK)
                return 300;
            if (b == Blocks.NETHER_BRICK_FENCE)
                return 150;
            if (b == Blocks.NETHER_BRICK_STAIRS)
                return 270;
            if (b == Blocks.ENCHANTING_TABLE)
                return 60;
            if (b == Blocks.END_PORTAL_FRAME)
                return 450;
            if (b == Blocks.END_STONE)
                return 500;
            if (b == Blocks.SANDSTONE_STAIRS || b == Blocks.RED_SANDSTONE_STAIRS)
                return 135;
            if (b == Blocks.ENDER_CHEST)
                return 40;
            if (b == Blocks.COBBLESTONE_WALL)
                return 90;
            if (b == Blocks.QUARTZ_ORE)
                return 50;
            if (b == Blocks.QUARTZ_BLOCK)
                return 120;
            if (b == Blocks.QUARTZ_STAIRS)
                return 108;
            if (b == Blocks.STAINED_HARDENED_CLAY || b == Blocks.HARDENED_CLAY || b instanceof BlockGlazedTerracotta)
                return 70;
            if (b == Blocks.PRISMARINE)
                return 375;
            if (b == Blocks.COAL_BLOCK)
                return 60;
            if (b == Blocks.PURPUR_BLOCK || b == Blocks.PURPUR_PILLAR)
                return 550;
            if (b == Blocks.PURPUR_SLAB || b == Blocks.PURPUR_DOUBLE_SLAB)
                return 450;
            if (b == Blocks.PURPUR_STAIRS)
                return 500;
            if (b == Blocks.END_BRICKS)
                return 450;
            if (b == Blocks.MAGMA)
                return 50;
            if (b == Blocks.BONE_BLOCK)
                return 70;
            if (b instanceof BlockShulkerBox)
                return 40;
            if (b == Blocks.CONCRETE)
                return 375;
            //石材
            return 400;
        } else if (mat == Material.SAND) {
            if (b == Blocks.GRAVEL)
                return 150;
            if (b == Blocks.SOUL_SAND)
                return 250;
            //砂
            return 200;
        } else if (mat == Material.WOOD) {
            if (b == Blocks.LOG || b == Blocks.LOG2)
                return 25;
            if (b == Blocks.CRAFTING_TABLE)
                return 20;
            if (b instanceof BlockTrapDoor)
                return 20;
            if (b == Blocks.BROWN_MUSHROOM_BLOCK || b == Blocks.RED_MUSHROOM_BLOCK)
                return 5;
            if (b == Blocks.WOODEN_SLAB)
                return 12;
            if (b instanceof BlockStairs)
                return 13.5f;
            //木材
            return 15;
        } else if (mat == Material.GLASS) {
            if (b == Blocks.GLOWSTONE)
                return 20;
            if (b == Blocks.BEACON)
                return 20;
            if (b == Blocks.SEA_LANTERN)
                return 40;
            //ガラス
            return 10;
        } else if (mat == Material.GROUND) {
            //土
            if (b == Blocks.FARMLAND)
                return 100;
            if (b == Blocks.GRASS_PATH)
                return 90;
            return 80;
        } else if (mat == Material.CLAY) {
            if (b == Blocks.SLIME_BLOCK)
                return 80 * liquidMultiplier;
            return 60;
        } else if (mat == Material.ANVIL) {
            //金床
            return 450;
        } else if (mat == Material.CLOTH || mat == Material.CARPET) {
            return 3;
        } else if (mat == Material.PISTON) {
            if (b == Blocks.PISTON_HEAD)
                return 20;
            return 100;
        } else if (mat == Material.LEAVES || mat == Material.PLANTS) {
            return 1;
        } else if (mat == Material.REDSTONE_LIGHT) {
            return 25;
        } else if (mat == Material.BARRIER) {
            return 10000;
        } else if (mat == Material.GRASS) {
            if (b == Blocks.HAY_BLOCK)
                return 2;
            return 85;
        } else if (mat == Material.CACTUS || mat == Material.CORAL || mat == Material.GOURD) {
            //植物
            return 10;
        } else if (mat == Material.AIR || mat == Material.FIRE) {
            return 0;
        } else if (mat == Material.ICE) {
            return 70;
        } else if (mat == Material.PACKED_ICE) {
            return 200;
        } else if (mat == Material.CIRCUITS) {
            if (b instanceof BlockRailBase)
                return 50;
            if (b instanceof BlockTorch)
                return 10;
            if (b == Blocks.REDSTONE_WIRE)
                return 1;
            if (b == Blocks.LADDER)
                return 5;
            if (b == Blocks.TRIPWIRE)
                return 0.5f;
            if (b == Blocks.LEVER)
                return 40;
            if (b == Blocks.STONE_BUTTON)
                return 50;
            if (b == Blocks.WOODEN_BUTTON)
                return 10;
            if (b == Blocks.TRIPWIRE_HOOK)
                return 40;
            if (b instanceof BlockRedstoneDiode)
                return 40;
            if (b == Blocks.END_ROD)
                return 60;
            return 10;
        } else if (mat == Material.SNOW || mat == Material.CRAFTED_SNOW) {
            return 15;
        } else if (mat == Material.WATER) {
            return 10 * liquidMultiplier;
        } else if (mat == Material.LAVA) {
            return 50 * liquidMultiplier;
        } else if (mat == Material.TNT) {
            return 50;
        } else if (mat == Material.VINE) {
            return 1;
        } else if (mat == Material.CAKE) {
            return 1;
        } else if (mat == Material.DRAGON_EGG) {
            return 10;
        } else if (mat == Material.WEB) {
            return 2;
        } else if (mat == Material.PORTAL) {
            return 0;
        }
        //System.out.println("not found " + b + " " + " " + (b.getClass()));
        return -1;
    }

    public static float getThickness(IBlockState state, float liquidMultiplier) {
        final Block b = state.getBlock();
        final Material mat = state.getMaterial();
        if (state.getMaterial() == Material.ROCK) {
            if (b == Blocks.STONE_SLAB || b == Blocks.DOUBLE_STONE_SLAB) {
                BlockStoneSlab.EnumType type = state.getValue(BlockStoneSlab.VARIANT);
                if (type == BlockStoneSlab.EnumType.BRICK)
                    //クソが死ね 名前が対応してなかったから例外
                    return getThickness(Blocks.BRICK_BLOCK, mat,liquidMultiplier) * 0.8f;
                else
                    return getThickness(Block.REGISTRY.getObject(new ResourceLocation(type.getName())), mat,liquidMultiplier) * 0.8f;
            } else if (b == Blocks.STONE_SLAB2 || b == Blocks.DOUBLE_STONE_SLAB2) {
                BlockStoneSlabNew.EnumType type = state.getValue(BlockStoneSlabNew.VARIANT);
                return getThickness(Block.REGISTRY.getObject(new ResourceLocation(type.getName())), mat,liquidMultiplier) * 0.8f;
            }
        } else if (mat == Material.SPONGE) {
            return state.getValue(BlockSponge.WET) ? 10 : 2;
        }

        return getThickness(state.getBlock(), mat,liquidMultiplier);
    }
}
