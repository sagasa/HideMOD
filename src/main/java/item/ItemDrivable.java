package item;

import entity.EntityDrivable;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemDrivable extends Item {

    public ItemDrivable() {
        this.maxStackSize = 1;
        this.setCreativeTab(CreativeTabs.COMBAT);
    }

    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            EntityDrivable entityDrivable = new EntityDrivable(worldIn, hitX, hitY, hitZ);
            worldIn.spawnEntity(entityDrivable);
        }
        if (!player.capabilities.isCreativeMode) player.getHeldItem(hand).shrink(1);

        /* TODO: 車両スポーン可能ブロックの制限 */
        return EnumActionResult.SUCCESS;
    }
}
