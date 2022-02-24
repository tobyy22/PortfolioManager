package Zapoctak;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/*
Vlakno starajici se o to, aby se v pravidelnych intervalech updatovala hodnota aktiv v portfoliu a aby
se pravidelne ukladala data. Portfolio si toto vlakno v urcitych intervalech vola.
 */

class updatePortfolio implements Runnable {
    Portfolio pf;
    public updatePortfolio(Portfolio pf) { this.pf = pf; }
    public void run()
    {
        pf.updateNetWorth(true);
        pf.saveData();
    }
}

/*
Trida reprezentujici samotne portfolio.
 */
class Portfolio {
    //promena sdruzujici vsechna aktiva typu c (kryptomeny)
    Assets cryptoAssets;
    /*
    Promena sdruzujici cryptoAssets, pripadne stockAssets a jine,...
    cryptoAssets, pripadne stockAssets a jine implemetuji stejne funkce,
    diky cemuz je pak promenna allAssets zpusob, jak pristupovat ke VSEM aktivum najednou
    Pro implementaci dalsich typu aktiv by pak stacilo implemetovat API, vytvorit pro nej
    zde promennou, v konstruktoru ji pridat do allAssets + samozrejme trivialnim zpusobem
    implemetovat tridu - viz trida CryptoAssets.

    Puvodne jsem zamyslel reprezentovat data jako polymorfni ArrayList, kde kazdy jednotlivy asset
    by se aktualozoval zvlast. Pak jsem ale objevil problemy popsane v api - mnoho api callu ->
    pomale, vetsina free api to ani neumoznuje. Proto jsem data dekomponoval na teto urovni.
     */
    Assets[] allAssets;

    //celkova hodnota portfolia
    BigDecimal netWorth;

    //datove uloziste
    Data data;

    //trida starajici se o periodicke spousteni vlakna updatePortfolio
    ScheduledExecutorService scheduledUpdate = Executors.newScheduledThreadPool(11);

    //konstruktor
    Portfolio(){
        data = new Data();
        cryptoAssets = new CryptoAssets();
        allAssets = new Assets[]{cryptoAssets};

        //nacteme data z fyzickeho ulozeni
        for (int i = 0; i < allAssets.length; i++) {
            data.loadAssets(allAssets[i]);
        }
        //spustime periodicky vlakno
        scheduledUpdate.scheduleAtFixedRate(new updatePortfolio(this),1,15, TimeUnit.SECONDS);
        System.out.println("Portfolio ready to use.");
    }

    /*
    Nasledujici funkce jsou synchronized z toho duvodu, ze pristupuji a pripadne updatuji promennou netWorth.
    Uzivatele to nijak neomezuje, protoze prikazy se zpracovavaji sekvencne a vzdy se vola max jedna funkce.
    Problem by mohl byt, kdyz by zrovna vlakno aktualizovalo hodnoty aktiv a uzivatel by se v tu dobu zeptal
    na promennou networth – v takovy moment tam muze byt "cokoliv" - radeji necham to vlakno dobehnout a pak
    zpracuji pozadavek. Uzivatel zpozdeni nejspise ani nepozna.
    */

    //pridani assetu do portfolia, najdu typ assetu odpovidajici parametru a zavolam na nem funcki addAsset
    public synchronized void addAsset(char type, String symbol, String amount){
        for (int i = 0; i < allAssets.length; i++) {
            if (allAssets[i].type == type) {
                allAssets[i].addAsset(symbol, amount);
                updateNetWorth(false);
                return;
            }
        }
        System.out.println("Unknown asset type.");
    }

    //stejnym zposubem odebrani assetu
    public synchronized void removeAsset(char type, String symbol){
        for (int i = 0; i < allAssets.length; i++) {
            if (allAssets[i].type == type) {
                allAssets[i].removeAsset(symbol);
                updateNetWorth(false);
                return;
            }
        }
        System.out.println("Unknown asset type.");
    }

