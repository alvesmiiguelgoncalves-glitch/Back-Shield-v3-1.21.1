package net.backshield.client;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.emi.trinkets.api.client.TrinketRendererRegistry;
import net.backshield.config.BackShieldConfig;
import net.backshield.network.ShieldActionPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.command.CommandSource;
import net.minecraft.item.Item;
import net.minecraft.item.ShieldItem;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class BackShieldClient implements ClientModInitializer {

    private static KeyBinding toggleKey;
    private static KeyBinding blockKey;
    private static boolean blocking;

    @Override
    public void onInitializeClient() {
        BackShieldConfig.load();

        // Registra o renderizador para todo ShieldItem (inclusive de outros mods) depois que todos os itens existem.
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            BackShieldRenderer renderer = new BackShieldRenderer();
            for (Item item : Registries.ITEM) {
                if (item instanceof ShieldItem) {
                    TrinketRendererRegistry.registerRenderer(item, renderer);
                }
            }
        });

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.backshield.toggle",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.backshield.key"));
        blockKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.backshield.block",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "category.backshield.key"));

        ClientTickEvents.END_CLIENT_TICK.register(BackShieldClient::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> blocking = false);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("backshield")
                        .then(ClientCommandManager.literal("show").executes(ctx -> {
                            BackShieldConfig.describe().forEach(line -> ctx.getSource().sendFeedback(Text.literal(line)));
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("reload").executes(ctx -> {
                            BackShieldConfig.load();
                            ctx.getSource().sendFeedback(Text.literal("backshield-client.json reloaded"));
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("reset").executes(ctx -> {
                            BackShieldConfig.reset();
                            ctx.getSource().sendFeedback(Text.literal("Back Shield settings reset to defaults"));
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("set")
                                .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                        .suggests((ctx, builder) -> CommandSource.suggestMatching(BackShieldConfig.fieldNames(), builder))
                                        .then(ClientCommandManager.argument("value", FloatArgumentType.floatArg())
                                                .executes(ctx -> {
                                                    String name = StringArgumentType.getString(ctx, "name");
                                                    float value = FloatArgumentType.getFloat(ctx, "value");
                                                    boolean ok = BackShieldConfig.set(name, value);
                                                    ctx.getSource().sendFeedback(Text.literal(ok
                                                            ? name + " = " + value
                                                            : "Unknown field: " + name));
                                                    return ok ? 1 : 0;
                                                }))))));
    }

    private static void tick(MinecraftClient client) {
        if (client.player == null) {
            blocking = false;
            return;
        }
        boolean canSend = ClientPlayNetworking.canSend(ShieldActionPayload.ID);

        while (toggleKey.wasPressed()) {
            if (canSend && client.currentScreen == null) {
                ClientPlayNetworking.send(new ShieldActionPayload(ShieldActionPayload.Action.TOGGLE));
            }
        }

        // Defesa: enquanto a tecla esta segura e nenhuma tela esta aberta.
        boolean wantBlock = canSend && blockKey.isPressed() && client.currentScreen == null && !client.player.isSpectator();
        if (wantBlock && !blocking) {
            blocking = true;
            ClientPlayNetworking.send(new ShieldActionPayload(ShieldActionPayload.Action.BLOCK_START));
        } else if (!wantBlock && blocking) {
            blocking = false;
            if (canSend) {
                ClientPlayNetworking.send(new ShieldActionPayload(ShieldActionPayload.Action.BLOCK_STOP));
            }
        }
    }
}
