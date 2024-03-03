package net.handbook.main.resources.waypoint;

public enum Teleports {
    Empty (new Teleport("Empty", "none", 0, 0,0, true)),
    //Valley safe
    Sierhaven (new Teleport("Sierhaven", "valley", -765,  107, 65,   true)),
    Nyr       (new Teleport("Nyr",       "valley", -140,  100, -80,  true)),
    Farr      (new Teleport("Farr",      "valley", 570,   100, 190,  true)),
    Highwatch (new Teleport("Highwatch", "valley", 1170,  130, -110, true)),
    Lowtide   (new Teleport("Lowtide",   "valley", 710,   73,  480,  true)),
    Oceangate (new Teleport("Oceangate", "valley", -1634, 122, 98,   true)),
    TaEldim   (new Teleport("Ta'Eldim",  "valley", 470,   190, -380, true)),
    //Valley unsafe
    White     (new Teleport("White Dungeon Lobby",       "valley", 215,  121, -155, false)),
    Orange    (new Teleport("Orange Dungeon Lobby",      "valley", 48,   107, 220,  false)),
    Magenta   (new Teleport("Magenta Dungeon Lobby",     "valley", 427,  34,  48,   false)),
    LightBlue (new Teleport("Light Blue Dungeon Lobby",  "valley", 821,  121, -320, false)),
    Yellow    (new Teleport("Yellow Dungeon Lobby",      "valley", 1199, 71,  110,  false)),
    Corridors (new Teleport("Ephemeral Corridors Lobby", "valley", 840,  85,  196,  false)),
    Willows   (new Teleport("Black Willows Lobby",       "valley", 350,  56, -171,  false)),
    Sanctum   (new Teleport("Forsworn Sanctum Lobby",    "valley", 503,  40,  436,  false)),
    Verdant   (new Teleport("Verdant Remnants Lobby",    "valley", 1003, 123, 186,  false)),
    //Isles safe
    Mistport   (new Teleport("Mistport",          "isles", -760,  84,  1320, true)),
    Alnera     (new Teleport("Alnera",            "isles", 320,   87,  760,  true)),
    Rahkeri    (new Teleport("Rahkeri",           "isles", -110,  160, 470,  true)),
    Molta      (new Teleport("Molta",             "isles", 200,   100, 80,   true)),
    Frostgate  (new Teleport("Frostgate",         "isles", -1520, 97,  970,  true)),
    Wispervale (new Teleport("Wispervale",        "isles", -1760, 131, -15,  true)),
    Nightroost (new Teleport("Nightroost",        "isles", -1315, 128, 510,  true)),
    Steelmeld  (new Teleport("Steelmeld",         "isles", -584,  8,  -474, true)),
    Carnival   (new Teleport("Floating Carnival", "isles", -466,  84,  1557, true)),
    Horseman   (new Teleport("Horseman Arena",    "isles", -1214, 80,  -464, true)),
    BlackMist  (new Teleport("Black Mist Lobby",  "isles", -137,  75,  3325, true)),
    //Isles unsafe
    Lime      (new Teleport("Lime Dungeon Lobby",       "isles", -551 , 40,  703,  false)),
    Pink      (new Teleport("Pink Dungeon Lobby",       "isles", -1040, 21,  609,  false)),
    Grey      (new Teleport("Grey Dungeon Lobby",       "isles", 238,   70,  400,  false)),
    LightGrey (new Teleport("Light Grey Dungeon Lobby", "isles", -1463, 155, -24,  false)),
    Cyan      (new Teleport("Cyan Dungeon Lobby",       "isles", -77,   34,  197,  false)),
    Purple    (new Teleport("Purple Dungeon Lobby",     "isles", 596,   45,  1416, false)),
    Teal      (new Teleport("Teal Dungeon Lobby",       "isles", -778,  14,  -436, false)),
    Shifting  (new Teleport("Shifting Lobby",           "isles", -391,  7,   -532, false)),
    SR        (new Teleport("Sealed Remorse Lobby",     "isles", -2064, 73,  -438, false)),
    //Ring safe
    PortManteau (new Teleport("Port Manteau",          "ring", -458, 46,  -915, true)),
    Galengarde  (new Teleport("Galengarde",            "ring", -300, 83,  -655, true)),
    NewAntium   (new Teleport("New Antium",            "ring", -450, 180, -75,  true)),
    Chantry     (new Teleport("Chantry of Repentance", "ring", 0,    170, 835,  true)),
    SKT         (new Teleport("SKT Lobby",             "ring", -530, 31,  -790, true)),
    Brown       (new Teleport("Brown Dungeon Lobby",   "ring", -410, 82,  74,   true)),
    Portal      (new Teleport("Portal Lobby",          "ring", -369, 114, -145, true)),
    Sirius      (new Teleport("Sirius Arena",          "ring", 211,  32,  985, true)),
    Austrus     (new Teleport("Austrus Bell",          "ring", 167,  121, 1368, true)),
    Occidenalus (new Teleport("Occidenalus Bell",      "ring", 1,    207, 978,  true)),
    Subterus    (new Teleport("Subterus Bell",         "ring", 193,  45,  833,  true)),
    Orentalus   (new Teleport("Orentalus Bell",        "ring", 155,  206, 685,  true)),
    Borealus    (new Teleport("Borealus Bell",         "ring", 9,    35,  623,  true)),
    //Ring unsafe
    Ruin (new Teleport("Ruin Lobby",         "ring", -86,  17,  -466, false)),
    Blue (new Teleport("Blue Dungeon Lobby", "ring", -530, 176, -386, false));

    private final Teleport teleport;

    Teleports(Teleport teleport) {
        this.teleport = teleport;
    }

    public Teleport get() {
        return teleport;
    }
}