    public synchronized void showNetWorth() {
        System.out.println(netWorth);
    }
    public synchronized void showNetWorth(char type) {
        for (int i = 0; i < allAssets.length; i++) {
            if (allAssets[i].type == type) {
                System.out.println(allAssets[i].totalPrice);
                return;
            }
        }
        System.out.println("Unknown asset type.");
    }

    public synchronized void showAssets() {
        for (int i = 0; i < allAssets.length; i++) {
            allAssets[i].showAssets();
            System.out.println(allAssets[i].assetName + " Net North: " + allAssets[i].totalPrice);
        }
        System.out.println("Total Net worth: " + netWorth);
    }
    public synchronized void showAssets(char type) {
        for (int i = 0; i < allAssets.length; i++) {
            if (allAssets[i].type == type) {
                allAssets[i].showAssets();
                System.out.println(allAssets[i].assetName + " Net North: " + allAssets[i].totalPrice);
                return;
            }
        }
        System.out.println("Unknown asset type.");
    }

    /*
    Funkce na aktualizaci networth. Parametr se pta, zda se maji jen spocitat nova hodnota portfolia
    bez aktualizace cen pres api anebo zda se maji aktualizovat - stahnout nove ceny.
     */
    public synchronized void updateNetWorth(boolean updateAPI) {
        netWorth = new BigDecimal(0);
        for (int i = 0; i < allAssets.length; i++) {
            if(updateAPI) {
                allAssets[i].updatePrices();
            }
            allAssets[i].updateTotalPrice();
            netWorth = netWorth.add(allAssets[i].totalPrice);
        }
    }

    public void saveData() {
        for (int i = 0; i < allAssets.length; i++) {
            data.saveAssets(allAssets[i]);
        }
    }
}

/*
Abstraktni trida reprezentujici mnozinu aktiv stejneho typu.
Potomci budou v zasade uplne stejni, jen budou pouzivat jine api + samozrejme jiny
assetName a jiny type.
 */
abstract class Assets {
    char type;
    API api;
    ArrayList<Asset> assetsArray;
    public BigDecimal totalPrice;
    String assetName;

    /*
    Konstruktor vytvari jen assetsArray, o ostatni promenne se staraji konstruktory potomku.
     */
    public Assets() {
        assetsArray = new ArrayList<>();
    }
    /*
    Funkce na pridani assetu a jeho mnozstvi.
     */
    public void addAsset(String name, String amount) {
        //na vstupu neresim lower upper case - vse se prevede
        name = name.toLowerCase();
        //pokusim se asset najit podle jmena (symbolu)
        for (int i = 0; i < assetsArray.size(); i++) {
            //pokud ho najdu
            if(assetsArray.get(i).symbol.equals(name)) {
                //spocitam nove mnozstvi assetu v portfoliu
                BigDecimal newAmount = assetsArray.get(i).amount.add(new BigDecimal(amount));
                //pokud by nove mnozstvi nebylo kladne, asset smazu
                if(newAmount.compareTo(new BigDecimal(0)) <= 0) {
                    assetsArray.remove(i);
                    System.out.println("Asset removed.");
                    return;
                }
                //jinak nove mnozstvi prictu ke stavajicimu assetu
                assetsArray.get(i).amount = newAmount;
                System.out.println("Added to existing asset.");
                return;
            }
        }
        //asset jsem nenasel, pomoci api zkontroluju, zda existuje
        BigDecimal price = new BigDecimal(api.validAsset(name));
        //pokud se rovna "-1" - viz api dokumentace, znamena to, ze asset neni podporovany
        if(price.compareTo(new BigDecimal(-1)) == 0) {
            System.out.println("Asset not supported.");
            return;
        }
        //v opacnem pripade zkonvertuji pridavane mnozstvi
        BigDecimal convertedAmount = new BigDecimal(amount);
        //zaporne mnozstvi assetu v portfoliu nechci podporovat
        if(convertedAmount.compareTo(new BigDecimal(0)) <= 0) {
            System.out.println("Amount must be positive.");
            return;
        }
        //asset z hodnot vytvorim
        Asset newAsset = new Asset(name, convertedAmount);
        //nastavim mu cenu, ktera se mi vratila z kontroly (api.validAsset) - vim, ze je v poradku
        newAsset.price = price;
        //pridam asset
        assetsArray.add(newAsset);
        System.out.println("New asset added.");
    }

