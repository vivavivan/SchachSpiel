package Spiel;

public class Koenigin extends Figur {
    public Koenigin(Farbe farbe) {
        super(farbe);
        if (farbe == Farbe.WEISS) {
            setFigurIcon("\u2655");
        } else {
            setFigurIcon("\u265B");
        }
    }
}
