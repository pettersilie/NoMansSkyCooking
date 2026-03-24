package de.nms.nmsrecipes.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EnglishTerminology {

    private EnglishTerminology() {
    }

    static Map<String, String> terms() {
        Map<String, String> terms = new LinkedHashMap<>();

        zip(terms, List.of("Köder"), List.of("Baits"));

        term(terms, "Aloe-Fleisch", "Aloe Flesh");
        term(terms, "Ausgeschöpfte Innereien", "Scooped Innards");
        term(terms, "Beinfleisch", "Leg Meat");
        term(terms, "Bittersüßer Kakao", "Bittersweet Cocoa");
        term(terms, "Chromatisches Metall", "Chromatic Metal");
        term(terms, "Dreckiges Fleisch", "Dirty Meat");
        term(terms, "Diplo-Stücke", "Diplo Chunks");
        term(terms, "Faecium", "Faecium");
        term(terms, "Feuerbeere", "Fireberry");
        term(terms, "Fleischige Flügel", "Meaty Wings");
        term(terms, "Fleischige Stücke", "Meaty Chunks");
        term(terms, "Fleischflocken", "Meat Flakes");
        term(terms, "Fleischige Wurzeln", "Pulpy Roots");
        term(terms, "Frische Milch", "Fresh Milk");
        term(terms, "Frostkristall", "Frost Crystal");
        term(terms, "Gammawurzel", "Gamma Root");
        term(terms, "Gefrorene Knollen", "Frozen Tubers");
        term(terms, "Gesammelte Pilze", "Foraged Mushrooms");
        term(terms, "Glaskörner", "Glass Grains");
        term(terms, "Grahbeere", "Grahberry");
        term(terms, "Große Eier", "Tall Eggs");
        term(terms, "Hexabeere", "Hexaberry");
        term(terms, "Heptaploid-Weizen", "Heptaploid Wheat");
        term(terms, "Holz-„Apfel“", "Crab 'Apple'");
        term(terms, "Hypnotisches Auge", "Hypnotic Eye");
        term(terms, "Impulsbohnen", "Impulse Beans");
        term(terms, "Innereienbeutel", "Offal Sac");
        term(terms, "Jadeerbsen", "Jade Peas");
        term(terms, "Kaktusfleisch", "Cactus Flesh");
        term(terms, "Kaktusnektar", "Cactus Nectar");
        term(terms, "Katzenleber", "Feline Liver");
        term(terms, "Kelpreis", "Kelp Rice");
        term(terms, "Kelpbeutel", "Kelp Sac");
        term(terms, "Klebriger „Honig“", "Sticky 'Honey'");
        term(terms, "Klumpiger Hirnstamm", "Lumpy Brainstem");
        term(terms, "Knochenklumpen", "Bone Nuggets");
        term(terms, "Knusprige Flügel", "Crunchy Wings");
        term(terms, "Kohlenstoff", "Carbon");
        term(terms, "Kohlenstoff-Nanorohr", "Carbon Nanotubes");
        term(terms, "Kreaturenei", "Creature Egg");
        term(terms, "Kreaturenkugeln", "Creature Pellets");
        term(terms, "Kristallfleisch", "Crystal Flesh");
        term(terms, "Kropfmilch", "Craw Milk");
        term(terms, "Kürbisfleisch", "Marrow Flesh");
        term(terms, "Kürbisknolle", "Marrow Bulb");
        term(terms, "Larvenkern", "Larval Core");
        term(terms, "Leopardenfrucht", "Leopard-Fruit");
        term(terms, "Mordit", "Mordite");
        term(terms, "Pilgerbeere", "Pilgrimberry");
        term(terms, "Pilzschimmel", "Fungal Mould");
        term(terms, "Protowurst", "ProtoSausage");
        term(terms, "Regis-Fett", "Regis Grease");
        term(terms, "Riesenei", "Giant Egg");
        term(terms, "Rohes Steak", "Raw Steak");
        term(terms, "Salz", "Salt");
        term(terms, "Salzige Fischfilets", "Salty Fingers");
        term(terms, "Schreiterwurst", "Strider Sausage");
        term(terms, "Schuppiges Fleisch", "Scaly Meat");
        term(terms, "Sievert-Bohnen", "Sievert Beans");
        term(terms, "Silikonei", "Silicon Egg");
        term(terms, "Solanium", "Solanium");
        term(terms, "Solartillo", "Solartillo");
        term(terms, "Sternenknolle", "Star Bulb");
        term(terms, "Süßwurzel", "Sweetroot");
        term(terms, "Synthetischer Honig", "Synthetic Honey");
        term(terms, "Ungiftiger Pilz", "Non-Toxic Mushroom");
        term(terms, "Unhold-Rogen", "Fiendish Roe");
        term(terms, "Verdichteter Kohlenstoff", "Condensed Carbon");
        term(terms, "Vergitterte Sehne", "Latticed Sinew");
        term(terms, "Vergitterte Sehnen", "Latticed Sinew");
        term(terms, "Verarbeitetes Fleisch", "Processed Meat");
        term(terms, "Verarbeiteter Zucker", "Processed Sugar");
        term(terms, "Verfeinertes Mehl", "Refined Flour");
        term(terms, "Warme Proto-Milch", "Warm Proto-Milk");
        term(terms, "Wilde Hefe", "Wild Yeast");
        term(terms, "Wilde Milch", "Wild Milk");
        term(terms, "Gedünstetes Gemüse", "Steamed Vegetables");
        term(terms, "Geräuchertes Fleisch", "Smoked Meat");

        zip(terms,
                List.of(
                        "Sahne", "Proto-Sahne", "Geschlagene Butter", "Proto-Butter", "Gebäck", "Knochenmilch",
                        "Knochenbutter", "Knochensahne (Käse)", "Sehr dickflüssiger Pudding", "Knochensahne",
                        "Klebriger Pudding", "Salziger Pudding", "Monströser Pudding", "Sternenpudding",
                        "Delikates Baiser", "Gesüßte Butter", "Gesüßte Proto-Butter", "Honigbutter",
                        "Proto-Butter mit Honig", "Klebrige Butter", "Klebrige Proto-Butter", "Würziger Käse",
                        "Proto-Käse", "Teig"
                ),
                List.of(
                        "Cream", "Proto-Cream", "Churned Butter", "Proto-Butter", "Pastry", "Bone Milk",
                        "Bone Butter", "Bone Cream 2", "Very Thick Custard", "Bone Cream", "Viscous Custard",
                        "Salty Custard", "Monstrous Custard", "Stellar Custard", "Delicate Meringue",
                        "Sweetened Butter", "Sweetened Proto-Butter", "Honey Butter", "Honied Proto-Butter",
                        "Gooey Butter", "Gooey ProtoButter", "Tangy Cheese", "ProtoCheese", "Dough"
                ));

        zip(terms,
                List.of("Knusperkaramell", "Bratöl", "Proto-Öl"),
                List.of("Crunchy Caramel", "Clarified Oil", "Proto-Oil"));

        zip(terms,
                List.of(
                        "Eisschreie", "Eiscreme", "Salziger Raureif", "Tödlich kalte Eiscreme", "Sterneneiscreme",
                        "Schokoladeneis", "Karamelleiscreme", "Fruchteiscreme", "„Apfel“-Eiscreme",
                        "Honigeiscreme", "Ewige Eiscreme", "Vy‘Eiscreme", "Eisiges Mark", "Würziges Eis"
                ),
                List.of(
                        "Iced Screams", "Ice Cream", "Briney Rime", "Deathly-Cold Ice Cream", "Stellar Ice Cream",
                        "Chocolate Ice Cream", "Caramel Ice Cream", "Fruity Ice Cream", "'Apple' Ice Cream",
                        "Honey Ice Cream", "Perpetual Ice Cream", "Vy'ice Cream", "Icey Marrow", "Spiced Ice"
                ));

        zip(terms,
                List.of(
                        "Brot", "Kuchenteig", "Proto-Teig", "Dicker, süßer Teig", "Heulender Teig",
                        "Extrafluffiger Teig", "Windender, aufgewühlter Teig"
                ),
                List.of(
                        "Bread", "Cake Batter", "Proto-Batter", "Thick, Sweet Batter", "Wailing Batter",
                        "Extra-Fluffy Batter", "Writhing, Roiling Batter"
                ));

        zip(terms,
                List.of(
                        "Schrecklicher Brei", "Schimmelpilzomelette", "Proto-Omelett", "Parasitenomelett",
                        "Omelett", "Gerührtes Knochenmark", "Gebackene Eier", "Flüsterndes Omelett"
                ),
                List.of(
                        "Horrifying Mush", "Fungal Omelette", "Proto-Omelette", "Parasitic Omelette", "Omelette",
                        "Scrambled Marrow", "Baked Eggs", "Whispering Omelette"
                ));

        zip(terms,
                List.of("Wurzelsaft", "Pilgerelixier", "Feuerwasser", "Erfrischungsgetränk", "Salzsaft"),
                List.of("Root Juice", "Pilgrim's Tonic", "Fire Water", "Refreshing Drink", "Salty Juice"));

        zip(terms,
                List.of("Wohlschmeckende Soße", "Heiße Soße", "Halbflüssiger Käse", "Cremige Soße"),
                List.of("Flavoursome Sauce", "Scorching Sauce", "Partially-Liquid Cheese", "Creamy Sauce"));

        zip(terms,
                List.of(
                        "Geheimnisvoller Fleischeintopf", "Fasriger Eintopf", "Geschmorte Organe", "Kristalline Suppe",
                        "Viel gerührter Eintopf", "Gallertartige Pampe", "Beschmutzte Suppe", "Zäher „Knödel“-Eintopf",
                        "Abgründiger Eintopf", "Würzige Organüberraschung", "Würziger Gemüseeintopf",
                        "Käse-Fleisch-Eintopf", "Rahmorgansuppe", "Gemüsecremesuppe", "Dicker Fleischeintopf",
                        "Gefüllte Organe", "Scharfer Gemüseeintopf", "Pikante Fleischbällchen",
                        "Wohlschmeckende Organe", "Köstlicher Gemüseeintopf", "Fleisch mit Gewürzkruste"
                ),
                List.of(
                        "Mystery Meat Stew", "Fibrous Stew", "Stewed Organs", "Crystalline Soup",
                        "Well-Stirred Stew", "Gelatinous Goop", "Soiled Soup", "Chewy 'Dumpling' Stew",
                        "Abyssal Stew", "Tangy Organ Surprise", "Tangy Vegetable Stew", "Cheese-and-Flesh Stew",
                        "Creamed Organ Soup", "Cream of Vegetable Soup", "Thick Meat Stew", "Devilled Organs",
                        "Fiery Vegetable Stew", "Spicy Fleshballs", "Flavoursome Organs",
                        "Delicious Vegetable Stew", "Herb-Encrusted Flesh"
                ));

        zip(terms,
                List.of(
                        "Anormale Marmelade", "Immerbrennende Marmelade", "Grahme-lade", "Kaktusgelee",
                        "Fellknäuelgelee", "Schlängelnde Marmelade"
                ),
                List.of(
                        "Anomalous Jam", "Ever-burning Jam", "Grahj'am", "Cactus Jelly", "Furball Jelly",
                        "Wriggling Jam"
                ));

        zip(terms,
                List.of("Klobiger Donut", "Proto-Krapfen"),
                List.of("Lumpen Doughnut", "Proto-Beignet"));

        zip(terms,
                List.of(
                        "Puddingdonut", "Salziger Donut", "Monströser Donut", "Der Stellarator", "Honigdonut",
                        "Honigbutterdonut", "Klebriger Proto-Donut", "Karamelldonut", "Kakaodonut",
                        "Proteinreicher Donut", "Marmeladendonut", "Schlängelnder Donut", "Anormaler Donut"
                ),
                List.of(
                        "Custard Doughnut", "Salty Doughnut", "Monstrous Doughnut", "The Stellarator",
                        "Honey Doughnut", "Honeybutter Doughnut", "Gooey ProtoDoughnut", "Caramel Doughnut",
                        "Cocoa Doughnut", "Proteinous Doughnut", "Jam Doughnut", "Wriggling Doughnut",
                        "Anomalous Doughnut"
                ));

        zip(terms,
                List.of(
                        "Tortenboden", "Geheimnisvolle Fleischpastete", "Geräucherte Fleischpastete",
                        "Ballaststoffreiche Pastete", "Fischpastete", "Zähe Organpastete", "Proto-Wurstpastete",
                        "„Beine im Teigmantel“", "Leuchtpastete", "Gestampfte Wurzelpastete", "Fester Fettkuchen",
                        "Käse-Gemüsepastete", "Knorpelpastete", "Ledrige Torte", "Die Pastete des Wissens",
                        "Grobe Fleischpastete", "Erdige Pastete", "Heimgesuchte Pastete", "Die Brüter-Torte",
                        "Der Zahnbrecher", "Fruchtpudding", "Pilztorte", "Marmeladentorte", "Anomale Torte",
                        "Stachelige Torte", "Honigkuchen", "Felltorte in Aspik", "Schlängelnde Torte",
                        "Kakaotorte", "Karamelltorte", "Puddingtorte (Kuchen)", "Sternenpuddingtorte",
                        "Gebackene Käsetorte", "Sahnehäppchen"
                ),
                List.of(
                        "Pie Case", "Mystery Meat Pie", "Smokey Meat Pie", "High-Fibre Pie", "Fish Pie",
                        "Chewy Organ Pie", "Proto-Sausage Pie", "'Legs-in-Pastry'", "Glowing Pie",
                        "Mushed Root Pie", "Solidified Grease Pie", "Cheesy Vegetable Pie", "Gristle Pie",
                        "Leathery Tart", "The Pie Of Knowledge", "Gritty Meat Pie", "Earthy Pie",
                        "Haunted Pie", "The Spawning Tart", "The Toothbreaker", "Fruity Pudding", "Fungal Tart",
                        "Jam Tart", "Anomalous Tart", "Spikey Tart", "Honey Tart", "Jellied Fur Tart",
                        "Wriggling Tart", "Cocoa Tart", "Caramel Tart", "Custard Tart", "Stellar Custard Tart",
                        "Baked Cheese Tart", "Creamy Treat"
                ));

        zip(terms,
                List.of(
                        "Feurige Marmeladenüberraschung", "Sahnebrötchen", "Speiseröhrenüberraschung",
                        "Puddingtorte", "Salzige Köstlichkeit", "Interstellare Torte", "Sahnekuriosität",
                        "Schokoladenkuriosität", "Karamellkuriosität", "„Apfel“-Kuriosität",
                        "Stachelige Kuriosität", "Marmeladenkuriosität", "Alarmierende Torte",
                        "Unlösbarer Marmeladensturz", "Puddingkuriosität", "Salzige Überraschung",
                        "Interstellare Kuriosität", "Schokotraum", "Funkelnder Honigkuchen",
                        "Fragwürdig süßer Kuchen", "Schokoladenkuchen", "Kuchen mit Karamellkruste",
                        "Gewürzter „Apfel“-Kuchen", "Traditionskuchen", "Ewig siedender Kuchen",
                        "Ewiger Kuchen", "Honig-Engelkuchen", "Superleichter Sahnekuchen",
                        "Fluffige Karamellfreude", "Engelsobstkuchen", "Sanfte Stachelüberraschung",
                        "Marmeladenbiskuit", "Feuriges Marmeladenbiskuit", "Ewiges Marmeladenbiskuit",
                        "Softe Puddingtorte", "Monströser Honigkuchen", "Proto-Kuchen mit Honig",
                        "Schreckliche, klebrige Köstlichkeit", "Heimgesuchte Schokoträume",
                        "Entfesseltes Sahnehorn", "Flüchtige Schokotorte", "Fluffiger Rachenspalter",
                        "Windendes Marmeladenküchlein", "Klebriger Brüller", "Der merkwürdigste Kuchen",
                        "Üppige Honigtorte", "Süße Sahneträume", "Klebriger Schokoladenkuchen",
                        "Klebriger Karamellkuchen", "Klebrige Fruchtüberraschung", "Rachenkleber mit Honig",
                        "Marmeladenschleimer", "Klebriges Mundfeuer", "Ewiger Honigkuchen",
                        "Klebrige Puddingtorte", "Honigkuchen mit Salz", "Sterngeburt-Köstlichkeit",
                        "Klebriges Honigküchlein", "Sahnekuchen des Untergangs", "Heulender Karamellkuchen",
                        "„Apfel“-Kuchen verlorener Seelen", "Würgendes Kuchenmonster",
                        "Entsetzlicher Marmeladenschwamm", "Brandkuchen des Grauens", "Glaskuchen",
                        "Gequälter Honigkuchen", "Kriechendes Juckhonigbiskuit", "Kuchen der Sünde",
                        "Kuchen der Verlorenen", "Karamellisierter Albtraum", "Entfesselte Monstrosität"
                ),
                List.of(
                        "Burning Jam Surprise", "Cream Buns", "Esophageal Surprise", "Custard Fancy",
                        "Briney Delight", "Interstellar Fancy", "Cream Curiosity", "Chocolate Curiosity",
                        "Caramel Curiosity", "'Apple' Curiosity", "Prickly Curiosity", "Jam Curiosity",
                        "Startling Fancy", "Unsolvable Jam Turnover", "Custard Curiosity", "Salty Surprise",
                        "Interstellar Curiosity", "Chocolate Dream", "Glittering Honey Cake",
                        "Questionably Sweet Cake", "Chocolate Cake", "Caramel-Encrusted Cake",
                        "Spiced 'Apple' Cake", "Traditional Cake", "Ever-Boiling Cake", "Perpetual Cake",
                        "Honied Angel Cake", "Extra-Fluffy Cream Cake", "Fluffy Caramel Delight",
                        "Angelic Fruitcake", "Soft and Spiky Surprise", "Jam Fluffer", "Burning Jam Fluffer",
                        "Perpetual Jam Fluffer", "Soft Custard Fancy", "Monstrous Honey Cake",
                        "Honied Proto-Cake", "Horrifying, Gooey Delight", "Haunted Chocolate Dreams",
                        "Unbound Cream Horn", "Volatile Chocolate Fancy", "Fluffy Throatripper",
                        "Writhing Jam Puff", "Gooey Screamer", "Most Curious Cake", "Honey-Soaked Fancy",
                        "Sweet Cream Dreams", "Gooey Chocolate Cake", "Gooey Caramel Cake",
                        "Gooey Fruit Surprise", "Honied Throat-Sticker", "Jam Oozers", "Gooey Mouthburner",
                        "Perpetual Honeycake", "Gooey Custard Fancy", "Salt-Laced Honey Cake",
                        "Starbirth Delight", "Gooey Honey Puff", "Doomed Cream Cake",
                        "Wailing Caramel Cake", "'Apple' Cake of Lost Souls", "Choking Monstrosity Cake",
                        "Appalling Jam Sponge", "Cake of Burning Dread", "Cake of Glass",
                        "Tortured Honey Cake", "Itching, Creeping Honey Sponge", "Cake of Sin",
                        "Cake of the Lost", "Caramelised Nightmare", "Unbound Monstrosity"
                ));

        return Map.copyOf(terms);
    }

    static Map<String, String> categories() {
        Map<String, String> categories = new LinkedHashMap<>();
        term(categories, "Köder", "Baits");
        term(categories, "Nährstoffe", "Nutrients");
        term(categories, "Essbare Produkte", "Edible Products");
        term(categories, "Butter", "Dairy");
        term(categories, "Knusperkaramel", "Caramel");
        term(categories, "Eiscreme", "Ice Cream");
        term(categories, "Teig", "Batter");
        term(categories, "Eier", "Egg Dishes");
        term(categories, "Getränke", "Drinks");
        term(categories, "Saucen", "Sauces");
        term(categories, "Suppen", "Stews and Soups");
        term(categories, "Marmeladen", "Jams");
        term(categories, "Donuts", "Doughnuts");
        term(categories, "Erweiterte Donuts", "Advanced Doughnuts");
        term(categories, "Kuchen", "Pies and Tarts");
        term(categories, "Erweiterte Kuchen", "Advanced Cakes");
        return Map.copyOf(categories);
    }

    private static void term(Map<String, String> map, String german, String english) {
        map.put(german, english);
    }

    private static void zip(Map<String, String> map, List<String> german, List<String> english) {
        if (german.size() != english.size()) {
            throw new IllegalArgumentException("Mismatched translation list sizes: " + german.size() + " vs " + english.size());
        }

        for (int index = 0; index < german.size(); index++) {
            term(map, german.get(index), english.get(index));
        }
    }
}
