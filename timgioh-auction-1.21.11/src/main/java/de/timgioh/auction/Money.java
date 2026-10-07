package de.timgioh.auction;

import java.util.Locale;

/** Parsen und Formatieren von Geldbeträgen (1.000,50 / 1,000.50 / 5k / 2.5m ...). */
public final class Money {
    private Money() {}

    /** @return Betrag oder -1, wenn nicht lesbar */
    public static double parse(String raw) {
        if (raw == null) return -1;
        String s = raw.trim().toLowerCase(Locale.ROOT).replace("$", "").replace("€", "").replace(" ", "");
        if (s.isEmpty()) return -1;

        double mult = 1;
        char last = s.charAt(s.length() - 1);
        if (last == 'k') mult = 1_000d;
        else if (last == 'm') mult = 1_000_000d;
        else if (last == 'b') mult = 1_000_000_000d;
        if (mult != 1) s = s.substring(0, s.length() - 1);

        s = s.replaceAll("[^0-9.,]", "");
        if (s.isEmpty()) return -1;

        int lastDot = s.lastIndexOf('.');
        int lastComma = s.lastIndexOf(',');
        int sepPos = Math.max(lastDot, lastComma);
        String normalized;
        if (sepPos < 0) {
            normalized = s;
        } else {
            char sep = s.charAt(sepPos);
            boolean hasBoth = lastDot >= 0 && lastComma >= 0;
            int count = 0;
            for (char c : s.toCharArray()) if (c == sep) count++;
            int digitsAfter = s.length() - sepPos - 1;
            boolean decimal;
            if (hasBoth) decimal = true;          // letztes Trennzeichen = Dezimalzeichen
            else if (count > 1) decimal = false;  // 1.000.000
            else decimal = digitsAfter != 3;      // "1.000" = tausend, "1.5" = eineinhalb
            if (decimal) {
                normalized = s.substring(0, sepPos).replaceAll("[.,]", "") + "." + s.substring(sepPos + 1);
            } else {
                normalized = s.replaceAll("[.,]", "");
            }
        }
        try {
            return Double.parseDouble(normalized) * mult;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static String format(double v) {
        if (v == Math.rint(v)) return String.format(Locale.GERMANY, "%,.0f", v);
        return String.format(Locale.GERMANY, "%,.2f", v);
    }
}
