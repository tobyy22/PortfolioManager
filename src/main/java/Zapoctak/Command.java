package Zapoctak;

import java.util.Arrays;

/*
Trida starajici se o uzivatelske rozhrani. Kazdy zakladni prikaz (show, add,...) je implementovany
pomoci potomka tridy command. Kazdy potomek povinne implemetuje funkci process, ktera zaridi
zprocesovani prikazu.
 */


public abstract class Command {

    /*
    Pole, kde jsou definovane zkratky pro prikazy nebo jejich parametry.
     */
    static final String[] addAcronyms = {"a", "add"};
    static final String[] newWorthAcronyms = {"nw", "networth"};
    static final String[] showAcronyms = {"sh", "show"};
    static final String[] removeAcronyms = {"remove", "rem"};
    static final String[] assetsAcronyms = {"as", "asset", "assets"};
    static final String[] exitAcronyms = {"exit", "ex"};

    /*
    Dale maji nektere prikazy (u kterych to dava smysl) referenci ma portfolio, na kterem provadi
    prikazy.
     */
    Portfolio portfolio;

    /*
    Cely naparsovany prikaz.
     */
    String[] tokenized;

    /*
    2 mozne kontruktory. Bezparametrovy konsturktor je bud prazdny anebo neznamy prikaz.
     */
    Command(){}
    Command(String[] input, Portfolio givenPortfolio) {
        tokenized = input;
        portfolio = givenPortfolio;
    }

    /*
    Funkce, ktera dostane input od uzivatele, podle prvniho tokenu rozpozna provadeny zakladni prikaz
    a vyvtori spravneho potomka. Pripadne preda tokenizovany vyraz a referenci na portfolio.

     */
    static Command createCommand(String input, Portfolio pf) {

        //naparsovani inputu
        String[] tokenizedInput = Arrays.stream(input.split("\\s")).filter(e -> e.trim().length() > 0).toArray(String[]::new);

        if(tokenizedInput.length == 0) {
            return new Empty();
        }
        /*
        Kontroluje, zda prvni token muze odpovidat prikazu Add, tedy ze prvni token je obsazen v addAcronyms.
        Pro ostatni prikazy totozne.
         */
        if (Arrays.asList(addAcronyms).contains(tokenizedInput[0])) {
            return new Add(tokenizedInput, pf);
        }
        if(Arrays.asList(newWorthAcronyms).contains(tokenizedInput[0])) {
            return new NetWorth(tokenizedInput, pf);
        }
        if(Arrays.asList(showAcronyms).contains(tokenizedInput[0])) {
            return new Show(tokenizedInput, pf);
        }
        if(Arrays.asList(removeAcronyms).contains(tokenizedInput[0])) {
            return new Remove(tokenizedInput, pf);
        }
        if(Arrays.asList(exitAcronyms).contains(tokenizedInput[0])) {
            return new Exit(tokenizedInput, pf);
        }
        return new Unknown();
    }
    abstract void process();
}

/*
Trida na pridavani aktiv do portfolia.
Ocekava se prikaz ve tvaru:
add 'type' 'jmenoAktiva' 'mnozstvi'
type je programem dany identifikator druhu aktiva typu char, pro kryptomeny je to 'c'

Odstraneni specifikovatelneho mnozstvi probiha take pomoci teto metody – proste se pred
odebirane mnozstvi da minus.
 */
class Add extends Command {
    Add(String[] tokenized, Portfolio givenPortfolio){
        super(tokenized, givenPortfolio);
    }
    void process() {
        //input musi mit 4 tokeny - viz tvar prikazu
        if(tokenized.length != 4) {
            System.out.println("Please specify type of asset, asset name and amount.");
            return;
        }

        //musi byt identifikator
        if(tokenized[1].length() != 1) {
            System.out.println("Incorrect input.");
            return;
        }
        //pokusim se naparsovat posledni token na Double – kontrola, ze je v decimalnim tvaru
        try {
            Double test = Double.parseDouble(tokenized[3]);
        }
        catch (NumberFormatException e) {
            System.out.println("Incorrect input.");
            return;
        }
        //pridam asset
        portfolio.addAsset(tokenized[1].charAt(0), tokenized[2], tokenized[3]);
    }
};

