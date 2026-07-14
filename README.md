# Schachspiel in Java

Ein Schachspiel in Java mit eigener GUI und 4 verschieden starken Bot-Gegnern. Die Spiellogik ist sauber von der Oberfläche getrennt, und extrem auf Effizienz poliert.

---

## Inhaltsverzeichnis
1. [Architektur](#architektur)
2. [Die Bots](#die-bots)
3. [Installation und Ausführung](#installation-und-ausführung)
4. [Zur UI](#zur-ui)
5. [Was noch fehlt](#was-noch-fehlt)

---

## Architektur

- **Regel-Engine (`Brett.java`)**
  - Verwaltet den Spielzustand auf einem 8x8-Gitter (`Figur[][] felder`)
  - Hält zusätzlich Listen aller weißen/schwarzen Figuren und die Königspositionen separat vor, damit Bots und die Matt-Prüfung nicht jedes Mal das ganze Brett absuchen müssen
  - Zug-Legalität läuft zweistufig: `istZugLegal()` prüft nur das reine Bewegungsmuster der Figur (Bauer, Läufer, ...). `istZugGueltig()` führt den Zug danach probeweise direkt auf dem echten Brett aus, prüft ob der eigene König im Schach steht, und macht den Zug wieder rückgängig – spart eine komplette Brett-Kopie pro Legalitätscheck
  - Prüft automatisch Schachmatt, Patt und Remis durch Materialmangel (u. a. K vs. K, K+L/S vs. K, K+L vs. K+L mit gleichfarbigem Läufer, K+2S vs. K)
  - Unterstützt En Passant und Rochade vollständig, inklusive korrektem Undo für beide Sonderfälle
- **Zughistorie (`ZugRegister.java`)**
  - Für jeden Zug ein eigenes Objekt zu erstellen wäre bei den ganzen Bot-Simulationen und beim Undo zu langsam
  - Deshalb wird ein Zug in einen einzigen Integer gepackt: Start- und Zielfeld (je 6 Bit), Promotion-Flag + Umwandlungstyp, En-Passant-Spalte, Typ der geschlagenen Figur, sowie ob Figur bzw. geschlagene Figur zuvor schon bewegt wurde (relevant fürs Rochade-Recht nach Undo)
  - Der Speicher ist ein simples `int[]`, das sich bei Bedarf verdoppelt, statt eine wachsende Liste von Objekten – dadurch entsteht bei tiefen Bot-Suchen (zehntausende Push/Undo-Operationen) kaum Garbage-Collector-Druck

---

## Die Bots

Es gibt vier Bot-Klassen mit steigender Stärke:

- **`RandomBotSpieler`** – wählt aus allen legalen Zügen rein zufällig einen aus. Dient hauptsächlich als Baseline zum Testen der Regel-Engine.
- **`ersterBotSpieler`** – ein "gieriger" 1-Zug-Bot. Er simuliert jeden möglichen Zug einmal, bewertet danach nur den reinen Materialwert (+ kleiner Bonus fürs Schachgebot) und wählt den besten. Schaut nicht voraus, lässt sich also leicht in eine Falle locken.
- **`MartinBot`** und **`BesserAlsStockFishBot`** – beide nutzen echten Minimax mit Alpha-Beta-Pruning und teilen sich denselben Suchbaum-Code. Der Unterschied liegt allein in der Bewertungsfunktion:

  - **Minimax mit Alpha-Beta-Pruning**
    - Klassischer Minimax-Baum zur Zugfindung, abwechselnd maximierend (eigener Zug) und minimierend (Gegnerzug)
    - Äste, die eh nicht besser werden können, werden abgeschnitten statt komplett durchgerechnet (`beta <= alpha`)
    - Die Suche arbeitet direkt auf einer Kopie des Bretts und macht Züge per `bewegeFigur()`/`undo()` rückgängig, statt bei jedem Suchbaum-Knoten neu zu kopieren
  - **Suchtiefe passt sich an**
    - Der Bot schaut, wie viele eigene Figuren noch auf dem Brett sind
    - Sind es weniger als 8, wird die Tiefe von 4 auf 5 erhöht (im Endspiel gibt es weniger mögliche Züge, das ist also verkraftbar)
  - **Ein bisschen Zufall**
    - Vor der Suche werden alle Züge durchgemischt, damit bei gleicher Bewertung nicht immer exakt derselbe Zug gewählt wird und keine Zug-Wiederholungsschleife entsteht
  - **Nur `BesserAlsStockFishBot`: Piece-Square Tables**
    - Zusätzlich zum reinen Materialwert (Bauer = 100, Springer/Läufer = 300, Turm = 500, Dame = 900, König = 10000) bekommt jede Figur einen positionsabhängigen Bonus aus einer eigenen 64-Feld-Tabelle
    - Der König hat zwei Tabellen: eine fürs Mittelspiel (Bonus für Rochade-Ecken/Sicherheit), eine fürs Endspiel (Bonus für Zentrumsnähe), umgeschaltet je nachdem ob die eigene Farbe noch mehr als 5 Figuren hat
  - Beide Bots bestrafen zusätzlich Figuren, die angegriffen und nicht gedeckt sind – der Abzug ist dabei größer als der reine Materialwert der Figur, damit ungedeckte Hänger konsequent vermieden werden
  - **Besser als Chess.com?**
    - "Martin Bot" besiegte 2/3 mal die chess.com engine auf 900 Elo

---

## Installation und Ausführung

Kein Maven, kein Gradle, keine externen Abhängigkeiten – einfach klonen und starten.

### Voraussetzungen
- Java Development Kit (JDK) Version 23 oder höher
- IDE: Visual Studio Code, IntelliJ IDEA oder Eclipse

### Ausführen
1. Repository klonen:
   ```bash
   git clone https://github.com/vivavivan/SchachSpiel.git
   ```
2. Hauptordner des Projekts in der IDE öffnen
3. `src/Spiel/Main.java` ausführen, um das Hauptmenü zu starten

---

## Zur UI

**Mac-Buttons**
- Native Mac-Buttons blockieren standardmäßig Hintergrundfarben
- Deshalb wird explizit `setOpaque(true)` und `setBorderPainted(false)` gesetzt, damit das Brett auf Windows, macOS und Linux gleich aussieht

**LLM-Hilfe beim GUI**
- Die eigentliche Logik (Brett, Bots, ZugRegister) ist komplett selbst geschrieben
- Für Teile von `SpielGUI.java` habe ich mir mit LLMs geholfen (responsives Layout, Farben, Swing-Boilerplate), um mich mehr auf die Bots konzentrieren zu können

**FlatLaf (optional)**
- Die UI ist schon für FlatLaf vorbereitet, läuft aber standardmäßig im normalen System-Look
- Um z. B. `FlatMacLightLaf` zu aktivieren:
  1. `flatlaf.jar` in den Classpath/Build-Path der IDE einbinden
  2. In `src/Spiel/Main.java` die `//` vor Import und Setup entfernen

---

## Was noch fehlt

Was ich mir für die Zukunft meines Projektes überlegt habe:
- **Maven**: FlatLaf ist aktuell manuell eingebunden, sollte irgendwann sauber über Maven (oder Gradle) laufen
- **Eigene Brett-Konstellation**: Es wäre interessant, selbst interaktiv die Figuren vor dem Spiel so setzen zu können, wie man will (siehe [lichess editor](https://lichess.org/editor))

---

**Entwickler:** Vivan Chandrasekhara
