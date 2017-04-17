package com.bobmowzie.mowziesmobs.server.entity.effects;

import com.bobmowzie.mowziesmobs.server.entity.foliaath.EntityFoliaath;
import com.bobmowzie.mowziesmobs.server.potion.PotionHandler;
import com.bobmowzie.mowziesmobs.server.sound.MMSounds;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockOldLeaf;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.lwjgl.Sys;
import scala.Option;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Josh on 4/14/2017.
 */
public class EntityBoulder extends Entity {
    int blockId;
    private EntityLivingBase caster;
    private int size;
    private boolean travelling;
    private static float SPEED = 1.5f;
    public IBlockState storedBlock;
    private int damage;
    private static final DataParameter<Optional<IBlockState>> BLOCK_STATE = EntityDataManager.createKey(EntityBoulder.class, DataSerializers.OPTIONAL_BLOCK_STATE);
    private static final DataParameter<Boolean> SHOULD_EXPLODE = EntityDataManager.createKey(EntityBoulder.class, DataSerializers.BOOLEAN);
    public int timeUntilDeath = 1200;
    public float animationOffset = 0;
    private List<Entity> ridingEntities = new ArrayList<Entity>();

    public EntityBoulder(World world) {
        super(world);
        setSize(1F, 1F);
        travelling = false;
        damage = 6;
        animationOffset = (float) (Math.random() * 8);
    }

    public EntityBoulder(World world, EntityLivingBase caster, int x, int y, int z, int size, IBlockState block) {
        this(world);
        this.caster = caster;
        this.setPosition(x + 0.5F, y + 1, z + 0.5F);
        if (!world.getEntitiesWithinAABB(EntityBoulder.class, getEntityBoundingBox()).isEmpty()) setDead();
        if (!world.isRemote && block != null) {
            Material mat = block.getMaterial();
            if (mat == Material.GRASS || mat == Material.GROUND) setBlock(Blocks.DIRT.getDefaultState());
            else if (mat == Material.ROCK) setBlock(Blocks.STONE.getDefaultState());
            else if (mat == Material.CLAY) setBlock(Blocks.CLAY.getDefaultState());
            else if (mat == Material.SAND) setBlock(Blocks.SANDSTONE.getDefaultState());
            else setDead();
        }
        if (world.collidesWithAnyBlock(getEntityBoundingBox())) setDead();
    }

    @Override
    protected void entityInit() {
        getDataManager().register(BLOCK_STATE, Optional.of(Blocks.DIRT.getDefaultState()));
        getDataManager().register(SHOULD_EXPLODE, false);
    }

    @Override
    public boolean isSilent() {
        return false;
    }

