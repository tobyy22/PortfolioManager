package Zapoctak;

import java.io.*;
import java.util.Scanner;

/*
Trida vyuzivana portfoliem pro ukladani a nacitani dat. Funguje tak, ze dostane referenci na Assets a
podle typu z databaze nacte napriklad udaje o ulozenych kryptomnach (pripad typ = 'c')
V soucasne chvili implementovano pomoci jednoho souboru, kde si u kazdeho assetu pamatuji jeho typ
 */
public class Data {
    //cesta k databazi
    String path;
    //jmeno souboru s ulozenymi assets
    String assetsFileName;
    Data() {
        path = "";
        assetsFileName = path + "assets.txt";
        createDB();
    }

    /*
    Nacteni assetu z databaze
     */
    public void saveAssets(Assets assets) {

        String savedChar = null;

        //nejprve se podivam, jaky typ assetu me zajima (slo by resit i pomoci assets.type)
        if(assets instanceof CryptoAssets) {
            savedChar = "c";
        }

        //pokusim se zapsat data v csv formatu - symbol,mnozstvi,typ
        try {
            FileWriter file = new FileWriter(assetsFileName);
            for (int i = 0; i < assets.assetsArray.size(); i++) {
                file.write(assets.assetsArray.get(i).symbol + "," + assets.assetsArray.get(i).amount + "," + savedChar);
                file.write('\n');
            }
            file.close();
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }
    //nacteni dat
    public void loadAssets(Assets assets) {
        String searchedChar = null;
        //stejne jak vyse
        if(assets instanceof CryptoAssets) {
            searchedChar = "c";
        }
        //budu nacitat postupne soubor
        try {
            File myObj = new File(assetsFileName);
            Scanner myReader = new Scanner(myObj);
            //nacitam radku po radce
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                //parsuji podle csv
                String[] csvSplit = data.split(",");
                /*
                Koukam se, zda se jedna o typ, ktery me zajima.
                Pri vice typech to neni uplne efektivni a lepsi by mohlo byt pouzit vice tabulek.
                 */
                if(csvSplit[2].equals(searchedChar)) {
                    //pridam data do portfolia
                    assets.assetsArray.add(new Asset(csvSplit[0], csvSplit[1]));
                }
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    /*
    Vytvoreni tabulky databaze.
    Jen kontrola, zda file existuje a kdyztak se vytvori.
    V pripade, ze bych chtel pouzit pro soubory databaze jiny adresar, bylo by nutne jeste
    vytvaret samotny adresar.
     */
    private void createDB() {
        try {
            File myObj = new File(assetsFileName);
            if (myObj.createNewFile()) {
                System.out.println("Database created: " + myObj.getName());
            } else {
                System.out.println("Database already exists.");
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}
