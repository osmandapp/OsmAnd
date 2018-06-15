package net.osmand.binary;

import java.util.LinkedHashMap;
import java.util.Map;

public class CommonWords {
	private static Map<String, Integer> commonWordsDictionary = new LinkedHashMap<>();
	private static void addCommon(String string) {
		commonWordsDictionary.put(string, commonWordsDictionary.size());
	}
	
	public static int getCommon(String name) {
//		if(true) {
//			// not ready for old versions yet
//			return -1;
//		}
		Integer i = commonWordsDictionary.get(name);
		return i == null ? -1 : i.intValue();
	}
	
	public static int getCommonSearch(String name) {
		Integer i = commonWordsDictionary.get(name);
		return i == null ? -1 : i.intValue();
	}
	
	public static int getCommonGeocoding(String name) {
		Integer i = commonWordsDictionary.get(name);
		return i == null ? -1 : i.intValue();
	}
	
	static {
		addCommon("la");
		addCommon("via");
		addCommon("rua");
		addCommon("de");
		addCommon("du");
		addCommon("des");
		addCommon("del");
		addCommon("am");
		addCommon("da");
		addCommon("a");
		addCommon("der");
		addCommon("do");
		addCommon("los");
		addCommon("di");
		addCommon("im");
		addCommon("el");
		addCommon("e");
		addCommon("an");
		addCommon("g.");
		addCommon("rd");
		addCommon("dos");
		addCommon("dei");
		addCommon("b");
		addCommon("st");
		addCommon("the");
		addCommon("las");
		addCommon("f");
		addCommon("u");
		addCommon("jl.");
		addCommon("j");
		addCommon("sk");
		addCommon("w");
		addCommon("a.");
		addCommon("of");
		addCommon("k");
		addCommon("r");
		addCommon("h");
		addCommon("mc");
		addCommon("sw");
		addCommon("g");
		addCommon("v");
		addCommon("m");
		addCommon("c.");
		addCommon("r.");
		addCommon("ct");
		addCommon("e.");
		addCommon("dr.");
		addCommon("j.");
		addCommon("in");
		addCommon("al");
		addCommon("út");
		addCommon("per");
		addCommon("ne");
		addCommon("p");
		addCommon("et");
		addCommon("s.");
		addCommon("f.");
		addCommon("t");
		addCommon("fe");
		addCommon("à");
		addCommon("i");
		addCommon("c");
		addCommon("le");
		addCommon("s");
		addCommon("av.");
		addCommon("den");
		addCommon("dr");
		addCommon("y");
		addCommon("un");

		
		
		addCommon("van");
		addCommon("road");
		addCommon("street");
		addCommon("drive");
		addCommon("avenue");
		addCommon("rue");
		addCommon("lane");
		addCommon("улица");
		addCommon("спуск");
		addCommon("straße");
		addCommon("chemin");
		addCommon("way");

		addCommon("court");
		addCommon("calle");

		addCommon("place");

		addCommon("avenida");
		addCommon("boulevard");
		addCommon("county");
		addCommon("route");
		addCommon("trail");
		addCommon("circle");
		addCommon("close");
		addCommon("highway");
		
		addCommon("strada");
		addCommon("impasse");
		addCommon("utca");
		addCommon("creek");
		addCommon("carrer");
		addCommon("вулиця");
		addCommon("allée");
		addCommon("weg");
		addCommon("площадь");
		addCommon("тупик");

		addCommon("terrace");
		addCommon("jalan");
		
		addCommon("parkway");
		addCommon("переулок");
		
		addCommon("carretera");
		addCommon("valley");
		
		addCommon("camino");
		addCommon("viale");
		addCommon("loop");
		
		addCommon("bridge");
		addCommon("embankment");
		addCommon("township");
		addCommon("town");
		addCommon("village");
		addCommon("piazza");
		addCommon("della");
		
		addCommon("plaza");
		addCommon("pasaje");
		addCommon("expressway");
		addCommon("ruta");
		addCommon("square");
		addCommon("freeway");
		addCommon("line");
		
		addCommon("track");
		
		addCommon("zum");
		addCommon("rodovia");
		addCommon("sokak");
		addCommon("sur");
		addCommon("path");
		addCommon("das");
		
		addCommon("yolu");
		
		addCommon("проспект");

		addCommon("auf");
		addCommon("alley");
		addCommon("são");
		addCommon("les");
		addCommon("delle");
		addCommon("paseo");
		addCommon("alte");
		addCommon("autostrada");
		addCommon("iela");
		addCommon("autovía");
		addCommon("d");
		addCommon("ulica");
		
		addCommon("na");
		addCommon("проезд");
		addCommon("n");
		addCommon("ул.");
		addCommon("voie");
		addCommon("ring");
		addCommon("ruelle");
		addCommon("vicolo");
		addCommon("avinguda");
		addCommon("шоссе");
		addCommon("zur");
		addCommon("corso");
		addCommon("autopista");
		addCommon("провулок");
		addCommon("broadway");
		addCommon("to");
		addCommon("passage");
		addCommon("sentier");
		addCommon("aleja");
		addCommon("dem");
		addCommon("valle");
		addCommon("cruz");

		addCommon("bypass");
		addCommon("rúa");
		addCommon("crest");
		addCommon("ave");
		
		addCommon("expressway)");
		
		addCommon("autoroute");
		addCommon("crossing");
		addCommon("camí");
		addCommon("bend");
		
		addCommon("end");
		addCommon("caddesi");
		addCommon("bis");
		
		addCommon("ქუჩა");
		addCommon("kalea");
		addCommon("pass");
		addCommon("ponte");
		addCommon("cruce");
		addCommon("se");
		addCommon("au");

		addCommon("allee");
		addCommon("autobahn");
		addCommon("väg");
		addCommon("sentiero");
		addCommon("plaça");
		addCommon("o");
		addCommon("vej");
		addCommon("aux");
		addCommon("spur");
		addCommon("ringstraße");
		addCommon("prospect");
		addCommon("m.");
		addCommon("chaussee");
		addCommon("row");
		addCommon("link");
	
		addCommon("travesía");
		addCommon("degli");
		addCommon("piazzale");
		addCommon("vei");
		addCommon("waldstraße");
		addCommon("promenade");
		addCommon("puente");
		addCommon("rond-point");
		addCommon("vía");
		addCommon("pod");
		addCommon("triq");
		addCommon("hwy");
		addCommon("οδός");
		addCommon("dels");
		addCommon("and");

		addCommon("pré");
		addCommon("plac");
		addCommon("fairway");
	
// 		addCommon("farm-to-market");

		addCommon("набережная");

		addCommon("chaussée");

		addCommon("náměstí");
		addCommon("tér");
		addCommon("roundabout");
		addCommon("lakeshore");
		addCommon("lakeside");
		addCommon("alle");
		addCommon("gasse");
		addCommon("str.");
//		addCommon("p.");
		addCommon("ville");
		addCommon("beco");
		addCommon("platz");

// 		addCommon("porto");

		addCommon("sideroad");
		addCommon("pista");

		addCommon("аллея");
		addCommon("бульвар");
		addCommon("город");
		addCommon("городок");
		addCommon("деревня");
		addCommon("дер.");
		addCommon("пос.");
		addCommon("дорога");
		addCommon("дорожка");
		addCommon("кольцо");
		addCommon("мост");
		addCommon("остров");
		addCommon("островок");
		addCommon("поселок");
		addCommon("посёлок");
		addCommon("путепровод");
		addCommon("слобода");
		addCommon("станция");
		addCommon("тоннель");
		addCommon("тракт");
		addCommon("island");
		addCommon("islet");
		addCommon("tunnel");
		addCommon("stadt");
		addCommon("brücke");
		addCommon("damm");
		addCommon("insel");
		addCommon("dorf");
		addCommon("bereich");
		addCommon("überführung");
		addCommon("bulevar");
		addCommon("ciudad");
		addCommon("pueblo");
		addCommon("anillo");
		addCommon("muelle");
		addCommon("isla");
		addCommon("islote");
		addCommon("carril");
		addCommon("viaje");
		addCommon("città");
		addCommon("paese");
		addCommon("villaggio");
		addCommon("banchina");
		addCommon("isola");
		addCommon("isolotto");
		addCommon("corsia");
		addCommon("viaggio");
		addCommon("canale");
		addCommon("pont");
		addCommon("quai");
		addCommon("île");
		addCommon("îlot");
		addCommon("voyage");
		addCommon("descente");
		addCommon("straat");
		addCommon("stad");
		addCommon("dorp");
		addCommon("brug");
		addCommon("kade");
		addCommon("eiland");
		addCommon("eilandje");
		addCommon("laan");
		addCommon("plein");
		addCommon("reizen");
		addCommon("afkomst");
		addCommon("kanaal");
		addCommon("doodlopende");
		addCommon("stradă");
		addCommon("rutier");
		addCommon("alee");
		addCommon("municipiu");
		addCommon("oras");
		addCommon("drumuri");
		addCommon("poduri");
		addCommon("cheu");
		addCommon("insula");
		addCommon("ostrov");
		addCommon("sat");
		addCommon("călătorie");
		addCommon("coborâre");
		addCommon("statie");
		addCommon("tunel");
		addCommon("fundătură");
		addCommon("ulice");
		addCommon("silnice");
		addCommon("bulvár");
		addCommon("město");
		addCommon("obec");
		addCommon("most");
		addCommon("nábřeží");
		addCommon("ostrova");
		addCommon("ostrůvek");
		addCommon("lane");
		addCommon("vesnice");
		addCommon("jezdit");
		addCommon("sestup");
		addCommon("nádraží");
		addCommon("kanál");
		addCommon("ulička");
		addCommon("gata");
		addCommon("by");
		addCommon("bro");
		addCommon("kaj");
		addCommon("ö");
		addCommon("holme");
		addCommon("fyrkant");
		addCommon("resa");
		addCommon("härkomst");
		addCommon("kanal");
		addCommon("återvändsgränd");
		addCommon("cesty");
		addCommon("ostrovček");
		addCommon("námestie");
		addCommon("dediny");
		addCommon("jazdiť");
		addCommon("zostup");
		addCommon("stanice");
		addCommon("cesta");
		addCommon("pot");
		addCommon("mesto");
		addCommon("kraj");
		addCommon("vas");
		addCommon("pomol");
		addCommon("otok");
		addCommon("otoček");
		addCommon("trg");
		addCommon("potovanje");
		addCommon("spust");
		addCommon("postaja");
		addCommon("predor");
		addCommon("вуліца");
		addCommon("шаша");
		addCommon("алея");
		addCommon("горад");
		addCommon("мястэчка");
		addCommon("вёска");
		addCommon("дарога");
		addCommon("набярэжная");
		addCommon("востраў");
		addCommon("астравок");
		addCommon("завулак");
		addCommon("плошча");
		addCommon("пасёлак");
		addCommon("праезд");
		addCommon("праспект");
		addCommon("станцыя");
		addCommon("тунэль");
		addCommon("тупік");
		addCommon("افي.");
		addCommon("إلى");
		addCommon("تسوية");
		addCommon("جادة");
		addCommon("جزيرة");
		addCommon("جسر");
		addCommon("زقاق");
		addCommon("شارع");
		addCommon("طريق");
		addCommon("قرية");
		addCommon("مأزق");
		addCommon("محطة");
		addCommon("مدينة");
		addCommon("مرور");
		addCommon("مسار");
		addCommon("ممر");
		addCommon("منطقة");
		addCommon("نفق");
		addCommon("път");
		addCommon("булевард");
		addCommon("град");
		addCommon("село");
		addCommon("кей");
		addCommon("островче");
		addCommon("платно");
		addCommon("квадрат");
		addCommon("пътуване");
		addCommon("произход");
		addCommon("гара");
		addCommon("тунел");
		addCommon("канал");
		addCommon("körút");
		addCommon("híd");
		addCommon("rakpart");
		addCommon("állomás");
		addCommon("alagút");
		addCommon("đường");
		addCommon("đại");
		addCommon("làng");
		addCommon("cầu");
		addCommon("đảo");
		addCommon("phố");
		addCommon("gốc");
		addCommon("kênh");
		addCommon("δρόμο");
		addCommon("λεωφόρος");
		addCommon("πόλη");
		addCommon("κωμόπολη");
		addCommon("χωριό");
		addCommon("δρόμος");
		addCommon("γέφυρα");
		addCommon("αποβάθρα");
		addCommon("νησί");
		addCommon("νησίδα");
		addCommon("λωρίδα");
		addCommon("πλατεία");
		addCommon("χωριό");
		addCommon("ταξίδια");
		addCommon("ø");
		addCommon("bane");

	}


	
}
