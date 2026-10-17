package net.backshield.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuracao SOMENTE visual (cliente). Arquivo: config/backshield-client.json
 *
 * Sistema de coordenadas: o escudo e desenhado no referencial do TORSO do jogador
 * (o mesmo que o modelo usa, por isso acompanha agachar/nadar/animacoes de outros mods):
 *   x = lateral, y = para BAIXO a partir da linha dos ombros, z = para TRAS (0.125 = superficie das costas).
 * Unidades em blocos (1 pixel de textura = 0.0625).
 *
 * Ajuste ao vivo no jogo: /backshield set <campo> <valor>, /backshield show, /backshield reload, /backshield reset
 */
public final class BackShieldConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("backshield");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("backshield-client.json");

    private static volatile BackShieldConfig current = new BackShieldConfig();

    // --- Posicao padrao (sem mochila) ---
    /** Escala do escudo (1.0 = tamanho da mao; torso tem 0.5 de largura). */
    public float scale = 0.55F;
    /** Altura do CENTRO do escudo, a partir dos ombros (torso vai de 0 a 0.75). */
    public float height = 0.375F;
    /** Deslocamento lateral. */
    public float offsetX = 0.0F;
    /** Folga entre as costas e o centro do escudo. */
    public float distance = 0.07F;
    /** Rotacoes extras em graus. */
    public float rotX = 0.0F;
    public float rotY = 0.0F;
    public float rotZ = 0.0F;

    // --- Equipamentos no peitoral ---
    /** Distancia extra quando ha peitoral/armadura (o modelo da armadura e "inchado"). */
    public float chestExtra = 0.07F;
    /** Distancia extra quando o peitoral e uma elytra. */
    public float elytraExtra = 0.10F;
    /** Distancia extra durante o voo com elytra (asas abertas). */
    public float flyingExtra = 0.06F;

    // --- Mochila (Backpacked) ---
    /** Quanto o escudo sobe quando ha mochila equipada. */
    public float backpackUp = 0.22F;
    /** Quanto o escudo vai para tras quando ha mochila equipada. */
    public float backpackBack = 0.25F;
    /** Inclinacao extra (graus, eixo X) quando ha mochila. */
    public float backpackTilt = 0.0F;

    public static BackShieldConfig get() {
        return current;
    }

    public static void load() {
        if (Files.exists(FILE)) {
            try (Reader reader = Files.newBufferedReader(FILE)) {
                BackShieldConfig loaded = GSON.fromJson(reader, BackShieldConfig.class);
                if (loaded != null) {
                    current = loaded;
                }
            } catch (Exception e) {
                LOGGER.warn("Could not read {} - using defaults", FILE, e);
                current = new BackShieldConfig();
            }
        }
        save(); // grava campos novos/valores padrao
    }

    public static void save() {
        try (Writer writer = Files.newBufferedWriter(FILE)) {
            GSON.toJson(current, writer);
        } catch (IOException e) {
            LOGGER.warn("Could not write {}", FILE, e);
        }
    }

    public static void reset() {
        current = new BackShieldConfig();
        save();
    }

    public static List<String> fieldNames() {
        List<String> names = new ArrayList<>();
        for (Field field : BackShieldConfig.class.getDeclaredFields()) {
            if (isConfigField(field)) {
                names.add(field.getName());
            }
        }
        return names;
    }

    /** @return true se o campo existe e foi alterado. */
    public static boolean set(String name, float value) {
        for (Field field : BackShieldConfig.class.getDeclaredFields()) {
            if (isConfigField(field) && field.getName().equals(name)) {
                try {
                    field.setFloat(current, value);
                    save();
                    return true;
                } catch (IllegalAccessException e) {
                    return false;
                }
            }
        }
        return false;
    }

    public static List<String> describe() {
        List<String> lines = new ArrayList<>();
        for (Field field : BackShieldConfig.class.getDeclaredFields()) {
            if (isConfigField(field)) {
                try {
                    lines.add(field.getName() + " = " + field.getFloat(current));
                } catch (IllegalAccessException ignored) {
                }
            }
        }
        return lines;
    }

    private static boolean isConfigField(Field field) {
        return field.getType() == float.class && !Modifier.isStatic(field.getModifiers());
    }
}
