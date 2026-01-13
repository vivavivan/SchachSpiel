package Spiel;

import java.util.Arrays;

public class ZugRegister {
    // Startgröße (wächst automatisch)
    private int[] zugSpeicher = new int[50];
    private int anzahlZuege = 0;

    public ZugRegister() {

    }

    public void registerZug(int vonZeile, int vonSpalte, int nachZeile, int nachSpalte, int istPromotion, int promotionTyp, int enPassant, int geschlageneFigurTyp, boolean warErsterZug, boolean hatOpferSichBewegt) {
        // 1. Prüfen, ob das Array voll ist
        if (anzahlZuege >= zugSpeicher.length) {
            // Array verdoppeln (sehr effizient)
            zugSpeicher = Arrays.copyOf(zugSpeicher, zugSpeicher.length * 2);
        }

        // Encoding:
        // 0-5: Start (Zeile<<3 | Spalte) | 6-11: Ziel | 12: Promo | 13-14: PromoTyp
        // 15-18: EnPassant (4 Bits für -1 bis 7) | 19-21: Geschlagene Figur | 22: War erster Zug
        // 23: Hat Opfer sich bewegt
        int start = (vonZeile << 3) | vonSpalte;
        int ziel = (nachZeile << 3) | nachSpalte;
        int zugWert = start | (ziel << 6) | (istPromotion << 12) | (promotionTyp << 13) | ((enPassant + 1) << 15)
                | (geschlageneFigurTyp << 19) | ((warErsterZug ? 1 : 0) << 22) | ((hatOpferSichBewegt ? 1 : 0) << 23);

        zugSpeicher[anzahlZuege++] = zugWert;
    }

    public int undo() {
        if (anzahlZuege > 0) {
            anzahlZuege--;
            return zugSpeicher[anzahlZuege];
        }
        return -1; // Kein Zug mehr da
    }
}
