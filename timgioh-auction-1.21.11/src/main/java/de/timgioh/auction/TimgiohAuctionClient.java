package de.timgioh.auction;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class TimgiohAuctionClient implements ClientModInitializer {
    public static final String MOD_ID = "timgioh_auction";

    private static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        AuctionConfig.load();

        // GLFW kennt keine Umlaute: Die Taste "Ö" auf dem deutschen Layout ist die physische
        // Semikolon-Taste (GLFW_KEY_SEMICOLON).
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.timgioh_auction.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_SEMICOLON,
                KeyBinding.Category.create(Identifier.of(MOD_ID, "main"))));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    client.setScreen(new AuctionScreen());
                }
            }
            AuctionManager.tick(client);
        });

        // Server-Meldungen (z. B. "Max hat dir 500 gesendet") kommen als Game-Messages an.
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) {
                AuctionManager.onChat(message.getString());
            }
        });

        HudElementRegistry.attachElementAfter(VanillaHudElements.CHAT, Identifier.of(MOD_ID, "auction_hud"), AuctionHud::render);
    }
}
