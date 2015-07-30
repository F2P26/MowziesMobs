package com.bobmowzie.mowziesmobs.common.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.EntityAIAttackOnCollide;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

/**
 * Created by jnad325 on 7/9/15.
 */

public class EntityTribesman extends MMEntityBase {
    protected boolean attacking = false;
    protected int timeSinceAttack = 0;
    public EntityTribesman(World world) {
        super(world);
        tasks.addTask(2, new EntityAIAttackOnCollide(this, EntityPlayer.class, 0.5D, false));
        this.targetTasks.addTask(2, new EntityAINearestAttackableTarget(this, EntityPlayer.class, 0, true));
        setMask(0);
    }

    @Override
    public boolean attackEntityAsMob(Entity entity) {
        attacking = false;
        timeSinceAttack = 0;
        return entity.attackEntityFrom(DamageSource.causeMobDamage(this), 2);
    }

    protected void updateAttackAI() {
        if (timeSinceAttack < 50) timeSinceAttack ++;
        if (getAttackTarget() != null)
        {
            if (targetDistance > 7) getNavigator().tryMoveToXYZ(getAttackTarget().posX, getAttackTarget().posY, getAttackTarget().posZ, 0.6);
            else
            {
                if (attacking == false) circleEntity(getAttackTarget(), 7, 0.3f, true, 0);
            }
            if (rand.nextInt(40) == 0 && timeSinceAttack == 50)
            {
                attacking = true;
                getNavigator().tryMoveToEntityLiving(getAttackTarget(), 0.5);
            }
        }
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        updateAttackAI();
    }

    protected void entityInit()
    {
        super.entityInit();
        dataWatcher.addObject(29, 0);
    }

    public int getMask()
    {
        return dataWatcher.getWatchableObjectInt(29);
    }

    public void setMask(Integer type)
    {
        dataWatcher.updateObject(29, type);
    }

    public void writeEntityToNBT(NBTTagCompound compound)
    {
        super.writeEntityToNBT(compound);
        compound.setInteger("mask", getMask());
    }

    public void readEntityFromNBT(NBTTagCompound compound)
    {
        super.readEntityFromNBT(compound);
        setMask(compound.getInteger("mask"));
    }
}