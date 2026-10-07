package de.timgioh.auction;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Konfiguration: config/timgioh_auction.json */
public final class AuctionConfig {
    private static final Logger LOG = LoggerFactory.getLogger(TimgiohAuctionClient.MOD_ID);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static Data data = new Data();
    private static final List<Pattern> patterns = new ArrayList<>();

    static class Data {
        int defaultDurationSeconds = 60;
        boolean showBidsInChat = true;
        String startMessage = "Auktion: {count}x {item} | Mindestgebot: {price} | Biete mit /pay {player} <Betrag> | Dauer: {seconds}s";
        String endMessage = "Auktion beendet! Gewinner: {winner} mit {amount} fuer {count}x {item}";
        String noWinnerMessage = "Auktion beendet - kein gueltiges Gebot fuer {count}x {item}.";
        String cancelMessage = "Die Auktion wurde abgebrochen.";
        /**
         * Regexe fuer eingehende Zahlungsmeldungen. Benoetigt die benannten Gruppen
         * "player" und "amount". Passe sie an die echten HugoSMP-Meldungen an!
         */
        List<String> payPatterns = new ArrayList<>(List.of(
                "(?i)(?<player>[A-Za-z0-9_]{3,16}) (?:hat dir|hat ihnen|has sent you|sent you|paid you|gave you|schickte dir) [$€]?\\s?(?<amount>\\d[\\d.,]*\\s?[kKmMbB]?)",
                "(?i)du hast [$€]?\\s?(?<amount>\\d[\\d.,]*\\s?[kKmMbB]?)\\s?[$€]? von (?<player>[A-Za-z0-9_]{3,16}) erhalten",
                "(?i)you (?:have )?received [$€]?\\s?(?<amount>\\d[\\d.,]*\\s?[kKmMbB]?)\\s?[$€]? from (?<player>[A-Za-z0-9_]{3,16})"
        ));
    }

    private AuctionConfig() {}

    public static void load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("timgioh_auction.json");
        try {
            if (Files.exists(file)) {
                try (Reader r = Files.newBufferedReader(file)) {
                    Data d = GSON.fromJson(r, Data.class);
                    if (d != null) data = d;
                }
            } else {
                try (Writer w = Files.newBufferedWriter(file)) {
                    GSON.toJson(data, w);
                }
            }
        } catch (Exception e) {
            LOG.error("Konfiguration konnte nicht geladen werden, nutze Standardwerte", e);
        }
        if (data.payPatterns == null) data.payPatterns = new Data().payPatterns;
        patterns.clear();
        for (String p : data.payPatterns) {
            try {
                patterns.add(Pattern.compile(p));
            } catch (PatternSyntaxException e) {
                LOG.warn("Ungueltiges Regex in payPatterns: {}", p);
            }
        }
    }

    public static List<Pattern> patterns() { return patterns; }
    public static int defaultDuration() { return Math.max(5, data.defaultDurationSeconds); }
    public static boolean showBidsInChat() { return data.showBidsInChat; }
    public static String startMessage() { return nn(data.startMessage); }
    public static String endMessage() { return nn(data.endMessage); }
    public static String noWinnerMessage() { return nn(data.noWinnerMessage); }
    public static String cancelMessage() { return nn(data.cancelMessage); }

    private static String nn(String s) { return s == null ? "" : s; }
}
