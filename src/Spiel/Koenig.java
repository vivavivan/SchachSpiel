package Spiel;

public class Koenig extends Figur {
    public Koenig(Farbe farbe) {
        super(farbe);
        if (farbe == Farbe.WEISS) {
            setFigurIcon("\u2654");
        } else {
            setFigurIcon("\u265A");
        }
    }
}
