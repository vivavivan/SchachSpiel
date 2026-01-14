package Spiel;

import java.util.Random;

public class RandomBotSpieler extends BotSpieler{
    public RandomBotSpieler(Figur.Farbe farbe) {
        super(farbe);
    }

    @Override
    public int[] berechneZug(Brett brett) {
        int[] alleZuege = generiereAlleZuege(brett, farbe);
        Random random = new Random();
        int randomIndex = random.nextInt(alleZuege.length);
        return zugDekodieren(alleZuege[randomIndex]);
    }
}
