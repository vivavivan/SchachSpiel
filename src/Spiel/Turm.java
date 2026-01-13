package Spiel;

public class Turm extends Figur {
    public Turm(Farbe farbe) {
        super(farbe);
        if (farbe == Farbe.WEISS) {
            setFigurIcon("\u2656");
        } else {
            setFigurIcon("\u265C");
        }
    }
}
