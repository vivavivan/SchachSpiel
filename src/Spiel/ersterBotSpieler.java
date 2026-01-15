package Spiel;

import java.util.List;

/**
 * Ein einfacher "gieriger" Bot.
 * Er schaut nur einen Zug voraus und wählt den Zug,
 * der sofort den höchsten Materialgewinn verspricht.
 */
public class ersterBotSpieler extends BotSpieler {

    public ersterBotSpieler(Figur.Farbe farbe) {
        super(farbe);
    }

    // Die Hauptmethode des Bots.
    // Generiert alle Züge, simuliert jeden einzelnen und wählt den mit der besten Bewertung.
    @Override
    public int[] berechneZug(Brett brett) {
        Brett brettKopie = brett.erstelleKopie();

        // 1. Alle legalen Züge für die aktuelle Farbe holen
        int[] alleZuege = generiereAlleZuege(brettKopie, this.farbe);

        // Züge mischen, damit bei gleicher Bewertung nicht immer der gleiche Zug gewählt wird
        mischeZuege(alleZuege);

        if (alleZuege.length == 0) return null;

        int besterZugEncoded = alleZuege[0];
        int besteBewertung = Integer.MIN_VALUE;

        // 2. Jeden Zug simulieren und bewerten
        for (int zugEncodiert : alleZuege) {
            int[] zugDecodiert = zugDekodieren(zugEncodiert);

            // Zug auf dem Brett ausführen
            brettKopie.bewegeFigur(zugDecodiert[0], zugDecodiert[1], zugDecodiert[2], zugDecodiert[3], zugDecodiert[4], zugDecodiert[5]);

            // Stellung bewerten
            int aktuelleBewertung = evaluieren(brettKopie);

            // Zug sofort wieder rückgängig machen
            brettKopie.undo();

            // Wenn dieser Zug besser ist als der bisherige, speichern
            if (aktuelleBewertung > besteBewertung) {
                besteBewertung = aktuelleBewertung;
                besterZugEncoded = zugEncodiert;
            }
        }

        // Den besten gefundenen Zug dekodieren und zurückgeben
        return zugDekodieren(besterZugEncoded);
    }

    /*
     * Bewertet die aktuelle Brettstellung.
     * Hauptsächlich basierend auf dem Materialwert, mit einem kleinen Bonus für ein Schachgebot.
     */
    private int evaluieren(Brett brett) {
        // Falls das Spiel durch den Zug endet
        int status = brett.getSchachmatt();
        boolean istSchach = brett.istKoenigBedroht(gegnerFarbe());
        int wert = 0;
        if (this.farbe == Figur.Farbe.WEISS) {
            if (status == -1) return 100000; // Sieg für Weiß
            if (status == 1) return -100000;  // Sieg für Schwarz
        } else {
            if (status == 1) return 100000;  // Sieg für Schwarz
            if (status == -1) return -100000; // Sieg für Weiß
        }
        if (status == 2 || status == -2 || status == 3) return 0;

        if(istSchach) {
            wert += 30; //damit er manchmal schachsetzen kann;
        }

        return berechneMaterialWert(brett, this.farbe) + wert - berechneMaterialWert(brett, gegnerFarbe());
    }

    private int berechneMaterialWert(Brett brett, Figur.Farbe f) {
        int wert = 0;
        List<Figur> figuren = (f == Figur.Farbe.WEISS) ? brett.getWeisseFiguren() : brett.getSchwarzeFiguren();

        for (Figur figur : figuren) {
            if (figur instanceof Bauer) wert += 100;
            else if (figur instanceof Springer) wert += 300;
            else if (figur instanceof Laeufer) wert += 300;
            else if (figur instanceof Turm) wert += 500;
            else if (figur instanceof Koenigin) wert += 900;
            //else if (figur instanceof Koenig) wert += 10000;
        }
        return wert;
    }

    private Figur.Farbe gegnerFarbe() {
        return (this.farbe == Figur.Farbe.WEISS) ? Figur.Farbe.SCHWARZ : Figur.Farbe.WEISS;
    }
}