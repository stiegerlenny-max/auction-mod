package de.timgioh.auction;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Zustand und Logik der laufenden Auktion. Gebot = Summe aller /pay-Zahlungen eines Spielers. */
public final class AuctionManager {
    private static boolean active;
    private static ItemStack stack = ItemStack.EMPTY;
    private static double minPrice;
    private static long endTimeMs;
    private static boolean announce;
    private static final Map<String, Double> totals = new LinkedHashMap<>();
    private static String lastResult = "";

    private AuctionManager() {}

    public static boolean isActive() { return active; }
    public static ItemStack getStack() { return stack; }
    public static double getMinPrice() { return minPrice; }
    public static String getLastResult() { return lastResult; }

    public static long remainingSeconds() {
        if (!active) return 0;
        return Math.max(0, (endTimeMs - System.currentTimeMillis() + 999) / 1000);
    }

    public static String describe() {
        return stack.getCount() + "x " + stack.getName().getString();
    }

    public static List<Map.Entry<String, Double>> ranking() {
        List<Map.Entry<String, Double>> list = new ArrayList<>(totals.entrySet());
        list.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return list;
    }

    public static void start(MinecraftClient mc, ItemStack item, double price, int seconds, boolean announceInChat) {
        totals.clear();
        lastResult = "";
        stack = item;
        minPrice = price;
        announce = announceInChat;
        endTimeMs = System.currentTimeMillis() + seconds * 1000L;
        active = true;

        info(mc, "§aAuktion gestartet: §f" + describe() + " §7ab §e" + Money.format(price) + " §7für §f" + seconds + "s");
        if (announce) chat(mc, fill(AuctionConfig.startMessage(), mc, seconds, null, 0));
    }

    public static void stop(MinecraftClient mc) {
        if (!active) return;
        active = false;
        lastResult = "Auktion abgebrochen.";
        info(mc, "§cAuktion abgebrochen.");
        if (announce) chat(mc, AuctionConfig.cancelMessage());
    }

    public static void tick(MinecraftClient mc) {
        if (active && System.currentTimeMillis() >= endTimeMs) finish(mc);
    }

    private static void finish(MinecraftClient mc) {
        active = false;
        String winner = null;
        double amount = 0;
        for (Map.Entry<String, Double> e : ranking()) {
            if (e.getValue() >= minPrice) {
                winner = e.getKey();
                amount = e.getValue();
                break;
            }
        }
        if (winner == null) {
            lastResult = "Beendet: kein gültiges Gebot.";
            info(mc, "§eAuktion beendet: kein gültiges Gebot für §f" + describe());
            if (announce) chat(mc, fill(AuctionConfig.noWinnerMessage(), mc, 0, null, 0));
        } else {
            lastResult = "Gewinner: " + winner + " (" + Money.format(amount) + ")";
            info(mc, "§aAuktion beendet! Gewinner: §f" + winner + " §7mit §e" + Money.format(amount)
                    + " §7für §f" + describe());
            info(mc, "§7Denk daran, Item übergeben und überbotene Spieler ggf. zurückzahlen.");
            if (announce) chat(mc, fill(AuctionConfig.endMessage(), mc, 0, winner, amount));
        }
    }

    /** Wird für jede eingehende Servermeldung aufgerufen. */
    public static void onChat(String text) {
        if (!active || text == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        String self = mc.getSession().getUsername();
        for (Pattern p : AuctionConfig.patterns()) {
            Matcher m = p.matcher(text);
            if (!m.find()) continue;
            String player;
            String rawAmount;
            try {
                player = m.group("player");
                rawAmount = m.group("amount");
            } catch (IllegalArgumentException e) {
                continue;
            }
            double amount = Money.parse(rawAmount);
            if (player == null || amount <= 0 || player.equalsIgnoreCase(self)) continue;

            double total = totals.merge(player, amount, Double::sum);
            if (AuctionConfig.showBidsInChat()) {
                info(mc, "§f" + player + " §7hat §e" + Money.format(amount) + " §7gezahlt §8(gesamt "
                        + Money.format(total) + (total >= minPrice ? "" : ", unter Mindestgebot") + ")");
            }
            return;
        }
    }

    private static String fill(String t, MinecraftClient mc, int seconds, String winner, double amount) {
        return t.replace("{item}", stack.getName().getString())
                .replace("{count}", String.valueOf(stack.getCount()))
                .replace("{price}", Money.format(minPrice))
                .replace("{seconds}", String.valueOf(seconds))
                .replace("{player}", mc.getSession().getUsername())
                .replace("{winner}", winner == null ? "-" : winner)
                .replace("{amount}", Money.format(amount));
    }

    private static void info(MinecraftClient mc, String msg) {
        if (mc.player != null) mc.player.sendMessage(Text.literal("§8[§6Auktion§8] §r" + msg), false);
    }

    private static void chat(MinecraftClient mc, String msg) {
        if (mc.player == null || msg.isBlank()) return;
        if (msg.length() > 256) msg = msg.substring(0, 256);
        if (msg.startsWith("/")) msg = " " + msg; // nie versehentlich als Befehl senden
        mc.player.networkHandler.sendChatMessage(msg);
    }
}
