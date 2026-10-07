package de.timgioh.auction;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Map;

/** Auktionsmenü (Taste Ö). */
public class AuctionScreen extends Screen {
    private static final int PANEL_W = 360;
    private static final int PANEL_H = 250;

    // bleiben erhalten, wenn das Menü geschlossen/neu geöffnet wird
    private static int selectedSlot = -1;
    private static String lastPrice = "";
    private static String lastDuration = "";
    private static boolean announce = true;

    private TextFieldWidget priceField;
    private TextFieldWidget durationField;
    private ButtonWidget announceBtn;
    private String status = "";
    private int left, top, gridX, gridY;

    public AuctionScreen() {
        super(Text.literal("timgioh auction"));
    }

    @Override
    protected void init() {
        left = (width - PANEL_W) / 2;
        top = (height - PANEL_H) / 2;
        gridX = left + 10;
        gridY = top + 28;
        int rx = left + 190, rw = 160;

        priceField = new TextFieldWidget(textRenderer, rx, top + 38, rw, 18, Text.literal("Preis"));
        priceField.setMaxLength(20);
        priceField.setText(lastPrice);
        priceField.setTextPredicate(s -> s.matches("[0-9.,kKmMbB]*"));
        addDrawableChild(priceField);

        durationField = new TextFieldWidget(textRenderer, rx, top + 72, rw, 18, Text.literal("Dauer"));
        durationField.setMaxLength(5);
        durationField.setText(lastDuration.isEmpty() ? String.valueOf(AuctionConfig.defaultDuration()) : lastDuration);
        durationField.setTextPredicate(s -> s.matches("[0-9]*"));
        addDrawableChild(durationField);

        announceBtn = ButtonWidget.builder(announceText(), b -> {
            announce = !announce;
            b.setMessage(announceText());
        }).dimensions(rx, top + 94, rw, 20).build();
        addDrawableChild(announceBtn);

        addDrawableChild(ButtonWidget.builder(Text.literal("§aStart"), b -> startAuction())
                .dimensions(rx, top + 118, 78, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("§cStopp"), b -> {
            AuctionManager.stop(MinecraftClient.getInstance());
            status = "";
        }).dimensions(rx + 82, top + 118, 78, 20).build());
    }

    private Text announceText() {
        return Text.literal("Im Chat ankündigen: " + (announce ? "§aAn" : "§cAus"));
    }