    /*
    Smazani assetu nehlede na mnozstvi.
     */
    public void removeAsset(String name) {
        BigDecimal assetAmount = null;
        //cyklem zkusim asset najit
        for(int i = 0; i < assetsArray.size(); i++) {
            //pokud najdu, ulozim si jeho mnozstvi
            if(assetsArray.get(i).symbol.equals(name)) {
                assetAmount = assetsArray.get(i).amount;
                break;
            }
            //nenajdu, funkci ukoncim
            if(i == assetsArray.size() - 1) {
                System.out.println("Asset not found in portfolio");
                return;
            }
        }
        //mnozstvi zneguju
        BigDecimal negativeAmount = assetAmount.negate();
        //znegovane mnozstvi odeberu
        addAsset(name, negativeAmount.toString());

    }

    /*
    Update cen vsech aktiv.
     */
    void updatePrices() {
        //reknu si o nove ceny
        ArrayList<String> newPrices = api.getPrice(assetsArray);
        //neznamy problem s api
        if(newPrices == null) {
            return;
        }

        //ceny cyklem updatnu, poradi vracene z api musi odpovidat poradi pozadavku
        for (int i = 0; i < assetsArray.size(); i++) {
            assetsArray.get(i).price = new BigDecimal(newPrices.get(i));
        }
    }
    public void updateTotalPrice() {
        //proste update celkove ceny portfolia
        totalPrice = new BigDecimal(0);
        for (int i = 0; i < assetsArray.size(); i++) {
            totalPrice = totalPrice.add(assetsArray.get(i).assetWorth());
        }
    }

    public void showAssets() {
        System.out.println(assetName);
        for (int i = 0; i < assetsArray.size(); i++) {
            assetsArray.get(i).showAsset();
        }
    }
}

/*
Potomek tridy Assets - pri implementaci noveho typu je nutne vytvorit noveho potomka, kteremu staci predat
spravny typ api, assetName pro vypis a jeho typ.
 */
class CryptoAssets extends Assets {
    public CryptoAssets(){
        super();
        api = new cryptoAPI();
        assetName = "Crypto";
        type = 'c';
    };
}

/*
Trida pro reprezentaci jednoho aktiva. Typ znat nemusim - nikde to neni potreba diky tomu, ze program
je dekomponovany na vyssi urovni.
 */
class Asset {
    //pamatuji si symbol(jmeno) assetu, mnozstvi a cenu za jednotku
    public final String symbol;
    public BigDecimal amount;
    public BigDecimal price;

    //2 temer totozne konstruktory
    Asset(String symbol, BigDecimal givenAmount) {
        this.symbol = symbol;
        this.amount = givenAmount;
    }

     Asset(String symbol, String givenAmount) {
         this.symbol = symbol;
         this.amount = new BigDecimal(givenAmount);
     }
    BigDecimal assetWorth() {
        //hodnota aktiva dana jednoduchym nasobenim
        return amount.multiply(price);
    }
    void showAsset() {
        /*jiz by se to stavat nemelo, ale teoreticky se muze stat, ze cena aktiva je null,
        ale pokud by se to stalo, chci se tomu branit.
         */
        if(price == null) {
            System.out.println(symbol + new String(new char[30-symbol.length()]).replace("\0", ".") + "Not updated yet");
            return;
        }
        System.out.println(symbol + new String(new char[30-symbol.length()]).replace("\0", ".") + price + " " + amount.toString() + " " + price.multiply(amount).toString());
    }



}





