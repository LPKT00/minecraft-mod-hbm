package com.hbmcompass.network;

import com.hbmcompass.HbmCompassMod;
import com.hbmcompass.item.ItemHbmCompass;
import com.hbmcompass.scanner.HbmStructureFinder;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketSearchStructure implements IMessage {
    private String structureId;
    private int maxRadiusChunks;

    public PacketSearchStructure() {}

    public PacketSearchStructure(String structureId, int maxRadiusChunks) {
        this.structureId = structureId;
        this.maxRadiusChunks = maxRadiusChunks;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.structureId = ByteBufUtils.readUTF8String(buf);
        this.maxRadiusChunks = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.structureId);
        buf.writeInt(this.maxRadiusChunks);
    }

    public static class Handler implements IMessageHandler<PacketSearchStructure, IMessage> {
        @Override
        public IMessage onMessage(PacketSearchStructure message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                ItemStack held = player.getHeldItemMainhand();
                if (held.isEmpty() || !(held.getItem() instanceof ItemHbmCompass)) {
                    held = player.getHeldItemOffhand();
                }
                if (held.isEmpty() || !(held.getItem() instanceof ItemHbmCompass)) {
                    return;
                }

                int radius = Math.min(Math.max(message.maxRadiusChunks, 32), 500);
                HbmStructureFinder.SearchResult res = HbmStructureFinder.findNearestStructure(
                        player.world, player.getPosition(), message.structureId, radius
                );

                NBTTagCompound tag = held.getTagCompound();
                if (tag == null) {
                    tag = new NBTTagCompound();
                    held.setTagCompound(tag);
                }

                tag.setString("StructureId", res.structureId);
                tag.setString("StructureDisplay", res.displayName);
                tag.setBoolean("Found", res.found);
                tag.setInteger("TargetX", res.x);
                tag.setInteger("TargetY", res.y);
                tag.setInteger("TargetZ", res.z);

                if (res.found) {
                    int dist = (int) Math.sqrt(player.getDistanceSq(res.x, player.posY, res.z));
                    player.sendMessage(new TextComponentString(
                            TextFormatting.GREEN + "[HBM Компас] " + TextFormatting.WHITE + "Знайдено: " +
                            TextFormatting.GOLD + res.displayName + TextFormatting.GRAY +
                            " [X: " + res.x + ", Z: " + res.z + "] (" + dist + " м)"
                    ));
                } else {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.RED + "[HBM Компас] " + TextFormatting.WHITE +
                            "Структуру не знайдено в радіусі " + (radius * 16) + " блоків."
                    ));
                }

                HbmCompassMod.NETWORK.sendTo(
                        new PacketSyncResult(res.found, res.x, res.y, res.z, res.structureId, res.displayName),
                        player
                );
            });
            return null;
        }
    }
}
