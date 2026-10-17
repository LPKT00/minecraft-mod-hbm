package com.hbmcompass.scanner;

import com.hbmcompass.HbmCompassMod;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class StructureRegistry {

    public static class StructureEntry {
        public final String id;
        public final String displayName;
        public final String category;

        public StructureEntry(String id, String displayName, String category) {
            this.id = id;
            this.displayName = displayName;
            this.category = category;
        }
    }

    private static final Map<String, StructureEntry> STRUCTURES = new LinkedHashMap<>();
    private static Class<?> hbmWorldGenClass = null;
    private static Object hbmWorldGenInstance = null;

    // Словник зрозумілих українських/англійських назв для структур HBM Extended 3.0.3
    private static final Map<String, String> FRIENDLY_NAMES = new HashMap<>();

    static {
        FRIENDLY_NAMES.put("ALL", "Усі структури HBM (Найближча будь-яка)");
        FRIENDLY_NAMES.put("Bunker", "Підземний бункер (Bunker)");
        FRIENDLY_NAMES.put("Silo", "Ракетна шахта (Missile Silo)");
        FRIENDLY_NAMES.put("Relay", "Ретранслятор / Антена (Relay / Antenna)");
        FRIENDLY_NAMES.put("Satellite", "Супутникова тарілка (Satellite Dish)");
        FRIENDLY_NAMES.put("Spaceship", "Космічний корабель (Spaceship)");
        FRIENDLY_NAMES.put("Barrel", "Сховище ядерних відходів (Nuclear Waste Barrel)");
        FRIENDLY_NAMES.put("Factory", "Покинутий завод (Factory)");
        FRIENDLY_NAMES.put("Nuclear", "Атомна електростанція (Nuclear Plant)");
        FRIENDLY_NAMES.put("Dud", "Нерозірвана ядерна бомба (Balefire / Nuke Dud)");
        FRIENDLY_NAMES.put("Vault", "Сховище / Сейф-бункер (Vault)");
        FRIENDLY_NAMES.put("Radio", "Радіостанція (Radio Station)");
        FRIENDLY_NAMES.put("Geiger", "Будиночок з лічильником Гейгера (Shed)");
        FRIENDLY_NAMES.put("DesertAtom", "Пустельний атомний тест (Desert Atom)");
        FRIENDLY_NAMES.put("Library", "Зруйнована бібліотека (Library Dungeon)");
        FRIENDLY_NAMES.put("Vertibird", "Збитий гвинтокрил (Crashed Vertibird)");
        FRIENDLY_NAMES.put("CrashedBomb", "Уламки бомбардувальника (Crashed Plane)");
        FRIENDLY_NAMES.put("Boat", "Покинутий корабель (Abandoned Boat)");
        FRIENDLY_NAMES.put("Lighthouse", "Маяк (Lighthouse)");
        FRIENDLY_NAMES.put("Broadcaster", "Передавач перешкод (Broadcaster)");
        FRIENDLY_NAMES.put("Minefield", "Мінне поле (Minefield)");
        FRIENDLY_NAMES.put("RadHotspot", "Радіаційна зона (Radiation Hotspot)");
        FRIENDLY_NAMES.put("Geyser", "Токсичний гейзер (Geyser)");
        FRIENDLY_NAMES.put("Capsule", "Космічна капсула Союз (Soyuz Capsule)");
        FRIENDLY_NAMES.put("Arctic", "Арктична дослідницька база (Arctic Lab)");
        FRIENDLY_NAMES.put("Jungle", "Храм у джунглях (Jungle Dungeon)");
        FRIENDLY_NAMES.put("Pyramid", "Стародавня піраміда (Ancient Pyramid)");
        FRIENDLY_NAMES.put("MeteorDungeon", "Метеоритний данж (Meteor Dungeon)");
        FRIENDLY_NAMES.put("OilRig", "Нафтова вишка в океані (Oil Rig)");
        FRIENDLY_NAMES.put("Sellafield", "Кратер Селлафілд (Sellafield Crater)");
    }

    public static void init() {
        register("ALL", FRIENDLY_NAMES.get("ALL"), "Загальне");

        // 1. Пошук класу генератора HBM (підтримка NTM Extended 3.0.3 та Alcater/Drillgon форків)
        String[] possibleGeneratorClasses = {
                "com.hbm.lib.HbmWorldGen",
                "com.hbm.world.WorldGeneratorHBM",
                "com.hbm.main.WorldGenerator"
        };

        for (String clsName : possibleGeneratorClasses) {
            try {
                hbmWorldGenClass = Class.forName(clsName);
                try {
                    hbmWorldGenInstance = hbmWorldGenClass.newInstance();
                } catch (Throwable ignored) {}
                HbmCompassMod.LOGGER.info("Знайдено генератор світу HBM: " + clsName);
                scanWorldGenBytecode(clsName);
                break;
            } catch (ClassNotFoundException ignored) {}
        }

        // 2. Додаємо повний базовий набір структур NTM Extended 1.12.2-3.0.3, щоб меню завжди мало чіткий список
        String[] defaultKeys = {
                "Bunker", "Silo", "Factory", "Nuclear", "Vault", "Dud",
                "Satellite", "Relay", "Radio", "Broadcaster", "Spaceship",
                "Vertibird", "Library", "DesertAtom", "Barrel", "Capsule",
                "Arctic", "Jungle", "Pyramid", "MeteorDungeon", "OilRig", "Sellafield"
        };
        for (String key : defaultKeys) {
            if (!STRUCTURES.containsKey(key)) {
                register(key, FRIENDLY_NAMES.getOrDefault(key, key), "Базові структури HBM");
            }
        }

        HbmCompassMod.LOGGER.info("HBM Explorer's Compass зареєстрував " + STRUCTURES.size() + " цілей для пошуку.");
    }

    private static void scanWorldGenBytecode(String className) {
        try {
            String resourcePath = className.replace('.', '/') + ".class";
            InputStream is = StructureRegistry.class.getClassLoader().getResourceAsStream(resourcePath);
            if (is == null) return;

            ClassReader reader = new ClassReader(is);
            reader.accept(new ClassVisitor(Opcodes.ASM5) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                    return new MethodVisitor(Opcodes.ASM5) {
                        @Override
                        public void visitTypeInsn(int opcode, String type) {
                            if (opcode == Opcodes.NEW && type.startsWith("com/hbm/world/")) {
                                String simpleName = type.substring(type.lastIndexOf('/') + 1);
                                if (!simpleName.contains("Ore") && !simpleName.contains("Cave") && !simpleName.contains("Layer")) {
                                    addDiscoveredClass(simpleName);
                                }
                            }
                        }

                        @Override
                        public void visitMethodInsn(int opcode, String owner, String mName, String mDesc, boolean itf) {
                            if (owner.startsWith("com/hbm/world/")) {
                                String simpleOwner = owner.substring(owner.lastIndexOf('/') + 1);
                                if (!simpleOwner.contains("Ore") && !simpleOwner.contains("Biome")) {
                                    addDiscoveredClass(simpleOwner + "." + mName);
                                }
                            }
                        }
                    };
                }
            }, 0);
            is.close();
        } catch (Throwable t) {
            HbmCompassMod.LOGGER.warn("Не вдалося просканувати байткод HbmWorldGen: " + t.getMessage());
        }
    }

    private static void addDiscoveredClass(String rawId) {
        String cleanKey = rawId;
        if (cleanKey.contains(".")) {
            cleanKey = cleanKey.substring(0, cleanKey.indexOf('.'));
        }
        for (Map.Entry<String, String> entry : FRIENDLY_NAMES.entrySet()) {
            if (rawId.toLowerCase(Locale.ROOT).contains(entry.getKey().toLowerCase(Locale.ROOT))) {
                register(entry.getKey(), entry.getValue(), "Авто-сканер NTM");
                return;
            }
        }
        if (!STRUCTURES.containsKey(cleanKey)) {
            register(cleanKey, "Структура NTM: " + cleanKey, "Авто-сканер NTM");
        }
    }

    public static void register(String id, String displayName, String category) {
        if (!STRUCTURES.containsKey(id)) {
            STRUCTURES.put(id, new StructureEntry(id, displayName, category));
        }
    }

    public static List<StructureEntry> getAllStructures() {
        return new ArrayList<>(STRUCTURES.values());
    }

    public static StructureEntry getById(String id) {
        return STRUCTURES.getOrDefault(id, STRUCTURES.get("ALL"));
    }

    public static Class<?> getHbmWorldGenClass() {
        return hbmWorldGenClass;
    }

    public static Object getHbmWorldGenInstance() {
        return hbmWorldGenInstance;
    }
}
