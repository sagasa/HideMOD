package hide.guns.entiry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.google.common.collect.Lists;
import helper.HideDamage;
import helper.HideDamage.HideDamageCase;
import helper.HideMath;
import helper.HideMathHelper;
import helper.RayTracer;
import hide.common.entity.EntityDebugLine;
import hide.guns.math.BlockPenetrationData;
import hide.guns.network.PacketHit;
import hide.types.effects.Explosion;
import hide.types.guns.ProjectileData;
import hide.types.util.DataView.ViewCache;
import hide.ux.SoundHandler;
import hide.ux.network.PacketHideParticle;
import hidemod.HideMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import org.apache.commons.lang3.RandomUtils;

/**
 * 銃弾・砲弾・爆弾など投擲系以外の全てこのクラスで追加
 */
public class EntityBullet extends Entity implements IEntityAdditionalSpawnData {

    /*
     * ダメージ関係はすべてサーバーサイドで判別 弾が消失した場合DWで位置と通知 ブロックに当たった音はここで出す クライアントサイドで音関連を実行する
     * 毎Tick通った範囲にプレイヤーがいないか見る…改善の余地あり
     *
     * 角度はモーションから計算
     */

    private Random random = new Random();

    public EntityBullet(World worldIn) {
        super(worldIn);
        this.setSize(0.5F, 0.5F);
    }

    private float addTick;
    public EntityPlayer Shooter;
    private ViewCache<ProjectileData> dataView;

    private String toolName;

    /**
     * 当たったエンティティのリスト 多段ヒット防止用
     */
    private List<Entity> AlreadyHit;
    /**
     * 貫通力
     */
    private float bulletPower;
    /**
     * 飛距離
     */
    private double FlyingDistance = 0;
    /**
     * Tickスキップ保管用
     */
    private long lastWorldTick = 0;

    /**
     * 消えるまでの距離
     */
    public float life = 0;

    public EntityBullet(ViewCache<ProjectileData> viewCache, EntityPlayer shooter, boolean isADS, float offset, double x,
                        double y, double z, float yaw, float pitch, String name) {
        this(shooter.world);
        //	System.out.println("off "+offset);
        toolName = name;
        dataView = viewCache;
        Shooter = shooter;
        AlreadyHit = new ArrayList<>();
        addTick = offset;
        bulletPower = dataView.get(ProjectileData.BulletPower);
        life = dataView.get(ProjectileData.Range);

        setLocationAndAngles(x, y, z, yaw, pitch);
        setPosition(posX, posY, posZ);
        // 精度の概念
        float accuracy = dataView.get(ProjectileData.Accuracy);
        if (shooter.isSneaking()) {
            accuracy = dataView.get(ProjectileData.AccuracySneak, accuracy);
        }
        if (isADS) {
            accuracy = dataView.get(ProjectileData.AccuracyADS, accuracy);
        }

        double d = (float) Math.toDegrees(Math.atan(accuracy / 50));
        //縦拡散
        double d2 = rand.nextDouble() * d;
        rotationPitch = (float) HideMath.normal(rotationPitch, d2);
        rotationYaw = (float) HideMath.normal(rotationYaw, d - d2);
        // 向いている方向をモーションに
        motionX = -Math.sin(Math.toRadians(rotationYaw)) * Math.cos(Math.toRadians(rotationPitch));
        motionZ = Math.cos(Math.toRadians(rotationYaw)) * Math.cos(Math.toRadians(rotationPitch));
        motionY = -Math.sin(Math.toRadians(rotationPitch));
        float f2 = MathHelper.sqrt(motionX * motionX + motionZ * motionZ + motionY * motionY) / dataView.get(ProjectileData.BulletSpeed);
        motionX /= f2;
        motionZ /= f2;
        motionY /= f2;
        onUpdate(addTick);
        addTick = 0;
        setPosition(posX, posY, posZ);
    }

    @Override
    public boolean isInRangeToRender3d(double x, double y, double z) {
        double d0 = this.posX - x;
        double d2 = this.posZ - z;
        double d3 = d0 * d0 + d2 * d2;
        return this.isInRangeToRenderDist(d3);
    }

