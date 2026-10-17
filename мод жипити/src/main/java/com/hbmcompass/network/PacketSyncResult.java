package com.hbmcompass.network;

import com.hbmcompass.client.GuiHbmCompass;
import com.hbmcompass.item.ItemHbmCompass;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PacketSyncResult implements IMessage {
    public boolean found;
    public int x, y, z;
    public String structureId;
    public String displayName;

    public PacketSyncResult() {}

    public PacketSyncResult(boolean found, int x, int y, int z, String structureId, String displayName) {
        this.found = found;
        this.x = x;
        this.y = y;
        this.z = z;
        this.structureId = structureId;
        this.displayName = displayName;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.found = buf.readBoolean();
        this.x = buf.readInt();
        this.y = buf.readInt();
        this.z = buf.readInt();
        this.structureId = ByteBufUtils.readUTF8String(buf);
        this.displayName = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(this.found);
        buf.writeInt(this.x);
        buf.writeInt(this.y);
        buf.writeInt(this.z);
        ByteBufUtils.writeUTF8String(buf, this.structureId);
        ByteBufUtils.writeUTF8String(buf, this.displayName);
    }

    public static class Handler implements IMessageHandler<PacketSyncResult, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketSyncResult message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                EntityPlayer player = Minecraft.getMinecraft().player;
                if (player == null) return;
                ItemStack held = player.getHeldItemMainhand();
                if (held.isEmpty() || !(held.getItem() instanceof ItemHbmCompass)) {
                    held = player.getHeldItemOffhand();
                }
                if (!held.isEmpty() && held.getItem() instanceof ItemHbmCompass) {
                    NBTTagCompound tag = held.getTagCompound();
                    if (tag == null) {
                        tag = new NBTTagCompound();
                        held.setTagCompound(tag);
                    }
                    tag.setString("StructureId", message.structureId);
                    tag.setString("StructureDisplay", message.displayName);
                    tag.setBoolean("Found", message.found);
                    tag.setInteger("TargetX", message.x);
                    tag.setInteger("TargetY", message.y);
                    tag.setInteger("TargetZ", message.z);
                }

                if (Minecraft.getMinecraft().currentScreen instanceof GuiHbmCompass) {
                    ((GuiHbmCompass) Minecraft.getMinecraft().currentScreen).onSearchResult(message);
                }
            });
            return null;
        }
    }
}