    @Override
    public void onUpdate() {
        if (storedBlock == null) storedBlock = getBlock();
        if (getShouldExplode()) explode();
        super.onUpdate();
        move(MoverType.SELF, motionX, motionY, motionZ);
        if (ridingEntities != null) ridingEntities.clear();
        List<Entity> onTopOfEntities = world.getEntitiesWithinAABBExcludingEntity(this, getEntityBoundingBox().move(new Vec3d(0, 0.5, 0)).expand(0.6,0.5,0.6));
        for (Entity entity : onTopOfEntities) {
            if (entity != null && entity.canBeCollidedWith() && !(entity instanceof EntityBoulder) && entity.posY >= this.posY + 0.2) ridingEntities.add(entity);
        }
        if (travelling){
            for (Entity entity:ridingEntities) {
                entity.move(MoverType.SHULKER_BOX, motionX, motionY, motionZ);
            }
        }

        if (ticksExisted < 4) {
            List<Entity> popUpEntities = world.getEntitiesWithinAABBExcludingEntity(this, getEntityBoundingBox());
            for (Entity entity:popUpEntities) {
                if (entity.canBeCollidedWith() && !(entity instanceof EntityBoulder)) entity.move(MoverType.SHULKER_BOX, 0, 0.9 * (-Math.pow(5, -ticksExisted) + 1), 0);
            }
        }
        List<EntityLivingBase> entitiesHit = getEntityLivingBaseNearby(1.7);
        if (travelling && !entitiesHit.isEmpty()) {
            for (Entity entity : entitiesHit) {
                if (world.isRemote) continue;
                if (entity == caster) continue;
                if (ridingEntities.contains(entity)) continue;
                if (caster instanceof EntityPlayer)  entity.attackEntityFrom(DamageSource.causePlayerDamage((EntityPlayer) caster), damage);
                else entity.attackEntityFrom(DamageSource.causeMobDamage(caster), damage);
                if (!isDead) setShouldExplode(true);
            }
        }
        List<EntityBoulder> bouldersHit = world.getEntitiesWithinAABB(EntityBoulder.class, getEntityBoundingBox().expand(0.2, 0.2, 0.2).move(new Vec3d(motionX, motionY, motionZ).normalize().scale(0.5)));
        if (travelling && !bouldersHit.isEmpty()) {
            for (EntityBoulder entity : bouldersHit) {
                if (!entity.travelling) {
                    entity.hitByEntity(this);
                    explode();
                }
            }
        }

        if (travelling && world.collidesWithAnyBlock(getEntityBoundingBox().expand(0.1,0.1,0.1))) explode();

        blockId = Block.getStateId(storedBlock);

        if (ticksExisted == 1) {
            for (int i = 0; i < 20; i++) {
                Vec3d particlePos = new Vec3d(Math.random() * 1.3, 0, 0);
                particlePos = particlePos.rotateYaw((float)(Math.random() * 2 * Math.PI));
                world.spawnParticle(EnumParticleTypes.BLOCK_CRACK, posX + particlePos.xCoord, posY - 1, posZ + particlePos.zCoord, particlePos.xCoord, 2, particlePos.zCoord, blockId);
            }
            playSound(MMSounds.EFFECT_GEOMANCY_SMALL_CRASH, 1.5f, 1.3f);
            playSound(MMSounds.EFFECT_GEOMANCY_MAGIC_SMALL, 1.5f, 1);
            EntityRing ring = new EntityRing(world, (float)posX, (float)posY - 0.9f, (float)posZ, new Vec3d(0,1,0), 7, 0.83f, 1, 0.39f, 1f, 1.5f, false);
            world.spawnEntity(ring);
        }

        int dripTick = ticksExisted - 2;
        int dripNumber = (int)(6 * Math.pow(1.07, -ticksExisted));
        if (dripNumber >= 1 && dripTick > 0) {
            dripNumber *= Math.random();
            for (int i = 0; i < dripNumber; i++) {
                Vec3d particlePos = new Vec3d(Math.random() * 0.6, 0, 0);
                particlePos = particlePos.rotateYaw((float)(Math.random() * 2 * Math.PI));
                world.spawnParticle(EnumParticleTypes.BLOCK_CRACK, posX + particlePos.xCoord, posY, posZ + particlePos.zCoord, 0, -1, 0, blockId);
            }
        }
        timeUntilDeath--;
        if (timeUntilDeath < 0) this.explode();
    }

    private void explode() {
        setDead();
        for (int i = 0; i < 40; i++) {
            Vec3d particlePos = new Vec3d(Math.random() * 0.7, 0, 0);
            particlePos = particlePos.rotateYaw((float)(Math.random() * 2 * Math.PI));
            particlePos = particlePos.rotatePitch((float)(Math.random() * 2 * Math.PI));
            world.spawnParticle(EnumParticleTypes.BLOCK_CRACK, posX + particlePos.xCoord, posY + 0.5 + particlePos.yCoord, posZ + particlePos.zCoord, particlePos.xCoord, particlePos.yCoord, particlePos.zCoord, blockId);
        }
        playSound(MMSounds.EFFECT_GEOMANCY_MAGIC_SMALL, 1.5f, 0.7f);
        playSound(MMSounds.EFFECT_GEOMANCY_BREAK, 1.5f, 1f);
    }

    @Nullable
    @Override
    public AxisAlignedBB getCollisionBoundingBox() {
        return getEntityBoundingBox();
    }

    public IBlockState getBlock() {
        return getDataManager().get(BLOCK_STATE).get();
    }

    public void setBlock(IBlockState block) {
        getDataManager().set(BLOCK_STATE, Optional.of(block));
        this.storedBlock = block;
    }

    public boolean getShouldExplode() {
        return getDataManager().get(SHOULD_EXPLODE);
    }