/*
Remove odstrani asset bez specifikace mnozstvi – proste odstrani vse
 */
class Remove extends Command {
    Remove(String[] tokenized, Portfolio givenPortfolio){
        super(tokenized, givenPortfolio);
    }
    void process() {

        //nespecifikuje se mnozstvi, tedy pouze 3 tokeny
        if(tokenized.length != 3) {
            System.out.println("Incorrect input.");
            return;
        }
        portfolio.removeAsset(tokenized[1].charAt(0), tokenized[2]);
    }
};

/*
Vicemene zkratka pro zobrazeni celkove hodnoty portfolia
 */
class NetWorth extends Command {
    NetWorth(String[] tokenized, Portfolio givenPortfolio){
        super(tokenized, givenPortfolio);
    }
    void process() {
        if(tokenized.length == 1) {
            portfolio.showNetWorth();
            return;
        }
        else if(tokenized.length == 2) {
            if(tokenized[1].length() != 1) {
                System.out.println("Asset type must be only one character.");
                return;
            }
            portfolio.showNetWorth(tokenized[1].charAt(0));
        }
        else {
            System.out.println("Incorrect input.");
            return;
        }
    }
};

/*
Prikaz pro zobrazovani informaci o portfoliu.
Mozne parametry jsou:
show assets - ukaze cele portfolio
show assets c - ukaze vsechny kryptomeny (nebo proste jen specifikovany druh aktiv)

show networth - ukaze hodnotu celeho portfolia
sh networth c - ukaze hodnotu jednoho druhu aktiv
 */
class Show extends Command {
    Show(String[] tokenized, Portfolio givenPortfolio){
        super(tokenized, givenPortfolio);
    }
    void process() {

        if(tokenized.length < 2) {
            System.out.println("Please specify parameter.");
            return;
        }

        //druhy prikaz jsou assets
        if(Arrays.asList(assetsAcronyms).contains(tokenized[1])) {
            //neobsahuje identifikator
            if(tokenized.length == 2) {
                portfolio.showAssets();
                return;
            }
            //obsahuje identifikator
            if(tokenized.length == 3) {
                //identifikator musi byt dlouhy jeden char
                if(tokenized[2].length() != 1) {
                    System.out.println("Asset type must be only one character.");
                    return;
                }
                portfolio.showAssets(tokenized[2].charAt(0));
                return;
            }

            //nema delku ani 2 ani 3 - neplatny prikaz
            else {
                System.out.println("Unknown parameters.");
                return;
            }
        }
        //totozne jako predchozi cast, akorat ukazuje jen networth
        else if(Arrays.asList(newWorthAcronyms).contains(tokenized[1])) {
            if(tokenized.length == 2) {
                portfolio.showNetWorth();
                return;
            }
            if(tokenized.length == 3) {
                if(tokenized[2].length() != 1) {
                    System.out.println("Asset type must be only one character.");
                    return;
                }
                portfolio.showNetWorth(tokenized[2].charAt(0));
                return;
            }
            else {
                System.out.println("Unknown parameters");
                return;
            }
        }
        else {
            System.out.println("Unknown parameters");
            return;
        }
    }
};

/*
Specialni komandy, ktere nic nedelaji.
 */
class Empty extends Command {
    Empty(){}
    void process() {
        System.out.println("Empty command");

    }
}

class Unknown extends Command {
    Unknown(){}
    void process() {
        System.out.println("Unknown command");
    }
}

class Exit extends Command {
    Exit(String[] tokenized, Portfolio givenPortfolio){
        super(tokenized, givenPortfolio);
    }
    void process() {
        portfolio.saveData();
        System.exit(0);
    }
}
