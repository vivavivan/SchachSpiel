package Spiel;

public class Springer extends Figur {
    public Springer(Farbe farbe) {
        super(farbe);
        if (farbe == Farbe.WEISS) {
            setFigurIcon("\u2658");
        } else {
            setFigurIcon("\u265E");
        }
    }
}
