package Spiel;

public abstract class Figur {

    private final Farbe farbe;
    private boolean hatSichBewegt = false;
    private int zeile;
    private int spalte;

    public Figur(Farbe farbe) {
        this.farbe = farbe;
    }

    public Farbe getFarbe() {
        return farbe;
    }

    public boolean hatSichBewegt() {
        return hatSichBewegt;
    }

    public int getZeile() {
        return zeile;
    }

    public void setZeile(int zeile) {
        this.zeile = zeile;
    }

    public int getSpalte() {
        return spalte;
    }

    public void setSpalte(int spalte) {
        this.spalte = spalte;
    }

    public void setHatSichBewegt(boolean hatSichBewegt) {
        this.hatSichBewegt = hatSichBewegt;
    }

    public enum Farbe {
        WEISS, SCHWARZ
    }
}
