package entity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import gamedata.HideDamage;
import gamedata.HideDamage.HideDamageCase;
import handler.PacketHandler;
import handler.SoundHandler;
import hideMod.PackData;
import helper.RayTracer;
import helper.RayTracer.Hit;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockReed;
import net.minecraft.block.BlockSign;
import net.minecraft.block.BlockVine;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import network.PacketHit;
import types.effect.Explosion;
import types.items.GunData;
import types.items.MagazineData;
import types.projectile.BulletData;

/** 銃弾・砲弾・爆弾など投擲系以外の全てこのクラスで追加 */
public class EntityBullet extends Entity {

	/*
	 * ダメージ関係はすべてサーバーサイドで判別 弾が消失した場合DWで位置と通知 ブロックに当たった音はここで出す クライアントサイドで音関連を実行する
	 * 毎Tick通った範囲にプレイヤーがいないか見る…改善の余地あり
	 *
	 * 角度はモーションから計算
	 */

	public EntityBullet(World worldIn) {
		super(worldIn);
		this.setSize(0.5F, 0.5F);
	}

	public EntityBullet(GunData gun, MagazineData magazine, Entity shooter, boolean isADS, float offset, double x,
			double y, double z, float yaw, float pitch) {
		this(shooter.world);

		setLocationAndAngles(x, y, z, yaw, pitch);
		setPosition(posX, posY, posZ);

		// 向いている方向をモーションに
		motionX = -Math.sin(Math.toRadians(rotationYaw)) * Math.cos(Math.toRadians(rotationPitch));
		motionZ = Math.cos(Math.toRadians(rotationYaw)) * Math.cos(Math.toRadians(rotationPitch));
		motionY = -Math.sin(Math.toRadians(rotationPitch));

		float f2 = MathHelper.sqrt(motionX * motionX + motionZ * motionZ + motionY * motionY);
		motionX /= f2;
		motionZ /= f2;
		motionY /= f2;

		setPosition(posX, posY, posZ);
	}

	@Override
	public void onUpdate() {
		onUpdate(1);
	}

	private void onUpdate(float tick) {
		this.lastTickPosX = this.posX;
		this.lastTickPosY = this.posY;
		this.lastTickPosZ = this.posZ;

		this.prevPosX = this.posX + (this.motionX * tick);
		this.prevPosY = this.posY + (this.motionY * tick);
		this.prevPosZ = this.posZ + (this.motionZ * tick);

		this.posX = this.prevPosX;
		this.posY = this.prevPosY;
		this.posZ = this.prevPosZ;
		this.setPosition(this.posX, this.posY, this.posZ);
	}

	@Override
	protected void entityInit() {

	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound compound) {

	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound compound) {

	}
}
