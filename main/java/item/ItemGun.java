package item;

import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class ItemGun extends Item{
	@Override
    public void onUpdate(ItemStack itemStack, World world, Entity entity, int slot, boolean isHeld) {

		NBTTagCompound NBTtag = new NBTTagCompound();
		NBTtag.setBoolean("isHideGun", true);
		itemStack.setTagCompound(NBTtag);;

    }
}
