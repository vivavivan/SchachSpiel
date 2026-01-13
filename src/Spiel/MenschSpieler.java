package Spiel;

public class MenschSpieler extends Spieler{

    public MenschSpieler(Figur.Farbe farbe) {
        super(farbe);
    }

    public boolean istBot() {
        return false;
    }
}