    @Override
    public boolean isInRangeToRenderDist(double distance) {
        return distance < 80000;
    }

    @Override
    public void onUpdate() {
        if (lastWorldTick == 0) {
            lastWorldTick = world.getTotalWorldTime() - 1;
        }
        onUpdate(world.getTotalWorldTime() - lastWorldTick);
        lastWorldTick = world.getTotalWorldTime();
    }

    private void onUpdate(float tick) {

        this.lastTickPosX = this.posX;
        this.lastTickPosY = this.posY;
        this.lastTickPosZ = this.posZ;
        this.posX += (this.motionX * tick);
        this.posY += (this.motionY * tick);
        this.posZ += (this.motionZ * tick);

        if (!this.world.isRemote) {
            // 削除計算
            life -= tick;
            if (life <= 0) {
                setDead();
            }
            ServerUpdate();
        } else {
            ClientUpdate();
        }
        this.setPosition(this.posX, this.posY, this.posZ);
    }

    private void lostBulletPower(float power) {
        final float lost = Math.max(0, power / dataView.get(ProjectileData.BulletPower));
        final float velLost = 1 - MathHelper.sqrt(lost);
        bulletPower = Math.max(0, bulletPower - power);
        //System.out.println(velLost + " " + lost + " " + power);
        Vec3d vec = new Vec3d(motionX,motionY,motionZ);
        vec = vec.scale(velLost);

        double d = vec.lengthVector();
        double rand = lost/4*d;
        vec=vec.addVector(HideMath.randome(0,rand), HideMath.randome(0,rand),HideMath.randome(0,rand));
        vec = vec.normalize().scale(d);
        motionX = vec.x;
        motionY = vec.y;
        motionZ = vec.z;

        markVelocityChanged();
    }

