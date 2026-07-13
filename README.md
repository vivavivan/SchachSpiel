# Schachspiel in Java

Ein Schachspiel in Java mit eigener GUI und 4 verschieden starken Bot-Gegnern. Die Spiellogik ist sauber von der Oberfläche getrennt, und extrem auf effiziens poliert.

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
  - Verwaltet den Spielzustand auf einem 8x8-Gitter
  - Prüft alle Züge, auch En Passant und Rochade
  - Erkennt automatisch Schachmatt, Patt und Remis durch Materialmangel

- **Zughistorie (`ZugRegister.java`)**
  - Für jeden Zug ein eigenes Objekt zu erstellen wäre bei den ganzen Bot-Simulationen und beim Undo zu langsam
  - Deshalb wird ein Zug (Start, Ziel, Promotion, En Passant, geschlagene Figur) einfach in einen einzigen 32-Bit-Integer gepackt - Macht die Bots deutlich effizienter

---

## Die Bots

Es gibt mehrere Bot-Klassen, von ganz simpel bis relativ stark – vom `RandomBotSpieler` bis zum `BesserAlsStockFishBot`. Der stärkste nutzt:

- **Minimax mit Alpha-Beta-Pruning**
  - Klassischer Minimax-Baum zur Zugfindung
  - Äste, die eh nicht besser werden können, werden abgeschnitten statt komplett durchgerechnet
  - Dadurch kann er deutlich tiefer suchen, ohne ewig zu brauchen

- **Suchtiefe passt sich an**
  - Der Bot schaut, wie viel Material noch auf dem Brett ist
  - Sind nur noch wenige Figuren übrig (z. B. unter 8 oder unter 4), geht er von Tiefe 4 auf Tiefe 5
  - Im Endspiel gibt's weniger mögliche Züge, also kann er sich das leisten

- **Piece-Square Tables**
  - Eine Stellung wird nicht nur nach Materialwert bewertet (Bauer = 100, Dame = 900, ...)
  - Jede Figur hat zusätzlich eine eigene Tabelle, die gute und schlechte Felder bewertet
  - Der König wird im Mittelspiel z. B. eher auf Sicherheit bewertet, im Endspiel eher auf Zentrumsnähe

- **Ein bisschen Zufall**
  - Sind mehrere Züge gleich gut bewertet, werden sie vorher durchgemischt
  - So spielt der Bot nicht immer exakt gleich und es kommt nicht zu einer Endlosschleife von Zügen

- **Besser als Chess.com?**
  - "Martin Bot" besiegte 2/3 mal die chess.com engine auf 900 Elo. 

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
- Deshalb wird explizit `setOpaque(true)` und `setBorderPainted(false)` gesetzt
- Dadurch sieht das Brett auf Windows, macOS und Linux gleich aus

**LLM-Hilfe beim GUI**
- Die eigentliche Logik (Brett, Bots, ZugRegister) ist komplett selbst geschrieben
- Für Teile von `SpielGUI.java` hab ich mir mit LLMs geholfen, vor allem bei:
  - dem responsiven Layout (Skalierung im `ComponentAdapter`)
  - Farben abstimmen
  - dem ganzen Swing-Boilerplate
- Hat Zeit gespart, damit ich mich mehr auf die Bots konzentrieren konnte

**FlatLaf (optional)**
- Die UI ist schon für FlatLaf vorbereitet, läuft aber standardmäßig im normalen System-Look
- Um z.B. `FlatMacLightLaf` zu aktivieren:
  1. `flatlaf.jar` in den Classpath/Build-Path der IDE einbinden
  2. In `src/Spiel/Main.java` die `//` vor Import und Setup entfernen

---

## ToDo

Was ich mir für die Zukunft meines Projektes überlegt habe:

- **Maven**: FlatLaf ist aktuell manuell eingebunden, sollte irgendwann sauber über Maven (oder Gradle) laufen
- **Eigene Brett Konstellation**: Es wäre interessant selber interaktiv die Figuren vor dem Spiel so setzen zu können, wie man will (siehe lichess editor: https://lichess.org/editor)

---

**Entwickler:** Vivan Chandrasekhara
