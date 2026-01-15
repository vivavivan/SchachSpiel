package Spiel;

import java.util.ArrayList;

/**
 * Die zentrale Logik-Klasse des Spiels.
 * Sie verwaltet das Schachbrett, die Figuren und die Spielregeln.
 */
public class Brett {

    // Das 8x8-Gitter, das die Positionen aller Figuren speichert.
    private final Figur[][] felder = new Figur[8][8];
    private ZugRegister zugRegister = new ZugRegister();
    private boolean istKopie = false;

    // Speichert die Positionen der Könige für schnellen Zugriff bei der Schach-Prüfung.
    private final int[] posKoenigWeiss = new int[2];
    private final int[] posKoenigSchwarz = new int[2];

    // Listen, um schnell auf alle Figuren einer Farbe zugreifen zu können (wichtig für Bots und Spielende-Prüfung).
    private final ArrayList<Figur> weisseFiguren = new ArrayList<>();
    private final ArrayList<Figur> schwarzeFiguren = new ArrayList<>();

    private int enPassantMoeglich = -1;
    // Speichert den Spielstatus: 0 = läuft, +/-1 = Matt, +/-2 = Patt, 3 = Remis.
    private int schachMatt = 0;

    // Standard-Konstruktor für ein neues Spiel.
    public Brett() {
        this(false);
    }

    public Brett(Boolean istKopie) {
        this.istKopie = istKopie;
        if (!this.istKopie) {
            initialisiereBrett();
        }
    }