    private void ServerUpdate() {
        /** 前のtickの位置ベクトル */
        final Vec3d lvo = new Vec3d(lastTickPosX, lastTickPosY, lastTickPosZ);
        /** 今のtickの位置ベクトル */
        final Vec3d lvt = new Vec3d(posX, posY, posZ);

        /** 弾が消失した位置 */
        Vec3d lve = lvt;
        /**前のbulletPower*/
        final float prevBulletPower = bulletPower;

        // ブロックとの衝突
        for (RayTraceResult res : RayTracer.getHitBlocks(world, lvo, lvt)) {
            final IBlockState state = world.getBlockState(res.getBlockPos());
            final Block block = state.getBlock();
            final float dist = (float) res.hitInfo;
            //貫通力計算
            final float thickness = BlockPenetrationData.getThickness(state, 1) * dist;

            //パーティクル
            spawnBlockHitParticle(res.hitVec, state, lvo.subtract(lvt), 1);
            if (bulletPower <= thickness) {
                //非貫通処理
                lve = res.hitVec.add(lvt.subtract(lvo).normalize().scale(dist * bulletPower / thickness));
            }
            lostBulletPower(thickness);
        }
        //if (gunData.HIT_IGNORING_ARMOR)
        //	damagesource.setDamageBypassesArmor();

        //EntityDebugLine debug = new EntityDebugLine(world, Lists.newArrayList(Vec3d.ZERO, lvend.subtract(lvo)), 0.8f, 0.2f, 0.2f);
        //debug.setPosition(lvo.x, lvo.y, lvo.z);
        //world.spawnEntity(debug);
        // Entityとの衝突
        Iterator<RayTraceResult> HitIterator = RayTracer.getHitEntity(this, world, lvo, lve, addTick).iterator();
        while (HitIterator.hasNext()) {
            RayTraceResult hit = HitIterator.next();
            Entity e = hit.entityHit;
            // 多段ヒット防止
            if (!AlreadyHit.contains(e)) {
                // ダメージが与えられる対象なら
                if (e instanceof EntityLivingBase && ((EntityLivingBase) e).deathTime == 0 && !(e.equals(Shooter))) {
                    //System.out.println("Shooter "+Shooter+" HitEntity "+e);
                    // ダメージを算出

                    DamageTarget dt = DamageTarget.getTarget(e);
                    // Missなら戻る
                    if (dt == DamageTarget.Miss) {
                        continue;
                    }

                    final double dist = lvo.distanceTo(hit.hitVec);
                    //BulletPower
                    float hitPower = bulletPower <= 0 ? HideMath.completion(prevBulletPower, bulletPower, (float) (dist / lvo.distanceTo(lve))) : bulletPower;

                    if (hitPower <= 0) {
                        return;
                    }

                    float damage = getFinalHitDamage(dt, (float) (FlyingDistance + dist), hitPower);

                    boolean isHeadShot = false;
                    // ヘッドショットを判定するEntity
                    if (e instanceof EntityPlayer || e instanceof EntityZombie || e instanceof EntityPigZombie
                            || e instanceof EntitySkeleton || e instanceof EntityVillager) {
                        isHeadShot = RayTracer.isHeadShot(e, lvo, lve, addTick);
                        if (isHeadShot) {
                            damage *= dataView.get(ProjectileData.HeadMultiplier);
                        }
                    }
                    // ダメージを与える
                    DamageSource damagesource = new HideDamage(HideDamageCase.GUN_BULLET, Shooter, toolName);
                    boolean isDamaged = HideDamage.Attack((EntityLivingBase) e, (HideDamage) damagesource, damage);

                    // 爆発があるなら
                    //explode(hit.hitVec, dataView.getData(ProjectileData.Explosion));//TODO

                    // ヒットマーク
                    if (Shooter instanceof EntityPlayerMP && isDamaged && damage > 0.5) {
                        HideMod.NETWORK.sendTo(new PacketHit(isHeadShot), (EntityPlayerMP) Shooter);
                    }

                    //worldserver.spawnParticle(EnumParticleTypes.BLOCK_CRACK, true, lve.x, lve.y, lve.z, 5, 0.0, 0.0, 0.0, 1.0, Block.getStateId(blockState));

                    Vec3d vec = lvo.subtract(lvt).normalize().scale(0.7);
                    HideMod.NETWORK.sendToAll(new PacketHideParticle(EnumParticleTypes.BLOCK_CRACK.getParticleID(), 152)
                            .spawnBox((float) hit.hitVec.x, (float) hit.hitVec.y, (float) hit.hitVec.z, 0, 0, 0, 6)
                            .setVelecity(0.02f)
                            .setVelecity3((float) vec.x, (float) vec.y, (float) vec.z));

                    lostBulletPower(10);
                    AlreadyHit.add(e);
                    // もしこの衝突で消えたなら
                    if (bulletPower <= 0) {
                        lve = hit.hitVec;
                        break;
                    }
                }
            }
        }
        FlyingDistance += lvo.distanceTo(lve);

        // 消去処理
        if (bulletPower <= 0 || life <= 0) {
            if (life <= 0 && dataView.get(ProjectileData.ExplosionOnTimeout)) {
                explode(lve, dataView.getData(ProjectileData.Explosion));
            } else
                explode(lve, dataView.getData(ProjectileData.Explosion));
            setDead();

        }
    }

    private void spawnBlockHitParticle(Vec3d pos, IBlockState blockState, Vec3d vec, float scale) {
        vec = vec.normalize().scale(0.3);
        vec = vec.normalize().scale(0.3);
        HideMod.NETWORK.sendToAll(new PacketHideParticle(EnumParticleTypes.BLOCK_CRACK.getParticleID(), Block.getStateId(blockState))
                .spawnBox((float) pos.x, (float) pos.y, (float) pos.z, 0, 0, 0, (int) (8 * scale))
                .setVelecity(0.02f)
                .setVelecity3((float) vec.x, (float) vec.y, (float) vec.z));
        vec = vec.scale(0.3);
        HideMod.NETWORK.sendToAll(new PacketHideParticle(EnumParticleTypes.CLOUD.getParticleID())
                .spawnBox((float) pos.x, (float) pos.y, (float) pos.z, 0, 0, 0, (int) (5 * scale))
                .setVelecity(0.02f)
                .setVelecity3((float) vec.x, (float) vec.y, (float) vec.z));
    }

