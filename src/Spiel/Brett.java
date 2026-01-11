package Spiel;

import java.util.ArrayList;

public class Brett {

    private final Figur[][] felder = new Figur[8][8];
    private final Figur[][] testfeld = new Figur[8][8];

    private final int[] posKoenigWeiss = new int[2];
    private final int[] posKoenigSchwarz = new int[2];

    private final ArrayList<Figur> weisseFiguren = new ArrayList<>();
    private final ArrayList<Figur> schwarzeFiguren = new ArrayList<>();

    private int enPassantMoeglich = -1;
    //-2 für Schwarz im patt, -1 für schwarz im matt, 0 für nichts, 1 für weiss im matt, 2 für weiss im patt, 3 für Remie
    private int schachMatt = 0;

    public Brett() {
        initialisiereBrett();
    }

    public Figur getFigur(int zeile, int spalte) {
        return felder[zeile][spalte];
    }


    public void bewegeFigur(int vonZeile, int vonSpalte, int nachZeile, int nachSpalte) {
        bewegeFigur(felder, vonZeile, vonSpalte, nachZeile, nachSpalte);
    }

    private void bewegeFigur(Figur[][] felder, int vonZeile, int vonSpalte, int nachZeile, int nachSpalte) {
        Figur figur = felder[vonZeile][vonSpalte];
        Figur.Farbe gegnerFarbe = (figur.getFarbe() == Figur.Farbe.WEISS) ? Figur.Farbe.SCHWARZ : Figur.Farbe.WEISS;
        ArrayList<Figur> gegnerFiguren = (gegnerFarbe == Figur.Farbe.WEISS) ? weisseFiguren : schwarzeFiguren;

        // Schlagzug: Gegnerische Figur aus Liste entfernen
        if (felder[nachZeile][nachSpalte] != null) {
            gegnerFiguren.remove(felder[nachZeile][nachSpalte]);
        }

        //enPassant Logik: Der andere Bauer muss verschwinden
        if (figur instanceof Bauer && vonSpalte != nachSpalte && felder[nachZeile][nachSpalte] == null) {
            Figur opfer = felder[vonZeile][nachSpalte];
            felder[vonZeile][nachSpalte] = null;
            if (opfer != null) gegnerFiguren.remove(opfer);
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
            setKoenigPos(figur.getFarbe(), nachZeile, nachSpalte);
        }

        wirdEnPassantMoeglich(figur, vonZeile, vonSpalte, nachZeile, nachSpalte);

        if (figur != null) figur.setHatSichBewegt(true);

        spielZuende(gegnerFarbe);

        System.out.println("Schwarz: " + istKoenigBedroht(Figur.Farbe.SCHWARZ));
        System.out.println("Weiss: " + istKoenigBedroht(Figur.Farbe.WEISS) + "\nSchachmatt?: " + schachMatt);
        System.out.println(enPassantMoeglich);
    }

    public void promoviereBauer(int zeile, int spalte, String neuerTyp) {
        Figur bauer = felder[zeile][spalte];
        if (bauer == null || !(bauer instanceof Bauer)) return;

        Figur.Farbe farbe = bauer.getFarbe();
        ArrayList<Figur> figurenListe = (farbe == Figur.Farbe.WEISS) ? weisseFiguren : schwarzeFiguren;

        // Alten Bauern entfernen
        figurenListe.remove(bauer);

        Figur neueFigur;
        switch (neuerTyp) {
            case "Turm": neueFigur = new Turm(farbe); break;
            case "Springer": neueFigur = new Springer(farbe); break;
            case "Läufer": neueFigur = new Laeufer(farbe); break;
            default: neueFigur = new Koenigin(farbe); break; // Standard ist Dame
        }

        // Neue Figur platzieren (nutzt deine existierende Hilfsmethode)
        platzieren(neueFigur, zeile, spalte);

        // Spielstatus neu berechnen, da die neue Figur Schach geben könnte
        spielZuende((farbe == Figur.Farbe.WEISS) ? Figur.Farbe.SCHWARZ : Figur.Farbe.WEISS);
    }