    public void setShouldExplode(boolean shouldExplode) {
        getDataManager().set(SHOULD_EXPLODE, shouldExplode);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        Optional<IBlockState> blockOption = Optional.of(getBlock());
        if (blockOption.isPresent()) {
            compound.setTag("block", NBTUtil.writeBlockState(new NBTTagCompound(), blockOption.get()));
        }
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        IBlockState blockState = NBTUtil.readBlockState((NBTTagCompound) compound.getTag("block"));
        setBlock(blockState);
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean hitByEntity(Entity entityIn) {
        if (ticksExisted > 3) {
            if (entityIn instanceof EntityPlayer
                    && ((EntityPlayer) entityIn).inventory.getCurrentItem() == ItemStack.EMPTY
                    && ((EntityPlayer) entityIn).isPotionActive(PotionHandler.INSTANCE.geomancy)) {
                EntityPlayer player = (EntityPlayer) entityIn;
                if (ridingEntities.contains(player)) {
                    Vec3d lateralLookVec = Vec3d.fromPitchYaw(0, player.rotationYaw).normalize();
                    motionX = SPEED * 0.5 * lateralLookVec.xCoord;
                    motionZ = SPEED * 0.5 * lateralLookVec.zCoord;
                } else {
                    motionX = SPEED * 0.5 * player.getLookVec().xCoord;
                    motionY = SPEED * 0.5 * player.getLookVec().yCoord;
                    motionZ = SPEED * 0.5 * player.getLookVec().zCoord;
                }
            }
            else if (entityIn instanceof EntityBoulder && ((EntityBoulder) entityIn).travelling) {
                EntityBoulder boulder = (EntityBoulder)entityIn;
                Vec3d thisPos = new Vec3d(posX, posY, posZ);
                Vec3d boulderPos = new Vec3d(boulder.posX, boulder.posY, boulder.posZ);
                Vec3d velVec = thisPos.subtract(boulderPos).normalize();
                motionX = SPEED * 0.5 * velVec.xCoord;
                motionY = SPEED * 0.5 * velVec.yCoord;
                motionZ = SPEED * 0.5 * velVec.zCoord;
            }
            else {
                return super.hitByEntity(entityIn);
            }
            if (!travelling) timeUntilDeath = 60;
            travelling = true;

            playSound(MMSounds.EFFECT_GEOMANCY_HIT_SMALL, 1.5f, 1f);
            playSound(MMSounds.EFFECT_GEOMANCY_MAGIC_SMALL, 1.5f, 0.9f);
            Vec3d ringOffset = new Vec3d(motionX, motionY, motionZ).normalize().scale(-1);
            EntityRing ring = new EntityRing(entityIn.world, (float) posX + (float) ringOffset.xCoord, (float) posY + 0.5f + (float) ringOffset.yCoord, (float) posZ + (float) ringOffset.zCoord, ringOffset.normalize(), 5, 0.83f, 1, 0.39f, 1f, 1.5f, false);
            entityIn.world.spawnEntity(ring);
        }
        return super.hitByEntity(entityIn);
    }

    public List<EntityLivingBase> getEntityLivingBaseNearby(double radius) {
        return getEntitiesNearby(EntityLivingBase.class, radius);
    }

    public <T extends Entity> List<T> getEntitiesNearby(Class<T> entityClass, double r) {
        return world.getEntitiesWithinAABB(entityClass, getEntityBoundingBox().expand(r, r, r), e -> e != this && getDistanceToEntity(e) <= r);
    }

    protected void repelEntities(float radius) {
        List<EntityLivingBase> nearbyEntities = getEntityLivingBaseNearby(radius);
        for (Entity entity : nearbyEntities) {
            double angle = (getAngleBetweenEntities(this, entity) + 90) * Math.PI / 180;
            entity.motionX = -0.1 * Math.cos(angle);
            entity.motionZ = -0.1 * Math.sin(angle);
        }
    }

    public double getAngleBetweenEntities(Entity first, Entity second) {
        return Math.atan2(second.posZ - first.posZ, second.posX - first.posX) * (180 / Math.PI) + 90;
    }

    @Override
    public void playSound(SoundEvent soundIn, float volume, float pitch) {
        super.playSound(soundIn, volume, pitch + (float)Math.random() * 0.5f - 0.25f);
    }
}
