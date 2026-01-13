package Spiel;

public abstract class Spieler {
    protected final Figur.Farbe farbe;

    protected Spieler(Figur.Farbe farbe) {
        this.farbe = farbe;
    }

    public Figur.Farbe getFarbe() {
        return farbe;
    }

    public abstract boolean istBot();
}
