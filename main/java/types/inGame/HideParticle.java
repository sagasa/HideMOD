package types.inGame;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.util.EnumParticleTypes;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class HideParticle {
	public static void spawnParticle(double x,double y,double z){
		EntityFX fx = Minecraft.getMinecraft().effectRenderer.spawnEffectParticle(2, x, y, z, 0D, 0D, 0D, new int[0]);
	}

	public static EntityFX spawnExplosionEffect(double x,double y,double z,float range){
		return null;
	}


	public static EntityFX getParticleEntity(String name,int x,int y,int z,int[] data){
		name = name.toLowerCase();
		int ParticleID = 0;
		for(EnumParticleTypes type : EnumParticleTypes.values()){
			if(name.contains(type.getParticleName().toLowerCase().replace("_", ""))){
				ParticleID = type.getParticleID();
			}
		}
		return Minecraft.getMinecraft().effectRenderer.spawnEffectParticle(ParticleID, x, y, z, 0D, 0D, 0D, data);
	}
}