    /**
     * Erstellt eine komplette, unabhängige Kopie des aktuellen Bretts.
     * Unverzichtbar für den Bot, damit er Züge simulieren kann, ohne das echte Spiel zu beeinflussen.
     */
    public Brett erstelleKopie() {
        Brett kopie = new Brett(true); // Leeres Brett erstellen

        // Relevante Werte kopieren
        kopie.enPassantMoeglich = this.enPassantMoeglich;
        kopie.schachMatt = this.schachMatt;
        System.arraycopy(this.posKoenigWeiss, 0, kopie.posKoenigWeiss, 0, 2);
        System.arraycopy(this.posKoenigSchwarz, 0, kopie.posKoenigSchwarz, 0, 2);

        // ZugRegister kopieren
        kopie.zugRegister = this.zugRegister.kopie();

        // Figuren reinmachen
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (this.felder[i][j] != null) {
                    Figur original = this.felder[i][j];
                    Figur klon = original.clone();
                    kopie.platzieren(klon, i, j); // Fügt Figur in Array und Listen ein
                }
            }
        }

        return kopie;
    }

    // Gibt die Figur auf einem bestimmten Feld zurück, oder null, wenn es leer ist.
    public Figur getFigur(int zeile, int spalte) {
        return felder[zeile][spalte];
    }


    /**
     * Führt einen einfachen Zug aus (ohne Bauernumwandlung).
     */
    public void bewegeFigur(int vonZeile, int vonSpalte, int nachZeile, int nachSpalte) {
        bewegeFigur(felder, vonZeile, vonSpalte, nachZeile, nachSpalte, 0, 0);
    }

    /**
     * Führt einen Zug aus und berücksichtigt dabei eine mögliche Bauernumwandlung.
     */
    public void bewegeFigur(int vonZeile, int vonSpalte, int nachZeile, int nachSpalte, int istPromotion, int promotionTyp) {
        bewegeFigur(felder, vonZeile, vonSpalte, nachZeile, nachSpalte, istPromotion, promotionTyp);
    }

    // Die eigentliche Logik, um eine Figur zu bewegen.
    // Sie aktualisiert das Brett, die Figurenlisten und registriert den Zug für die Undo-Funktion.
    private void bewegeFigur(Figur[][] felder, int vonZeile, int vonSpalte, int nachZeile, int nachSpalte, int istPromotion, int promotionTyp) {
        Figur figur = felder[vonZeile][vonSpalte];
        if (figur == null) return; // Sicherheitscheck gegen NPE
        Figur.Farbe gegnerFarbe = (figur.getFarbe() == Figur.Farbe.WEISS) ? Figur.Farbe.SCHWARZ : Figur.Farbe.WEISS;
        ArrayList<Figur> gegnerFiguren = (gegnerFarbe == Figur.Farbe.WEISS) ? weisseFiguren : schwarzeFiguren;
        boolean warErsterZug = !figur.hatSichBewegt();
        int geschlageneFigurTyp = 0;
        boolean hatOpferSichBewegt = false;
        int enPassantGeschlagenSpalte = -1; // Speichert die Spalte, falls ein EP-Schlag stattfindet

        // Schlagzug: Gegnerische Figur aus Liste entfernen
        if (felder[nachZeile][nachSpalte] != null) {
            geschlageneFigurTyp = gibFigurTypID(felder[nachZeile][nachSpalte]);
            hatOpferSichBewegt = felder[nachZeile][nachSpalte].hatSichBewegt();
            gegnerFiguren.remove(felder[nachZeile][nachSpalte]);
        }

        //enPassant Logik: Der andere Bauer muss verschwinden
        if (figur instanceof Bauer && vonSpalte != nachSpalte && felder[nachZeile][nachSpalte] == null) {
            Figur opfer = felder[vonZeile][nachSpalte];
            if (opfer != null) {
                hatOpferSichBewegt = opfer.hatSichBewegt();
                gegnerFiguren.remove(opfer);
                enPassantGeschlagenSpalte = nachSpalte; // Wichtig für Undo: Wir merken uns, wo geschlagen wurde
            }
            felder[vonZeile][nachSpalte] = null;
        }

        // Rochade Logik: Wenn König 2 Schritte macht, bewege auch den Turm
        if (figur instanceof Koenig && Math.abs(nachSpalte - vonSpalte) == 2) {
            int turmSpalte = (nachSpalte > vonSpalte) ? 7 : 0;
            int turmZielSpalte = (nachSpalte > vonSpalte) ? nachSpalte - 1 : nachSpalte + 1;

            Figur turm = felder[vonZeile][turmSpalte];
            felder[vonZeile][turmSpalte] = null;
            felder[vonZeile][turmZielSpalte] = turm;

            // Turm Koordinaten aktualisieren
            if (turm != null) {
                turm.setSpalte(turmZielSpalte);
                turm.setHatSichBewegt(true);
            }
        }

        felder[vonZeile][vonSpalte] = null;
        felder[nachZeile][nachSpalte] = figur;

        // Eigene Figur Koordinaten aktualisieren
        figur.setZeile(nachZeile);
        figur.setSpalte(nachSpalte);

        if (figur instanceof Koenig) {
            setzeKoenigPos(figur.getFarbe(), nachZeile, nachSpalte);
        }

        if(istPromotion == 1) {
            if(promotionTyp == 1) {
                promoviereBauer(nachZeile, nachSpalte, "Turm");
            }else if (promotionTyp == 2) {
                promoviereBauer(nachZeile, nachSpalte, "Läufer");
            }else if (promotionTyp == 3) {
                promoviereBauer(nachZeile, nachSpalte, "Springer");
            }else {
                promoviereBauer(nachZeile, nachSpalte, "Dame");
            }

        }

        setzeEnPassantMoeglichkeit(figur, vonZeile, vonSpalte, nachZeile, nachSpalte);

        if (figur != null) figur.setHatSichBewegt(true);

        zugRegister.registriereZug(vonZeile, vonSpalte, nachZeile, nachSpalte, istPromotion, promotionTyp, enPassantGeschlagenSpalte, geschlageneFigurTyp, warErsterZug, hatOpferSichBewegt);

        spielZuende(gegnerFarbe);
    }

    /**
     * Tauscht einen Bauern, der die gegnerische Grundlinie erreicht hat,
     * gegen eine andere Figur (Dame, Turm, etc.) aus.
     */
    public void promoviereBauer(int zeile, int spalte, String neuerTyp) {
        Figur bauer = felder[zeile][spalte];
        if (bauer == null || !(bauer instanceof Bauer)) return;

        Figur.Farbe farbe = bauer.getFarbe();
        ArrayList<Figur> figurenListe = (farbe == Figur.Farbe.WEISS) ? weisseFiguren : schwarzeFiguren;

        // Alten Bauern entfernen
        figurenListe.remove(bauer);

        Figur neueFigur;
        if (neuerTyp.equals("Turm")) {
            neueFigur = new Turm(farbe);
        } else if (neuerTyp.equals("Springer")) {
            neueFigur = new Springer(farbe);
        } else if (neuerTyp.equals("Läufer")) {
            neueFigur = new Laeufer(farbe);
        } else {
            neueFigur = new Koenigin(farbe); // Standard ist Dame
        }

        neueFigur.setHatSichBewegt(true);

        // Neue Figur platzieren (nutzt deine existierende Hilfsmethode)
        platzieren(neueFigur, zeile, spalte);

        // Spielstatus neu berechnen, da die neue Figur Schach geben könnte
        spielZuende((farbe == Figur.Farbe.WEISS) ? Figur.Farbe.SCHWARZ : Figur.Farbe.WEISS);
    }

    /**
     * Macht den letzten Zug rückgängig.
     * Stellt geschlagene Figuren wieder her und setzt den "hatSichBewegt"-Status zurück.
     */
    public boolean undo() {
        int letzterZug = zugRegister.gibLetztenZug();
        if (letzterZug == -1) return false;

        int startFeld = letzterZug & 0x3F;
        int zielFeld = (letzterZug >> 6) & 0x3F;
        int istPromotion = (letzterZug >> 12) & 1;
        int enPassant = ((letzterZug >> 15) & 15) - 1;
        int geschlageneTyp = (letzterZug >> 19) & 7;
        boolean warErsterZug = ((letzterZug >> 22) & 1) == 1;
        boolean hatOpferSichBewegt = ((letzterZug >> 23) & 1) == 1;

        int vonZeile = startFeld >> 3;
        int vonSpalte = startFeld & 7;
        int nachZeile = zielFeld >> 3;
        int nachSpalte = zielFeld & 7;

        Figur figur = felder[nachZeile][nachSpalte];
        if (figur == null) return false;

        // Falls Promotion war: Zurück zum Bauern
        if (istPromotion == 1) {
            ArrayList<Figur> figurenListe = (figur.getFarbe() == Figur.Farbe.WEISS) ? weisseFiguren : schwarzeFiguren;
            figurenListe.remove(figur);

            figur = new Bauer(figur.getFarbe());
            figurenListe.add(figur);
        }

        // Figur zurückbewegen
        felder[vonZeile][vonSpalte] = figur;
        felder[nachZeile][nachSpalte] = null;
        figur.setZeile(vonZeile);
        figur.setSpalte(vonSpalte);
        if (warErsterZug) figur.setHatSichBewegt(false);

        // Königsposition aktualisieren
        if (figur instanceof Koenig) {
            setzeKoenigPos(figur.getFarbe(), vonZeile, vonSpalte);
        }

        // Rochade rückgängig machen (Turm zurückstellen)
        if (figur instanceof Koenig && Math.abs(vonSpalte - nachSpalte) == 2) {
            int turmZielSpalte = (nachSpalte > vonSpalte) ? nachSpalte - 1 : nachSpalte + 1;
            int turmStartSpalte = (nachSpalte > vonSpalte) ? 7 : 0;
            Figur turm = felder[vonZeile][turmZielSpalte];
            if (turm != null) {
                felder[vonZeile][turmStartSpalte] = turm;
                felder[vonZeile][turmZielSpalte] = null;
                turm.setSpalte(turmStartSpalte);
                turm.setHatSichBewegt(false);
            }
        }

        // Geschlagene Figur wiederherstellen
        if (enPassant != -1) {
            Figur.Farbe gegnerFarbe = (figur.getFarbe() == Figur.Farbe.WEISS) ? Figur.Farbe.SCHWARZ : Figur.Farbe.WEISS;
            Bauer opfer = new Bauer(gegnerFarbe);
            platzieren(opfer, vonZeile, nachSpalte);
            opfer.setHatSichBewegt(true);
            opfer.setHatSichBewegt(hatOpferSichBewegt);
        } else if (geschlageneTyp > 0) {
            Figur.Farbe gegnerFarbe = (figur.getFarbe() == Figur.Farbe.WEISS) ? Figur.Farbe.SCHWARZ : Figur.Farbe.WEISS;
            Figur opfer = erzeugeFigurNachTyp(geschlageneTyp, gegnerFarbe);
            platzieren(opfer, nachZeile, nachSpalte);
            opfer.setHatSichBewegt(hatOpferSichBewegt);
        }
        if(!istKopie) spielZuende(figur.getFarbe());
        return true;
    }

    private int gibFigurTypID(Figur f) {
        if (f instanceof Bauer) return 1;
        if (f instanceof Turm) return 2;
        if (f instanceof Springer) return 3;
        if (f instanceof Laeufer) return 4;
        if (f instanceof Koenigin) return 5;
        return 6; // König
    }

    private Figur erzeugeFigurNachTyp(int typ, Figur.Farbe farbe) {
        switch (typ) {
            case 1: return new Bauer(farbe);
            case 2: return new Turm(farbe);
            case 3: return new Springer(farbe);
            case 4: return new Laeufer(farbe);
            case 5: return new Koenigin(farbe);
            default: return new Koenig(farbe);
        }
    }

    /**
     * Die wichtigste Methode zur Zugvalidierung.
     * Prüft, ob ein Zug "pseudo-legal" ist (also den Bewegungsregeln der Figur entspricht)
     * und simuliert ihn dann, um zu sehen, ob der eigene König danach im Schach stehen würde.
     */
    public boolean istZugGueltig(int vonZeile, int vonSpalte, int nachZeile, int nachSpalte) {
        Figur ziehendeFigur = felder[vonZeile][vonSpalte];
        if (ziehendeFigur == null) return false;

        if (!istPseudoLegal(vonZeile, vonSpalte, nachZeile, nachSpalte)) return false;

        // --- Simulation Start ---
        Figur originalZiel = felder[nachZeile][nachSpalte];
        int alteZeile = ziehendeFigur.getZeile();
        int alteSpalte = ziehendeFigur.getSpalte();
        ArrayList<Figur> gegnerFiguren = (ziehendeFigur.getFarbe() == Figur.Farbe.WEISS) ? schwarzeFiguren : weisseFiguren;

        // Zug ausführen
        felder[nachZeile][nachSpalte] = ziehendeFigur;
        felder[vonZeile][vonSpalte] = null;
        ziehendeFigur.setZeile(nachZeile);
        ziehendeFigur.setSpalte(nachSpalte);

        if (ziehendeFigur instanceof Koenig) {
            setzeKoenigPos(ziehendeFigur.getFarbe(), nachZeile, nachSpalte);
        }

        // Schlagzug simulieren (Figur aus Liste entfernen)
        if (originalZiel != null) {
            gegnerFiguren.remove(originalZiel);
        }

        // En Passant simulieren
        boolean enPassant = (originalZiel == null && enPassantMoeglich == nachSpalte && ziehendeFigur instanceof Bauer) ? true : false;
        Figur enPassantOpfer = null;
        if (enPassant) {
            enPassantOpfer = felder[vonZeile][nachSpalte];
            felder[vonZeile][nachSpalte] = null;
            if (enPassantOpfer != null) gegnerFiguren.remove(enPassantOpfer);
        }

        boolean stehtImSchach = istKoenigBedroht(ziehendeFigur.getFarbe());

        if (ziehendeFigur instanceof Koenig) {
            setzeKoenigPos(ziehendeFigur.getFarbe(), vonZeile, vonSpalte);
        }

        // Zug rückgängig machen
        felder[vonZeile][vonSpalte] = ziehendeFigur;
        ziehendeFigur.setZeile(alteZeile);
        ziehendeFigur.setSpalte(alteSpalte);

        if (enPassant) {
            felder[vonZeile][nachSpalte] = enPassantOpfer;
            felder[nachZeile][nachSpalte] = null;
            if (enPassantOpfer != null) gegnerFiguren.add(enPassantOpfer);
        } else {
            felder[nachZeile][nachSpalte] = originalZiel;
            if (originalZiel != null) gegnerFiguren.add(originalZiel);
        }

        return !stehtImSchach;
    }

    /**
     * Prüft, ob ein Zug den reinen Bewegungsregeln einer Figur entspricht (ohne Schach-Prüfung).
     * Das ist eine schnelle Vorab-Prüfung.
     */
    public boolean istPseudoLegal(int vonZeile, int vonSpalte, int nachZeile, int nachSpalte) {
        Figur figur = getFigur(vonZeile, vonSpalte);
        if (figur == null) {
            return false;
        }

        // Grundlegende Prüfungen
        if (!istImBrett(nachZeile, nachSpalte)) {
            return false;
        }

        Figur zielFigur = getFigur(nachZeile, nachSpalte);
        if (zielFigur != null && zielFigur.getFarbe() == figur.getFarbe()) {
            return false;
        }

        //Herrscht schon Schach?
        //return koenigBedroht;

        // Spezifische Regeln je nach Figurtyp
        if (figur instanceof Bauer) {
            return istBauerZugGueltig((Bauer) figur, vonZeile, vonSpalte, nachZeile, nachSpalte);
        } else if (figur instanceof Turm) {
            return istTurmZugGueltig(vonZeile, vonSpalte, nachZeile, nachSpalte);
        } else if (figur instanceof Springer) {
            return istSpringerZugGueltig(vonZeile, vonSpalte, nachZeile, nachSpalte);
        } else if (figur instanceof Laeufer) {
            return istLaeuferZugGueltig(vonZeile, vonSpalte, nachZeile, nachSpalte);
        } else if (figur instanceof Koenigin) {
            return istKoeniginZugGueltig(vonZeile, vonSpalte, nachZeile, nachSpalte);
        } else if (figur instanceof Koenig) {
            return istKoenigPseudoLegal(vonZeile, vonSpalte, nachZeile, nachSpalte);
        }

        return false;
    }

    private boolean hatKeineLegalenZuege(Figur.Farbe farbe) {
        ArrayList<Figur> figuren = (farbe == Figur.Farbe.WEISS) ? weisseFiguren : schwarzeFiguren;

        for (Figur f : new ArrayList<>(figuren)) {
            int vonZeile = f.getZeile();
            int vonSpalte = f.getSpalte();

            for (int nachZeile = 0; nachZeile <= 7; nachZeile++) {
                for (int nachSpalte = 0; nachSpalte <= 7; nachSpalte++) {
                    if (istZugGueltig(vonZeile, vonSpalte, nachZeile, nachSpalte)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }
    
    /**
     * Prüft, ob das Spiel zu Ende ist (Schachmatt, Patt oder Remis)
     * und setzt die `schachMatt`-Variable entsprechend.
     */
    public boolean spielZuende(Figur.Farbe koenigfarbe) {
        if(istRemisWegenMaterialmangel()) {
            schachMatt = 3;
            return true;
        }
        if(istKoenigBedroht(koenigfarbe) && hatKeineLegalenZuege(koenigfarbe)) {
            schachMatt = (koenigfarbe == Figur.Farbe.WEISS) ? 1 : -1;
            return true;
        } else if(!istKoenigBedroht(koenigfarbe) && hatKeineLegalenZuege(koenigfarbe)) {
            schachMatt = (koenigfarbe == Figur.Farbe.WEISS) ? 2 : -2;
            return true;
        }
        schachMatt = 0;
        return false;
    }

    private boolean istRemisWegenMaterialmangel() {
        int weisseSpringer = 0;
        int schwarzeSpringer = 0;

        // 1. Weiße Figuren analysieren
        for (Figur f : weisseFiguren) {
            // Sobald ein Bauer, Turm oder eine Dame da ist, reicht das Material theoretisch zum Matt
            if (f instanceof Bauer || f instanceof Turm || f instanceof Koenigin) {
                return false;
            }
            if (f instanceof Springer) weisseSpringer++;
        }

        // 2. Schwarze Figuren analysieren
        for (Figur f : schwarzeFiguren) {
            if (f instanceof Bauer || f instanceof Turm || f instanceof Koenigin) {
                return false;
            }
            if (f instanceof Springer) schwarzeSpringer++;
        }

        int gesamt = weisseFiguren.size() + schwarzeFiguren.size();

        // Fall A: Nur noch die zwei Könige (K vs K)
        if (gesamt == 2) return true;

        // Fall B: Ein König und eine Leichtfigur gegen einen König (K+L vs K oder K+S vs K)
        if (gesamt == 3) {
            return true;
        }

        // Fall C: König und Läufer gegen König und Läufer (K+L vs K+L)
        if (weisseFiguren.size() == 2 && schwarzeFiguren.size() == 2) {
            return true;
        }
        
        if (gesamt == 4 && (weisseSpringer == 2 || schwarzeSpringer == 2)) {
            return true;
        }

        return false;
    }

    /**
     * Prüft, ob ein bestimmtes Feld von einer gegnerischen Figur angegriffen wird.
     * Wichtig für die Schach-Prüfung und die Rochade.
     */
    public boolean istFeldBedroht(int zeile, int spalte, Figur.Farbe eigeneFarbe) {
        Figur.Farbe gegnerFarbe = (eigeneFarbe == Figur.Farbe.WEISS) ? Figur.Farbe.SCHWARZ : Figur.Farbe.WEISS;

        // 1. Bauern prüfen
        // Wenn wir Weiß sind, greifen schwarze Bauern von "unten" an (Zeile + 1), da sie nach oben (-1) ziehen.
        // Wenn wir Schwarz sind, greifen weiße Bauern von "oben" an (Zeile - 1), da sie nach unten (+1) ziehen.
        int bauerZeile = (eigeneFarbe == Figur.Farbe.WEISS) ? zeile + 1 : zeile - 1;
        if (istImBrett(bauerZeile, spalte - 1)) {
            Figur f = getFigur(bauerZeile, spalte - 1);
            if (f instanceof Bauer && f.getFarbe() == gegnerFarbe) return true;
        }
        if (istImBrett(bauerZeile, spalte + 1)) {
            Figur f = getFigur(bauerZeile, spalte + 1);
            if (f instanceof Bauer && f.getFarbe() == gegnerFarbe) return true;
        }

        // 2. Springer prüfen
        int[][] springerZuege = {{-2, -1}, {-2, 1}, {-1, -2}, {-1, 2}, {1, -2}, {1, 2}, {2, -1}, {2, 1}};
        for (int[] zug : springerZuege) {
            int z = zeile + zug[0];
            int s = spalte + zug[1];
            if (istImBrett(z, s)) {
                Figur f = getFigur(z, s);
                if (f instanceof Springer && f.getFarbe() == gegnerFarbe) return true;
            }
        }

        // 3. Gerade Linien (Turm, Dame)
        int[][] geradeRichtungen = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] richtung : geradeRichtungen) {
            int z = zeile + richtung[0];
            int s = spalte + richtung[1];
            while (istImBrett(z, s)) {
                Figur f = getFigur(z, s);
                if (f != null) {
                    if (f.getFarbe() == gegnerFarbe && (f instanceof Turm || f instanceof Koenigin)) {
                        return true;
                    }
                    break; // Eigene Figur oder andere gegnerische Figur blockiert die Sichtlinie
                }
                z += richtung[0];
                s += richtung[1];
            }
        }

        // 4. Diagonalen (Läufer, Dame)
        int[][] diagonalRichtungen = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
        for (int[] richtung : diagonalRichtungen) {
            int z = zeile + richtung[0];
            int s = spalte + richtung[1];
            while (istImBrett(z, s)) {
                Figur f = getFigur(z, s);
                if (f != null) {
                    if (f.getFarbe() == gegnerFarbe && (f instanceof Laeufer || f instanceof Koenigin)) {
                        return true;
                    }
                    break; // Blockiert
                }
                z += richtung[0];
                s += richtung[1];
            }
        }

        // 5. König (Nachbarfelder)
        for (int z = zeile - 1; z <= zeile + 1; z++) {
            for (int s = spalte - 1; s <= spalte + 1; s++) {
                if (z == zeile && s == spalte) continue;
                if (istImBrett(z, s)) {
                    Figur f = getFigur(z, s);
                    if (f instanceof Koenig && f.getFarbe() == gegnerFarbe) return true;
                }
            }
        }
        return false;
    }

    // Findet schnell die Position eines Königs über die gespeicherten Koordinaten.
    public int[] findeKoenig(Figur.Farbe farbe) {
        return (farbe == Figur.Farbe.WEISS) ? posKoenigWeiss : posKoenigSchwarz;
    }

    // Prüft, ob der König einer bestimmten Farbe im Schach steht.
    public boolean istKoenigBedroht(Figur.Farbe eigeneFarbe) {
        int[] koenig = findeKoenig(eigeneFarbe);
        return istFeldBedroht(koenig[0], koenig[1], eigeneFarbe);
    }

    private void setzeEnPassantMoeglichkeit(Figur figur, int vonZeile, int vonSpalte, int nachZeile, int nachSpalte) {

        enPassantMoeglich = -1;

        //Voraussetzungen für enPassant erfüllt?
        if (Math.abs(nachZeile - vonZeile) == 2 && figur instanceof Bauer &&
                (nachSpalte > 0 && (getFigur(nachZeile, nachSpalte - 1) instanceof Bauer) &&
                        (getFigur(nachZeile, nachSpalte - 1).getFarbe() != figur.getFarbe()) ||
                        ((nachSpalte < 7 && (getFigur(nachZeile, nachSpalte + 1) instanceof Bauer) &&
                                (getFigur(nachZeile, nachSpalte + 1).getFarbe() != figur.getFarbe()))))) {

            enPassantMoeglich = vonSpalte;

        }
    }

    // Getter für die Figurenliste von Schwarz.
    public ArrayList<Figur> getSchwarzeFiguren() {
        return schwarzeFiguren;
    }

    // Getter für die Figurenliste von Weiß.
    public ArrayList<Figur> getWeisseFiguren() {
        return weisseFiguren;
    }

    // Prüft, ob die gegebenen Koordinaten innerhalb des 8x8-Bretts liegen.
    private boolean istImBrett(int zeile, int spalte) {
        return zeile >= 0 && zeile < 8 && spalte >= 0 && spalte < 8;
    }

    private boolean istBauerZugGueltig(Bauer bauer, int vonZeile, int vonSpalte, int nachZeile, int nachSpalte) {
        int richtung = (bauer.getFarbe() == Figur.Farbe.WEISS) ? 1 : -1;

        // Normaler Zug
        if (vonSpalte == nachSpalte && getFigur(nachZeile, nachSpalte) == null) {
            if (nachZeile == vonZeile + richtung) {
                return true;
            }
            // Erster Zug
            boolean istErsterZug = (bauer.getFarbe() == Figur.Farbe.WEISS && vonZeile == 1) || (bauer.getFarbe() == Figur.Farbe.SCHWARZ && vonZeile == 6);
            if (istErsterZug && nachZeile == vonZeile + 2 * richtung && getFigur(vonZeile + richtung, vonSpalte) == null) {
                return true;
            }
        }

        // Schlagzug
        if (Math.abs(vonSpalte - nachSpalte) == 1 && nachZeile == vonZeile + richtung) {
            Figur zielFigur = getFigur(nachZeile, nachSpalte);
            if (zielFigur != null && zielFigur.getFarbe() != bauer.getFarbe()) {
                return true;
            } else if (enPassantMoeglich == nachSpalte) {
                return true;
            }
        }

        return false;
    }

    private boolean istTurmZugGueltig(int vonZeile, int vonSpalte, int nachZeile, int nachSpalte) {
        if (vonZeile != nachZeile && vonSpalte != nachSpalte) {
            return false;
        }

        // Vertikale Bewegung
        if (vonSpalte == nachSpalte) {
            int start = Math.min(vonZeile, nachZeile) + 1;
            int ende = Math.max(vonZeile, nachZeile);
            for (int i = start; i < ende; i++) {
                if (getFigur(i, vonSpalte) != null) {
                    return false;
                }
            }
        }

        // Horizontale Bewegung
        if (vonZeile == nachZeile) {
            int start = Math.min(vonSpalte, nachSpalte) + 1;
            int ende = Math.max(vonSpalte, nachSpalte);
            for (int i = start; i < ende; i++) {
                if (getFigur(vonZeile, i) != null) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean istSpringerZugGueltig(int vonZeile, int vonSpalte, int nachZeile, int nachSpalte) {
        int zeilenDiff = Math.abs(vonZeile - nachZeile);
        int spaltenDiff = Math.abs(vonSpalte - nachSpalte);
        return (zeilenDiff == 2 && spaltenDiff == 1) || (zeilenDiff == 1 && spaltenDiff == 2);
    }

    private boolean istLaeuferZugGueltig(int vonZeile, int vonSpalte, int nachZeile, int nachSpalte) {
        if (Math.abs(vonZeile - nachZeile) != Math.abs(vonSpalte - nachSpalte)) {
            return false;
        }

        int zeilenRichtung = (nachZeile - vonZeile) > 0 ? 1 : -1;
        int spaltenRichtung = (nachSpalte - vonSpalte) > 0 ? 1 : -1;
        int zeile = vonZeile + zeilenRichtung;
        int spalte = vonSpalte + spaltenRichtung;

        while (zeile != nachZeile && spalte != nachSpalte) {
            if (getFigur(zeile, spalte) != null) {
                return false;
            }
            zeile += zeilenRichtung;
            spalte += spaltenRichtung;
        }

        return true;
    }

    private boolean istKoeniginZugGueltig(int vonZeile, int vonSpalte, int nachZeile, int nachSpalte) {
        return istTurmZugGueltig(vonZeile, vonSpalte, nachZeile, nachSpalte) || istLaeuferZugGueltig(vonZeile, vonSpalte, nachZeile, nachSpalte);
    }

    private boolean istKoenigPseudoLegal(int vonZeile, int vonSpalte, int nachZeile, int nachSpalte) {
        int zeilenDiff = Math.abs(vonZeile - nachZeile);
        int spaltenDiff = Math.abs(vonSpalte - nachSpalte);

        if (zeilenDiff <= 1 && spaltenDiff <= 1) {
            return true;
        }

        if (zeilenDiff == 0 && spaltenDiff == 2) {
            return istRochadeGueltig(vonZeile, vonSpalte, nachZeile, nachSpalte);
        }

        return false;
    }

    private boolean istRochadeGueltig(int vonZeile, int vonSpalte, int nachZeile, int nachSpalte) {
        int richtung = (vonSpalte < nachSpalte) ? 1 : -1;
        int turmSpalte = (vonSpalte < nachSpalte) ? 7 : 0;

        if (istKoenigBedroht(felder[vonZeile][vonSpalte].getFarbe())) {
            return false;
        }

        if (getFigur(vonZeile, vonSpalte).hatSichBewegt() == true) {
            return false;
        }

        if (getFigur(vonZeile, turmSpalte) == null || !(getFigur(vonZeile, turmSpalte) instanceof Turm) || getFigur(vonZeile, turmSpalte).hatSichBewegt() == true) {
            return false;
        }

        for (int k = vonSpalte + richtung; k != turmSpalte; k += richtung) {
            if (getFigur(vonZeile, k) != null || istFeldBedroht(vonZeile, k, getFigur(vonZeile, vonSpalte).getFarbe()))
                return false;
        }

        return true;
    }

    // Gibt den aktuellen Spielstatus zurück (0=läuft, +/-1=Matt, etc.).
    public int getSchachmatt() {
        return schachMatt;
    }

    // Aktualisiert die gespeicherte Position eines Königs.
    private void setzeKoenigPos(Figur.Farbe farbe, int zeile, int spalte) {
        if (farbe == Figur.Farbe.WEISS) {
            posKoenigWeiss[0] = zeile;
            posKoenigWeiss[1] = spalte;
        } else {
            posKoenigSchwarz[0] = zeile;
            posKoenigSchwarz[1] = spalte;
        }
    }

    // Eine Hilfsmethode, um eine Figur auf dem Brett zu platzieren und sie den Listen hinzuzufügen.
    private void platzieren(Figur figur, int zeile, int spalte) {
        figur.setZeile(zeile);
        figur.setSpalte(spalte);
        felder[zeile][spalte] = figur;
        if (figur.getFarbe() == Figur.Farbe.WEISS) {
            weisseFiguren.add(figur);
        } else {
            schwarzeFiguren.add(figur);
        }
    }

    /**
     * Generiert eine Liste aller pseudo-legalen Züge für eine Figur auf einem Feld.
     * Diese Züge müssen danach noch mit `istZugGueltig` auf Schach-Sicherheit geprüft werden.
     */
    public ArrayList<int[]> getLegaleZuege(int zeile, int spalte) {
        Figur figur = getFigur(zeile, spalte);
        if (figur == null) return new ArrayList<>();

        ArrayList<int[]> moeglicheFelder = new ArrayList<>();

        if (figur instanceof Bauer) {
            int richtung = (figur.getFarbe() == Figur.Farbe.WEISS) ? 1 : -1;
            pruefenUndHinzufuegen(zeile, spalte, zeile + richtung, spalte, moeglicheFelder);
            pruefenUndHinzufuegen(zeile, spalte, zeile + 2 * richtung, spalte, moeglicheFelder);
            pruefenUndHinzufuegen(zeile, spalte, zeile + richtung, spalte - 1, moeglicheFelder);
            pruefenUndHinzufuegen(zeile, spalte, zeile + richtung, spalte + 1, moeglicheFelder);
        } else if (figur instanceof Turm) {
            scanRichtungen(zeile, spalte, new int[][]{{0, 1}, {0, -1}, {1, 0}, {-1, 0}}, moeglicheFelder);
        } else if (figur instanceof Springer) {
            int[][] spruenge = {{-2, -1}, {-2, 1}, {-1, -2}, {-1, 2}, {1, -2}, {1, 2}, {2, -1}, {2, 1}};
            for (int[] s : spruenge) {
                pruefenUndHinzufuegen(zeile, spalte, zeile + s[0], spalte + s[1], moeglicheFelder);
            }
        } else if (figur instanceof Laeufer) {
            scanRichtungen(zeile, spalte, new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}}, moeglicheFelder);
        } else if (figur instanceof Koenigin) {
            scanRichtungen(zeile, spalte, new int[][]{{0, 1}, {0, -1}, {1, 0}, {-1, 0}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}}, moeglicheFelder);
        } else if (figur instanceof Koenig) {
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    if (!(i == 0 && j == 0)) {
                        pruefenUndHinzufuegen(zeile, spalte, zeile + i, spalte + j, moeglicheFelder);
                    }
                }
            }
            pruefenUndHinzufuegen(zeile, spalte, zeile, spalte + 2, moeglicheFelder);
            pruefenUndHinzufuegen(zeile, spalte, zeile, spalte - 2, moeglicheFelder);
        }
        return moeglicheFelder;
    }

    // Scannt in geraden oder diagonalen Linien nach möglichen Zügen (für Turm, Läufer, Dame).
    private void scanRichtungen(int zeile, int spalte, int[][] richtungen, ArrayList<int[]> liste) {
        for (int[] r : richtungen) {
            for (int i = 1; i < 8; i++) {
                int nachZeile = zeile + i * r[0];
                int nachSpalte = spalte + i * r[1];
                if (!istImBrett(nachZeile, nachSpalte)) break;
                if (istZugGueltig(zeile, spalte, nachZeile, nachSpalte)) {
                    liste.add(new int[]{nachZeile, nachSpalte});
                }
                if (getFigur(nachZeile, nachSpalte) != null) break;
            }
        }
    }

    // Prüft, ob ein Zug gültig ist und fügt ihn ggf. einer Liste hinzu.
    private void pruefenUndHinzufuegen(int vonZeile, int vonSpalte, int nachZeile, int nachSpalte, ArrayList<int[]> liste) {
        if (istZugGueltig(vonZeile, vonSpalte, nachZeile, nachSpalte)) {
            liste.add(new int[]{nachZeile, nachSpalte});
        }
    }

    // Stellt alle Figuren in ihre Startpositionen und initialisiert die Listen.
    private void initialisiereBrett() {
        weisseFiguren.clear();
        schwarzeFiguren.clear();

        // Weiße Figuren
        platzieren(new Turm(Figur.Farbe.WEISS), 0, 0);
        platzieren(new Springer(Figur.Farbe.WEISS), 0, 1);
        platzieren(new Laeufer(Figur.Farbe.WEISS), 0, 2);
        platzieren(new Koenigin(Figur.Farbe.WEISS), 0, 3);
        platzieren(new Koenig(Figur.Farbe.WEISS), 0, 4);
        posKoenigWeiss[0] = 0;
        posKoenigWeiss[1] = 4;
        platzieren(new Laeufer(Figur.Farbe.WEISS), 0, 5);
        platzieren(new Springer(Figur.Farbe.WEISS), 0, 6);
        platzieren(new Turm(Figur.Farbe.WEISS), 0, 7);
        for (int i = 0; i < 8; i++) {
            platzieren(new Bauer(Figur.Farbe.WEISS), 1, i);
        }

        // Schwarze Figuren
        platzieren(new Turm(Figur.Farbe.SCHWARZ), 7, 0);
        platzieren(new Springer(Figur.Farbe.SCHWARZ), 7, 1);
        platzieren(new Laeufer(Figur.Farbe.SCHWARZ), 7, 2);
        platzieren(new Koenigin(Figur.Farbe.SCHWARZ), 7, 3);
        platzieren(new Koenig(Figur.Farbe.SCHWARZ), 7, 4);
        posKoenigSchwarz[0] = 7;
        posKoenigSchwarz[1] = 4;
        platzieren(new Laeufer(Figur.Farbe.SCHWARZ), 7, 5);
        platzieren(new Springer(Figur.Farbe.SCHWARZ), 7, 6);
        platzieren(new Turm(Figur.Farbe.SCHWARZ), 7, 7);
        for (int i = 0; i < 8; i++) {
            platzieren(new Bauer(Figur.Farbe.SCHWARZ), 6, i);
        }
    }
}
