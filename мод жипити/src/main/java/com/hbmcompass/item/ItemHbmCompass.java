package com.hbmcompass.item;

import com.hbmcompass.HbmCompassMod;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class ItemHbmCompass extends Item {

    public ItemHbmCompass() {
        this.setRegistryName(new ResourceLocation(HbmCompassMod.MODID, "hbm_compass"));
        this.setUnlocalizedName(HbmCompassMod.MODID + ".hbm_compass");
        this.setMaxStackSize(1);
        this.setCreativeTab(CreativeTabs.TOOLS);

        this.addPropertyOverride(new ResourceLocation("angle"), new IItemPropertyGetter() {
            @SideOnly(Side.CLIENT)
            double rotation;
            @SideOnly(Side.CLIENT)
            double rota;
            @SideOnly(Side.CLIENT)
            long lastUpdateTick;

            @SideOnly(Side.CLIENT)
            @Override
            public float apply(ItemStack stack, @Nullable World worldIn, @Nullable EntityLivingBase entityIn) {
                if (entityIn == null && !stack.isOnItemFrame()) {
                    return 0.0F;
                } else {
                    boolean flag = entityIn != null;
                    Entity entity = flag ? entityIn : stack.getItemFrame();
                    if (worldIn == null) {
                        worldIn = entity.world;
                    }

                    double d0;
                    NBTTagCompound tag = stack.getTagCompound();
                    if (tag != null && tag.getBoolean("Found") && worldIn.provider.getDimension() == 0) {
                        double d1 = flag ? (double) entity.rotationYaw : this.getFrameRotation((EntityItemFrame) entity);
                        d1 = MathHelper.positiveModulo(d1 / 360.0D, 1.0D);
                        double d2 = this.getTargetAngle(worldIn, entity, tag.getInteger("TargetX"), tag.getInteger("TargetZ")) / (Math.PI * 2D);
                        d0 = 0.5D - (d1 - 0.25D - d2);
                    } else {
                        d0 = Math.random();
                    }

                    if (flag) {
                        d0 = this.wobble(worldIn, d0);
                    }

                    return MathHelper.positiveModulo((float) d0, 1.0F);
                }
            }

            @SideOnly(Side.CLIENT)
            private double wobble(World worldIn, double amount) {
                if (worldIn.getTotalWorldTime() != this.lastUpdateTick) {
                    this.lastUpdateTick = worldIn.getTotalWorldTime();
                    double d0 = amount - this.rotation;
                    d0 = MathHelper.positiveModulo(d0 + 0.5D, 1.0D) - 0.5D;
                    this.rota += d0 * 0.1D;
                    this.rota *= 0.8D;
                    this.rotation = MathHelper.positiveModulo(this.rotation + this.rota, 1.0D);
                }
                return this.rotation;
            }

            @SideOnly(Side.CLIENT)
            private double getFrameRotation(EntityItemFrame frame) {
                return (double) MathHelper.wrapDegrees(180 + frame.facingDirection.getHorizontalIndex() * 90);
            }

            @SideOnly(Side.CLIENT)
            private double getTargetAngle(World world, Entity entity, int targetX, int targetZ) {
                return Math.atan2((double) targetZ + 0.5D - entity.posZ, (double) targetX + 0.5D - entity.posX);
            }
        });
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        ItemStack stack = playerIn.getHeldItem(handIn);
        if (worldIn.isRemote && handIn == EnumHand.MAIN_HAND) {
            HbmCompassMod.proxy.openCompassGui(playerIn);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GRAY + "ПКМ — відкрити меню пошуку структур HBM NTM");
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasKey("StructureDisplay")) {
            tooltip.add(TextFormatting.GOLD + "Ціль: " + TextFormatting.WHITE + tag.getString("StructureDisplay"));
            if (tag.getBoolean("Found")) {
                tooltip.add(TextFormatting.GREEN + "Координати: X=" + tag.getInteger("TargetX") + ", Z=" + tag.getInteger("TargetZ"));
            } else {
                tooltip.add(TextFormatting.RED + "Статус: Не знайдено / Очікування");
            }
        }
    }
}