    private void startAuction() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (AuctionManager.isActive()) {
            status = "Es läuft bereits eine Auktion.";
            return;
        }
        ItemStack stack = selectedSlot >= 0 ? mc.player.getInventory().getStack(selectedSlot) : ItemStack.EMPTY;
        if (stack.isEmpty()) {
            status = "Bitte zuerst ein Item auswählen.";
            return;
        }
        double price = Money.parse(priceField.getText());
        if (price <= 0) {
            status = "Bitte einen gültigen Preis eingeben.";
            return;
        }
        int seconds;
        try {
            seconds = Integer.parseInt(durationField.getText());
        } catch (NumberFormatException e) {
            seconds = -1;
        }
        if (seconds < 5 || seconds > 86400) {
            status = "Dauer: 5 bis 86400 Sekunden.";
            return;
        }
        AuctionManager.start(mc, stack.copy(), price, seconds, announce);
        status = "";
    }

    // ---- Slot-Geometrie: Hotbar = 0-8, Inventar = 9-35 ----
    private int slotX(int i) {
        return gridX + (i < 9 ? i : (i - 9) % 9) * 18;
    }

    private int slotY(int i) {
        return i < 9 ? gridY + 3 * 18 + 4 : gridY + ((i - 9) / 9) * 18;
    }

    private int slotAt(double mx, double my) {
        for (int i = 0; i < 36; i++) {
            int x = slotX(i), y = slotY(i);
            if (mx >= x && mx < x + 16 && my >= y && my < y + 16) return i;
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int slot = slotAt(click.x(), click.y());
        if (slot >= 0 && click.button() == 0) {
            selectedSlot = (selectedSlot == slot) ? -1 : slot;
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    private static void border(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    /** Hintergrund + Panel + Inventar; die Widgets werden danach von super.render() gezeichnet. */
    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.renderBackground(ctx, mouseX, mouseY, delta);
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        ctx.fill(left, top, left + PANEL_W, top + PANEL_H, 0xE0151520);
        border(ctx, left, top, PANEL_W, PANEL_H, 0xFFFFAA00);
        String title2 = "§6§ltimgioh auction §r§7– HugoSMP";
        ctx.drawTextWithShadow(textRenderer, title2, width / 2 - textRenderer.getWidth(title2) / 2, top + 8, 0xFFFFFFFF);

        ctx.drawTextWithShadow(textRenderer, "Item auswählen:", gridX, top + 18, 0xFFAAAAAA);
        for (int i = 0; i < 36; i++) {
            int x = slotX(i), y = slotY(i);
            ctx.fill(x, y, x + 16, y + 16, 0xFF3A3A3A);
            ItemStack s = mc.player.getInventory().getStack(i);
            if (!s.isEmpty()) {
                ctx.drawItem(s, x, y);
                ctx.drawStackOverlay(textRenderer, s, x, y);
            }
            if (i == selectedSlot) border(ctx, x - 1, y - 1, 18, 18, 0xFFFFD700);
        }

        ItemStack sel = selectedSlot >= 0 ? mc.player.getInventory().getStack(selectedSlot) : ItemStack.EMPTY;
        String selText = sel.isEmpty() ? "§8Kein Item gewählt" : "§f" + sel.getCount() + "x " + sel.getName().getString();
        ctx.drawTextWithShadow(textRenderer, "Auswahl: " + selText, gridX, gridY + 3 * 18 + 4 + 22, 0xFFFFFFFF);

        int rx = left + 190;
        ctx.drawTextWithShadow(textRenderer, "Mindestpreis (z. B. 5000, 5k, 1.5m):", rx, top + 27, 0xFFAAAAAA);
        ctx.drawTextWithShadow(textRenderer, "Dauer in Sekunden:", rx, top + 61, 0xFFAAAAAA);
        if (!status.isEmpty()) ctx.drawTextWithShadow(textRenderer, "§c" + status, rx, top + 142, 0xFFFFFFFF);

        // Live-Bereich
        int ly = top + 156;
        ctx.fill(left + 8, ly - 4, left + PANEL_W - 8, ly - 3, 0xFF555555);
        if (AuctionManager.isActive()) {
            long t = AuctionManager.remainingSeconds();
            ctx.drawTextWithShadow(textRenderer, String.format("§aLäuft: §f%s §7| Rest: §e%02d:%02d §7| ab §e%s",
                    AuctionManager.describe(), t / 60, t % 60, Money.format(AuctionManager.getMinPrice())),
                    left + 10, ly, 0xFFFFFFFF);
        } else {
            String res = AuctionManager.getLastResult();
            ctx.drawTextWithShadow(textRenderer, res.isEmpty() ? "§7Keine aktive Auktion." : "§7" + res, left + 10, ly, 0xFFFFFFFF);
        }
        List<Map.Entry<String, Double>> rank = AuctionManager.ranking();
        if (rank.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, "§8Noch keine Zahlungen eingegangen.", left + 10, ly + 14, 0xFFFFFFFF);
        } else {
            for (int i = 0; i < Math.min(7, rank.size()); i++) {
                Map.Entry<String, Double> e = rank.get(i);
                boolean ok = e.getValue() >= AuctionManager.getMinPrice();
                ctx.drawTextWithShadow(textRenderer, "§f" + (i + 1) + ". " + e.getKey() + "  "
                        + (ok ? "§e" : "§8") + Money.format(e.getValue()), left + 10, ly + 14 + i * 10, 0xFFFFFFFF);
            }
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        lastPrice = priceField.getText();
        lastDuration = durationField.getText();
        super.render(ctx, mouseX, mouseY, delta);

        MinecraftClient mc = MinecraftClient.getInstance();
        int slot = slotAt(mouseX, mouseY);
        if (slot >= 0 && mc.player != null) {
            ItemStack s = mc.player.getInventory().getStack(slot);
            if (!s.isEmpty()) ctx.drawItemTooltip(textRenderer, s, mouseX, mouseY);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
