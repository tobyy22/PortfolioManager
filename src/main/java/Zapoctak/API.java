package Zapoctak;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/*
Trida se stara o API – zpusob, jak se aktualizuji ceny jednotlivych aktiv.

Pokusil jsem se to navrhnout tak, aby bylo API snadno rozsiritelne i pro API
jinych aplikaci. Pri implementaci noveho API staci, kdyz toto API prepise
abstraktni funkce validAsset, createURLAddress a parsePrice a lze jednoduse
vytvorit API pro novy typ aktiv.

API pracuje se vsemi aktivy jednoho typu naraz. Napriklad u kryptomen neaktualizuje
postupne bitcoin, ethereum, litecoin,...zvlast, ale ceny vsech techto aktiv ziska v
ramci jednoho callu – je to rychlejsi a umoznuje to delat podstatne mene API callu.
Samozrejme za predpokladu, ze API serveru, odkud se data ziskavaji, umoznuje ziskat
vice hodnot v ramci jednoho callu. Pokud to neumoznuje, je pro me toto API prakticky
nepouzitelne – nejen z hlediska rychlosti, ale spise proto, ze vetsina free licenci
ma omezeny pocet api callu (jedna se versinou o cca desitky za minutu), tudiz pokud
bych mel v portfoliu stovky mnoho aktiv jednoho druhu, velice obtizne by se to aktualizovalo.
 */

abstract class API {
    /*
    Pri pridavani assetu se zavola tato funkce, ktera vrati bud cenu aktiva ve formatu String
    anebo vrati "-1" coz znamena, ze asset neexistuje.
    Zaporne hodnoty u libovolneho assetu nedavaji smysl a nepredpokladam, ze by je api mohlo vratit.
     */
    abstract String validAsset(String name);

    /*
    Dostane ArrayList stringu – nazvy aktiv a vytvori z toho adresu pro api call.
     */
    abstract String createURLAddress(ArrayList<String> assetNames);

    /*
    Dostane data ze serveru, jmena aktiv, jejichz ceny chceme naparsovat a vrati ArrayList stringu,
    coz budou ceny jednotlivych aktiv.
     */
    abstract ArrayList<String> parsePrice(String data, ArrayList<String> assetNames);

    /*
    Funkce, ktera bude pro kazde api stejna – dostane adresu a vrati odpoved ze serveru –
    oboji stringy.
     */
    String apiCall(String address) {
        try {
            BufferedReader reader;
            String line;
            StringBuilder response = new StringBuilder();
            URL url = new URL(address);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int status = connection.getResponseCode();

            if(status > 299) {
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                while((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                return null;
            }
            else {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                while((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
            }
            return response.toString();
        }

        catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /*
    Opet funkce pro vsechna api pri spravne implementaci abstraktnich metod – dostane jmena assetu,
    vrati jejich ceny.
     */
    public ArrayList<String> getPrice(ArrayList<Asset> assetNames) {
        if(assetNames.size() == 0) {
            return null;
        }
        ArrayList<String> temp = new ArrayList<>();
        for (int i = 0; i < assetNames.size(); i++) {
            temp.add(assetNames.get(i).symbol);
        }
        String address = createURLAddress(temp);
        String response = apiCall(address);

        ArrayList<String> parsedPrice = parsePrice(response, temp);
        if(parsedPrice == null) {
            return null;
        }
        if(parsedPrice.equals("0")){
            return null;
        }
        return parsedPrice;
    }
}

class cryptoAPI extends API {

    /*
    Funkce, ktera vyvtori adresu pro spravny api call. Jmena aktiv specifikovana v parametru.
    Adresa pro kazde api bude jina, proto je nutne ji v kazdem api predefinovat (implememtovat).
     */
    String createURLAddress(ArrayList<String> coinsNames) {
        //ulozim si natvrdo 2 casti adresy, pomoci kterych vyslednou adresu vytvorime.
        final String[] address = {"https://api.coingecko.com/api/v3/simple/price?ids=", "&vs_currencies=usd"};
        //vytvorim StringBuilder, kam pridam prvni cast adresy
        StringBuilder build_address = new StringBuilder();
        build_address.append(address[0]);
        //nasledne budu adresu stavet pomoci jmen jednotlivych aktiv
        for (int i = 0; i < coinsNames.size() - 1; i++) {
            build_address.append(coinsNames.get(i));
            build_address.append("%2C");
        }
        build_address.append(coinsNames.get(coinsNames.size()-1));
        //pridam druhou cast adresy
        build_address.append(address[1]);
        return build_address.toString();
    }
    ArrayList<String> parsePrice(String data, ArrayList<String> coinNames) {
        //edge case
        if(data == null) {
            return null;
        }
        try {
            //odpoved ze serveru budu parsovat pomoci json knihovny a ceny aktiv pridavat do arraylistu
            ArrayList<String> result = new ArrayList<>();
            //naparsuji celou odpoved
            JSONObject obj = new JSONObject(data);
            //nasledne budu ziskavat ceny jednotlivych sktiv specifikovanych v parametru
            for (int i = 0; i < coinNames.size(); i++) {
                JSONObject temp = (JSONObject) obj.get(coinNames.get(i));
                //defaultni a nemenna mena je usd
                result.add(temp.get("usd").toString());
            }
            return result;
        }
        catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    //popis vyse
    public String validAsset(String name) {
        ArrayList<String> temp = new ArrayList<>();
        temp.add(name);
        String address = createURLAddress(temp);
        String resp = apiCall(address);
        if(resp == null) {
            System.out.println("Unknown problem");
            return "-1";
        }
        if(resp.equals("{}")) {
            return "-1";
        }
        JSONObject obj1 = new JSONObject(resp);
        JSONObject obj2 = (JSONObject) obj1.get(name);
        return  obj2.get("usd").toString();

    }
}

