# timgioh auction (Fabric, Minecraft 1.21.11)

Client-Mod für HugoSMP. **Taste Ö** (deutsches Layout = physische Semikolon-Taste) öffnet das Auktionsmenü.
Die Taste lässt sich unter Optionen → Steuerung → "timgioh auction" ändern.

## Bedienung
1. Ö drücken, Item im Inventar anklicken, Mindestpreis (`5000`, `5k`, `1.5m`) und Dauer in Sekunden eingeben.
2. "Im Chat ankündigen" an/aus, dann **Start**.
3. Eingehende `/pay`-Zahlungen werden im Chat, im Menü und in einem HUD (oben links) live mit Name + Betrag angezeigt.
   Ein Gebot = Summe aller Zahlungen eines Spielers. Gewinner = höchste Summe >= Mindestpreis.
4. Nach Ablauf wird der Gewinner angezeigt (und optional im Chat verkündet). Item-Übergabe/Rückzahlungen machst du manuell.

## Wichtig: Zahlungsmeldungen anpassen
Die Mod erkennt Zahlungen anhand der Servermeldung, die DU beim Empfang bekommst. Die Regexe stehen in
`config/timgioh_auction.json` (`payPatterns`, benötigt Gruppen `player` und `amount`). Zahle dir einmal selbst
(oder lass einen Freund zahlen), kopiere die Meldung und passe das Regex bei Bedarf an.

## Bauen
- **IntelliJ IDEA:** Ordner öffnen → Gradle-Import abwarten → Gradle-Task `build` → `build/libs/timgioh-auction-1.0.0.jar`
- **Kommandozeile:** JDK 21 + Gradle 9.2+ installieren, dann `gradle build`
- **GitHub:** Projekt hochladen, Actions-Workflow "build" liefert das fertige JAR als Artifact.

JAR in `.minecraft/mods` legen (Fabric Loader >= 0.18, Fabric API für 1.21.11).

Hinweis: Die Versionen in `gradle.properties` (Loom, Loader, Fabric API, Yarn) konnte ich hier nicht per Build
verifizieren. Falls Gradle meckert, die aktuellen Werte für 1.21.11 auf https://fabricmc.net/develop abgleichen.

NoRisk Client: Profil mit Fabric + 1.21.11 anlegen, Fabric API per Browse installieren, das JAR bei Custom Mods per
Drag & Drop hinzufügen.
