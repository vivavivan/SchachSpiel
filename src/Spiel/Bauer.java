package Spiel;

public class Bauer extends Figur {
    public Bauer(Farbe farbe) {
        super(farbe);
        if (farbe == Farbe.WEISS) {
            setFigurIcon("\u2659");
        } else {
            setFigurIcon("\u265F");
        }
    }
}