    public boolean istZugGueltig(int vonZeile, int vonSpalte, int nachZeile, int nachSpalte) {
        Figur ziehendeFigur = felder[vonZeile][vonSpalte];
        if (ziehendeFigur == null) return false;

        if (!zugLogik(vonZeile, vonSpalte, nachZeile, nachSpalte)) return false;

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
            setKoenigPos(ziehendeFigur.getFarbe(), nachZeile, nachSpalte);
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
            setKoenigPos(ziehendeFigur.getFarbe(), vonZeile, vonSpalte);
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

    public boolean zugLogik(int vonZeile, int vonSpalte, int nachZeile, int nachSpalte) {
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
            return istKoenigZugGueltig(vonZeile, vonSpalte, nachZeile, nachSpalte);
        }

        return false;
    }

    private void testFeldAktualisieren() {
        for (int i = 0; i <= 7; i++) {
            for (int j = 0; j <= 7; j++) {
                testfeld[i][j] = felder[i][j];
            }
        }
    }

    private boolean hatKeineLegalenZuege(Figur.Farbe farbe) {
        ArrayList<Figur> figuren = (farbe == Figur.Farbe.WEISS) ? weisseFiguren : schwarzeFiguren;

        for (Figur f : figuren) {
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
    
    public boolean spielZuende(Figur.Farbe koenigfarbe) {
        if(hatZuWenigMaterial()) {
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

    private boolean hatZuWenigMaterial() {
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
        // Nur Remis, wenn beide Läufer auf der gleichen Feldfarbe stehen
        if (weisseFiguren.size() == 2 && schwarzeFiguren.size() == 2) {
            return true;
        }

        if (gesamt == 4 && weisseSpringer == 1 && schwarzeSpringer == 1) {
            return true;
        }

        if (gesamt == 4 && (weisseSpringer == 2 || schwarzeSpringer == 2)) {
            return true;
        }

        return false;
    }

    private boolean istFeldBedroht(int zeile, int spalte, Figur.Farbe eigeneFarbe) {
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

    public int[] findeKoenig(Figur.Farbe farbe) {
        return (farbe == Figur.Farbe.WEISS) ? posKoenigWeiss : posKoenigSchwarz;
    }

    public boolean istKoenigBedroht(Figur.Farbe eigeneFarbe) {
        int[] koenig = findeKoenig(eigeneFarbe);
        return istFeldBedroht(koenig[0], koenig[1], eigeneFarbe);
    }

    private void wirdEnPassantMoeglich(Figur figur, int vonZeile, int vonSpalte, int nachZeile, int nachSpalte) {

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

    private boolean istKoenigZugGueltig(int vonZeile, int vonSpalte, int nachZeile, int nachSpalte) {
        int zeilenDiff = Math.abs(vonZeile - nachZeile);
        int spaltenDiff = Math.abs(vonSpalte - nachSpalte);

        if (zeilenDiff <= 1 && spaltenDiff <= 1) {
            return true;
        }

        if (zeilenDiff == 0 && spaltenDiff == 2) {
            return rochade(vonZeile, vonSpalte, nachZeile, nachSpalte);
        }

        return false;
    }

    private boolean rochade(int vonZeile, int vonSpalte, int nachZeile, int nachSpalte) {
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

    public int getSchachmatt() {
        return schachMatt;
    }

    private void setKoenigPos(Figur.Farbe farbe, int zeile, int spalte) {
        if (farbe == Figur.Farbe.WEISS) {
            posKoenigWeiss[0] = zeile;
            posKoenigWeiss[1] = spalte;
        } else {
            posKoenigSchwarz[0] = zeile;
            posKoenigSchwarz[1] = spalte;
        }
    }

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
