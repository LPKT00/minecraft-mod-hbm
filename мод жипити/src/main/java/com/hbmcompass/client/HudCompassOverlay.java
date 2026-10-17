package com.hbmcompass.client;

import com.hbmcompass.item.ItemHbmCompass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class HudCompassOverlay extends Gui {

    @SubscribeEvent
    public void onRenderHud(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null || mc.gameSettings.showDebugInfo) return;

        ItemStack stack = player.getHeldItemMainhand();
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemHbmCompass)) {
            stack = player.getHeldItemOffhand();
        }
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemHbmCompass)) return;

        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey("StructureDisplay")) return;

        String name = tag.getString("StructureDisplay");
        boolean found = tag.getBoolean("Found");

        drawRect(6, 6, 230, 44, 0xAA101610);
        mc.fontRenderer.drawStringWithShadow("§a[HBM Radar] §f" + name, 12, 11, 0xFFFFFF);

        if (found) {
            int tx = tag.getInteger("TargetX");
            int tz = tag.getInteger("TargetZ");
            int dist = (int) Math.sqrt(player.getDistanceSq(tx, player.posY, tz));
            mc.fontRenderer.drawStringWithShadow("§eКоординати: §fX: " + tx + " | Z: " + tz, 12, 22, 0xFFFFFF);
            mc.fontRenderer.drawStringWithShadow("§bДистанція: §f" + dist + " м", 12, 32, 0xFFFFFF);
        } else {
            mc.fontRenderer.drawStringWithShadow("§cЦіль не знайдена (ПКМ для пошуку)", 12, 24, 0xFFFFFF);
        }
    }
}
