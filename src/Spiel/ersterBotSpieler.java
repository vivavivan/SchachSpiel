package Spiel;

public class ersterBotSpieler extends BotSpieler {
    public ersterBotSpieler(Figur.Farbe farbe) {
        super(farbe);

    }

    @Override
    public int[] berechneZug(Brett brett) {
        return new int[0];
    }
}