    private void ClientUpdate() {

    }

    @Override
    protected void entityInit() {

    }

    private void explode(Vec3d endPos, ViewCache<Explosion> explosion) {
        // 爆発があるなら
        float range = explosion.get(Explosion.Range);
        if (range > 0) {
            List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(this, new AxisAlignedBB(endPos.x - range,
                    endPos.y - range, endPos.z - range, endPos.x + range, endPos.y + range, endPos.z + range));

            for (Entity e : list) {
                if (!(e instanceof EntityLivingBase)) {
                    continue;
                }
                // 障害物の判定
                double dis = -1;
                System.out.println(world.rayTraceBlocks(endPos, new Vec3d(e.posX, e.posY, e.posZ), false, true, false));
                if (world.rayTraceBlocks(endPos, new Vec3d(e.posX, e.posY, e.posZ), false, true, false) == null) {
                    dis = new Vec3d(e.posX, e.posY, e.posZ).distanceTo(endPos);
                }
                if (world.rayTraceBlocks(endPos, new Vec3d(e.posX, e.posY + e.getEyeHeight(), e.posZ), false, true,
                        false) == null) {
                    double d = new Vec3d(e.posX, e.posY + e.getEyeHeight(), e.posZ).distanceTo(endPos);
                    dis = dis > d ? d : dis;
                }
                if (dis > range) {
                    continue;
                }
                // ダメージを算出
                float damage = 1;
                if (dis != -1) {
                    if (e instanceof EntityPlayer) {
                        damage = explosion.get(Explosion.DamagePlayer).get((float) dis);
                    } else if (e instanceof EntityLiving) {
                        damage = explosion.get(Explosion.DamageLiving).get((float) dis);
                    }
                }

                DamageSource damagesource = new HideDamage(HideDamageCase.GUN_Explosion, Shooter, toolName);
                // ダメージを与える
                HideDamage.Attack((EntityLivingBase) e, (HideDamage) damagesource, damage);
            }
            // サウンド
            SoundHandler.broadcastSound(Shooter, endPos.x, endPos.y, endPos.z, explosion.getData(Explosion.Sound), false);
            // TODO エフェクト サウンドの対象を座標に変えなきゃ
        }
    }

    /**
     * 直撃ダメージ算出
     */
    private float getFinalHitDamage(DamageTarget target, float distance, float power) {
        float damage = 0;
        switch (target) {
            case Living:
                damage = dataView.get(ProjectileData.DamageLiving).get(distance);
                break;
            case Player:
                damage = dataView.get(ProjectileData.DamagePlayer).get(distance);
                break;
            case Vehicle:
                break;
            case Aircraft:
                break;
            default:
                break;
        }
        //System.out.println(damage+" "+(power / dataView.get(ProjectileData.BulletPower))+" "+power);
        return damage * power / dataView.get(ProjectileData.BulletPower);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        setDead();
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {

    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        buffer.writeFloat(rotationYaw);
        buffer.writeFloat(rotationPitch);
        buffer.writeDouble(motionX);
        buffer.writeDouble(motionY);
        buffer.writeDouble(motionZ);
        buffer.writeDouble(world.getTotalWorldTime());
    }

    @Override
    public void readSpawnData(ByteBuf buffer) {
        rotationYaw = buffer.readFloat();
        rotationPitch = buffer.readFloat();
        motionX = buffer.readDouble();
        motionY = buffer.readDouble();
        motionZ = buffer.readDouble();
        float tickt = (float) (world.getTotalWorldTime() - buffer.readDouble());
        tickt = tickt < 0 ? 0 : tickt;
        onUpdate(tickt);
    }

    public enum DamageTarget {
        Player, Living, Vehicle, Aircraft, Miss;

        public static DamageTarget getTarget(Entity entity) {
            if (entity instanceof EntityPlayer) {
                return Player;
            } else if (entity instanceof EntityLivingBase) {
                return Living;
            }
            // TODO 判別
            return Miss;
        }
    }
}
