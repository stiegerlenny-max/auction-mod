package de.timgioh.auction;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Live-Anzeige oben links, solange eine Auktion läuft. */
public final class AuctionHud {
    private AuctionHud() {}

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!AuctionManager.isActive() || mc.options.hudHidden || mc.currentScreen instanceof AuctionScreen) return;

        TextRenderer tr = mc.textRenderer;
        long left = AuctionManager.remainingSeconds();
        String time = String.format("%02d:%02d", left / 60, left % 60);

        List<String> lines = new ArrayList<>();
        lines.add("§6§ltimgioh auction");
        lines.add("§f" + AuctionManager.describe());
        lines.add("§7Mindestgebot: §e" + Money.format(AuctionManager.getMinPrice()));
        lines.add("§7Zeit: " + (left <= 10 ? "§c" : "§a") + time);

        List<Map.Entry<String, Double>> rank = AuctionManager.ranking();
        if (rank.isEmpty()) {
            lines.add("§8Noch keine Gebote");
        } else {
            lines.add("§7Gebote:");
            for (int i = 0; i < Math.min(5, rank.size()); i++) {
                Map.Entry<String, Double> e = rank.get(i);
                lines.add("§f" + (i + 1) + ". " + e.getKey() + " §e" + Money.format(e.getValue()));
            }
        }

        int w = 0;
        for (String l : lines) w = Math.max(w, tr.getWidth(l));
        int x = 6, y = 6, h = lines.size() * 11 + 6;
        ctx.fill(x - 3, y - 3, x + w + 3, y - 3 + h, 0x99000000);
        for (String l : lines) {
            ctx.drawTextWithShadow(tr, l, x, y, 0xFFFFFFFF);
            y += 11;
        }
    }
}
