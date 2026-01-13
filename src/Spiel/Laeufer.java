package Spiel;

public class Laeufer extends Figur {
    public Laeufer(Farbe farbe) {
        super(farbe);
        if (farbe == Farbe.WEISS) {
            setFigurIcon("\u2657");
        } else {
            setFigurIcon("\u265D");
        }
    }
}
