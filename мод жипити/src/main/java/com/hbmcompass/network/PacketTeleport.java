package com.hbmcompass.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketTeleport implements IMessage {
    private int x, y, z;

    public PacketTeleport() {}

    public PacketTeleport(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.x = buf.readInt();
        this.y = buf.readInt();
        this.z = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.x);
        buf.writeInt(this.y);
        buf.writeInt(this.z);
    }

    public static class Handler implements IMessageHandler<PacketTeleport, IMessage> {
        @Override
        public IMessage onMessage(PacketTeleport message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (!player.canUseCommand(2, "tp") && !player.isCreative()) {
                    player.sendMessage(new TextComponentString(TextFormatting.RED + "[HBM Компас] Для телепортації потрібні права оператора або Креатив!"));
                    return;
                }
                player.world.getChunkFromBlockCoords(new BlockPos(message.x, 64, message.z));
                int safeY = player.world.getTopSolidOrLiquidBlock(new BlockPos(message.x, 80, message.z)).getY() + 1;
                if (safeY <= 1) safeY = 75;
                player.connection.setPlayerLocation(message.x + 0.5D, safeY, message.z + 0.5D, player.rotationYaw, player.rotationPitch);
                player.sendMessage(new TextComponentString(TextFormatting.AQUA + "[HBM Компас] Телепортовано до [" + message.x + ", " + safeY + ", " + message.z + "]"));
            });
            return null;
        }
    }
}
