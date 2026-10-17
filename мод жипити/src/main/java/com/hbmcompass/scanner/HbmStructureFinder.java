package com.hbmcompass.scanner;

import com.hbmcompass.HbmCompassMod;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Random;

public class HbmStructureFinder {

    public static class SearchResult {
        public final boolean found;
        public final int x;
        public final int y;
        public final int z;
        public final String structureId;
        public final String displayName;

        public SearchResult(boolean found, int x, int y, int z, String structureId, String displayName) {
            this.found = found;
            this.x = x;
            this.y = y;
            this.z = z;
            this.structureId = structureId;
            this.displayName = displayName;
        }
    }

    /**
     * Виконує надшвидке спіральне сканування чанків навколо гравця,
     * використовуючи точну формулу генератора випадкових чисел Minecraft (Chunk Seed)
     * та налаштування шансів із GeneralConfig твого HBM Extended 3.0.3.
     */
    public static SearchResult findNearestStructure(World world, BlockPos origin, String targetId, int maxChunkRadius) {
        long worldSeed = world.getSeed();
        int startChunkX = origin.getX() >> 4;
        int startChunkZ = origin.getZ() >> 4;

        StructureRegistry.StructureEntry targetEntry = StructureRegistry.getById(targetId);
        String targetDisplay = targetEntry != null ? targetEntry.displayName : targetId;

        // Зчитуємо реальні шанси спавну з GeneralConfig HBM (якщо мод завантажений)
        StructureChances chances = new StructureChances();

        // Спіральний обхід чанків від центру (0..maxChunkRadius)
        for (int r = 0; r <= maxChunkRadius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue;

                    int cx = startChunkX + dx;
                    int cz = startChunkZ + dz;

                    String matchedType = simulateHbmChunk(world, worldSeed, cx, cz, targetId, chances);
                    if (matchedType != null) {
                        int blockX = (cx << 4) + 8;
                        int blockZ = (cz << 4) + 8;
                        int blockY = world.getHeight(blockX, blockZ);
                        if (blockY <= 0) blockY = 70;
                        StructureRegistry.StructureEntry foundEntry = StructureRegistry.getById(matchedType);
                        String disp = (foundEntry != null && !"ALL".equals(targetId)) ? targetDisplay :
                                (foundEntry != null ? foundEntry.displayName : targetDisplay);
                        return new SearchResult(true, blockX, blockY, blockZ, matchedType, disp);
                    }
                }
            }
        }

        return new SearchResult(false, origin.getX(), origin.getY(), origin.getZ(), targetId, targetDisplay);
    }

    private static String simulateHbmChunk(World world, long worldSeed, int chunkX, int chunkZ, String filterId, StructureChances chances) {
        // Формула FML GameRegistry.generateWorld для першого/другого генератора в списку:
        Random fmlRand = new Random(worldSeed);
        long xSeed = fmlRand.nextLong() >> 2 + 1L;
        long zSeed = fmlRand.nextLong() >> 2 + 1L;
        long chunkSeed = (xSeed * chunkX + zSeed * chunkZ) ^ worldSeed;

        Random rand = new Random(chunkSeed);
        Biome biome = world.getBiomeForCoordsBody(new BlockPos((chunkX << 4) + 8, 64, (chunkZ << 4) + 8));

        // Перевірка основних структур HbmWorldGen у точному порядку викликів NTM 1.12.2
        boolean isOcean = (biome == Biomes.OCEAN || biome == Biomes.DEEP_OCEAN || biome == Biomes.FROZEN_OCEAN);
        boolean isDesert = (biome == Biomes.DESERT || biome == Biomes.DESERT_HILLS);
        boolean isPlainsOrForest = (biome == Biomes.PLAINS || biome == Biomes.FOREST || biome == Biomes.EXTREME_HILLS || biome == Biomes.TAIGA);
        boolean isCold = biome.getDefaultTemperature() < 0.25F;
        boolean isJungle = (biome == Biomes.JUNGLE || biome == Biomes.JUNGLE_HILLS);

        // Симуляція послідовності rand.nextInt(chance) == 0
        if (!isOcean) {
            if (checkRoll(rand, chances.radioStructure) && matches(filterId, "Radio")) return "Radio";
            if (checkRoll(rand, chances.antennaStructure) && matches(filterId, "Relay")) return "Relay";
            if (isPlainsOrForest && checkRoll(rand, chances.atomStructure) && matches(filterId, "Nuclear")) return "Nuclear";
            if (isDesert && checkRoll(rand, chances.desertAtom) && matches(filterId, "DesertAtom")) return "DesertAtom";
            if (checkRoll(rand, chances.vertibirdStructure) && matches(filterId, "Vertibird")) return "Vertibird";
            if (checkRoll(rand, chances.dungeonStructure) && matches(filterId, "Library")) return "Library";
            if (checkRoll(rand, chances.relayStructure) && matches(filterId, "Relay")) return "Relay";
            if (checkRoll(rand, chances.satelliteStructure) && matches(filterId, "Satellite")) return "Satellite";
            if (checkRoll(rand, chances.bunkerStructure) && matches(filterId, "Bunker")) return "Bunker";
            if (checkRoll(rand, chances.siloStructure) && matches(filterId, "Silo")) return "Silo";
            if (checkRoll(rand, chances.factoryStructure) && matches(filterId, "Factory")) return "Factory";
            if (checkRoll(rand, chances.dudStructure) && matches(filterId, "Dud")) return "Dud";
            if (checkRoll(rand, chances.spaceshipStructure) && matches(filterId, "Spaceship")) return "Spaceship";
            if (isDesert && checkRoll(rand, chances.barrelStructure) && matches(filterId, "Barrel")) return "Barrel";
            if (checkRoll(rand, chances.broadcaster) && matches(filterId, "Broadcaster")) return "Broadcaster";
            if (checkRoll(rand, chances.vault) && matches(filterId, "Vault")) return "Vault";
            if (isCold && checkRoll(rand, chances.capsule) && matches(filterId, "Capsule")) return "Capsule";
            if (isCold && checkRoll(rand, chances.arctic) && matches(filterId, "Arctic")) return "Arctic";
            if (isJungle && checkRoll(rand, chances.jungle) && matches(filterId, "Jungle")) return "Jungle";
            if (isDesert && checkRoll(rand, chances.pyramid) && matches(filterId, "Pyramid")) return "Pyramid";
            if (checkRoll(rand, chances.meteorDungeon) && matches(filterId, "MeteorDungeon")) return "MeteorDungeon";
            if (checkRoll(rand, chances.sellafield) && matches(filterId, "Sellafield")) return "Sellafield";
        } else {
            if (checkRoll(rand, chances.oilRig) && matches(filterId, "OilRig")) return "OilRig";
        }

        return null;
    }

    private static boolean checkRoll(Random rand, int chance) {
        return chance > 0 && rand.nextInt(chance) == 0;
    }

    private static boolean matches(String filterId, String currentType) {
        if (filterId == null || "ALL".equalsIgnoreCase(filterId)) return true;
        return filterId.toLowerCase(Locale.ROOT).contains(currentType.toLowerCase(Locale.ROOT))
                || currentType.toLowerCase(Locale.ROOT).contains(filterId.toLowerCase(Locale.ROOT));
    }

    private static class StructureChances {
        int radioStructure = 500;
        int antennaStructure = 250;
        int atomStructure = 500;
        int desertAtom = 500;
        int vertibirdStructure = 500;
        int dungeonStructure = 64;
        int relayStructure = 500;
        int satelliteStructure = 500;
        int bunkerStructure = 1000;
        int siloStructure = 1000;
        int factoryStructure = 1000;
        int dudStructure = 500;
        int spaceshipStructure = 1000;
        int barrelStructure = 500;
        int broadcaster = 5000;
        int vault = 2500;
        int capsule = 100;
        int arctic = 500;
        int jungle = 2000;
        int pyramid = 4000;
        int meteorDungeon = 15000;
        int sellafield = 4000;
        int oilRig = 1500;

        StructureChances() {
            try {
                Class<?> cfg = Class.forName("com.hbm.config.GeneralConfig");
                radioStructure = getIntField(cfg, "radioStructure", radioStructure);
                antennaStructure = getIntField(cfg, "antennaStructure", antennaStructure);
                atomStructure = getIntField(cfg, "atomStructure", atomStructure);
                vertibirdStructure = getIntField(cfg, "vertibirdStructure", vertibirdStructure);
                dungeonStructure = getIntField(cfg, "dungeonStructure", dungeonStructure);
                relayStructure = getIntField(cfg, "relayStructure", relayStructure);
                satelliteStructure = getIntField(cfg, "satelliteStructure", satelliteStructure);
                bunkerStructure = getIntField(cfg, "bunkerStructure", bunkerStructure);
                siloStructure = getIntField(cfg, "siloStructure", siloStructure);
                factoryStructure = getIntField(cfg, "factoryStructure", factoryStructure);
                dudStructure = getIntField(cfg, "dudStructure", dudStructure);
                spaceshipStructure = getIntField(cfg, "spaceshipStructure", spaceshipStructure);
                barrelStructure = getIntField(cfg, "barrelStructure", barrelStructure);
                broadcaster = getIntField(cfg, "enableBroadcaster", broadcaster);
                vault = getIntField(cfg, "vaultfreq", vault);
            } catch (Throwable ignored) {}
        }

        private int getIntField(Class<?> cls, String name, int def) {
            try {
                Field f = cls.getDeclaredField(name);
                f.setAccessible(true);
                int val = f.getInt(null);
                return val > 0 ? val : def;
            } catch (Throwable t) {
                return def;
            }
        }
    }
}
