package net.backshield.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Integracao OPCIONAL com o Backpacked (MrCrayfish). Usa reflexao em
 * com.mrcrayfish.backpacked.BackpackHelper#getFirstBackpackStack(Player) (conferido no jar 3.0.5),
 * entao o Backpacked nao e dependencia de compilacao nem obrigatoria. Qualquer falha desliga a integracao.
 */
@Environment(EnvType.CLIENT)
final class BackpackCompat {

    private static final Logger LOGGER = LoggerFactory.getLogger("backshield");

    private static boolean initialized;
    private static Method getFirstBackpackStack;

    private BackpackCompat() {
    }

    static boolean hasBackpack(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity player)) {
            return false;
        }
        if (!initialized) {
            initialized = true;
            if (FabricLoader.getInstance().isModLoaded("backpacked")) {
                try {
                    Class<?> helper = Class.forName("com.mrcrayfish.backpacked.BackpackHelper");
                    getFirstBackpackStack = helper.getMethod("getFirstBackpackStack", PlayerEntity.class);
                } catch (Throwable t) {
                    LOGGER.warn("Backpacked is installed but its API could not be found; backpack offset disabled", t);
                }
            }
        }
        if (getFirstBackpackStack == null) {
            return false;
        }
        try {
            return getFirstBackpackStack.invoke(null, player) instanceof ItemStack stack && !stack.isEmpty();
        } catch (Throwable t) {
            LOGGER.warn("Backpacked integration failed; backpack offset disabled", t);
            getFirstBackpackStack = null;
            return false;
        }
    }
}
