package com.hbmcompass;

import com.hbmcompass.client.ClientProxy;
import com.hbmcompass.common.CommonProxy;
import com.hbmcompass.item.ItemHbmCompass;
import com.hbmcompass.network.PacketSearchStructure;
import com.hbmcompass.network.PacketSyncResult;
import com.hbmcompass.network.PacketTeleport;
import com.hbmcompass.scanner.StructureRegistry;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = HbmCompassMod.MODID, name = HbmCompassMod.NAME, version = HbmCompassMod.VERSION, dependencies = "after:hbm")
@Mod.EventBusSubscriber
public class HbmCompassMod {
    public static final String MODID = "hbmcompass";
    public static final String NAME = "HBM Explorer's Compass";
    public static final String VERSION = "1.0";

    public static final Logger LOGGER = LogManager.getLogger(MODID);

    @Mod.Instance(MODID)
    public static HbmCompassMod instance;

    @SidedProxy(clientSide = "com.hbmcompass.client.ClientProxy", serverSide = "com.hbmcompass.common.CommonProxy")
    public static CommonProxy proxy;

    public static SimpleNetworkWrapper NETWORK;
    public static Item HBM_COMPASS;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
        int packetId = 0;
        NETWORK.registerMessage(PacketSearchStructure.Handler.class, PacketSearchStructure.class, packetId++, Side.SERVER);
        NETWORK.registerMessage(PacketSyncResult.Handler.class, PacketSyncResult.class, packetId++, Side.CLIENT);
        NETWORK.registerMessage(PacketTeleport.Handler.class, PacketTeleport.class, packetId++, Side.SERVER);

        HBM_COMPASS = new ItemHbmCompass();
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        StructureRegistry.init();
        proxy.postInit(event);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(HBM_COMPASS);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(HBM_COMPASS, 0, new ModelResourceLocation(HBM_COMPASS.getRegistryName(), "inventory"));
    }
}
