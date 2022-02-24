package Zapoctak;

import java.util.Scanner;

/*
Zde se resi trivialni logika cele aplikace - nacte se input, vytvori se prikaz a ten se zprocesuje.
 */
public class Main {
    public static void main(String[] args) {
        Scanner readinput = new Scanner(System.in);
        //instance portfolia
        Portfolio portfolio = new Portfolio();

        while(readinput.hasNext()) {
            String input = readinput.nextLine();
            Command com = Command.createCommand(input, portfolio);
            com.process();
        }
    }
}
