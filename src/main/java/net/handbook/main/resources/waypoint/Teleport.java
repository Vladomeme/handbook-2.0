package net.handbook.main.resources.waypoint;

public enum Teleport {
    Empty ("Empty", "none", 0, 0,0, true),
    //Valley safe
    Sierhaven ("Sierhaven", "valley", -765,  107, 65,   true),
    Nyr       ("Nyr",       "valley", -140,  100, -80,  true),
    Farr      ("Farr",      "valley", 570,   100, 190,  true),
    Highwatch ("Highwatch", "valley", 1170,  130, -110, true),
    Lowtide   ("Lowtide",   "valley", 710,   73,  480,  true),
    Oceangate ("Oceangate", "valley", -1634, 122, 98,   true),
    TaEldim   ("Ta'Eldim",  "valley", 470,   190, -380, true),
    //Valley unsafe
    White     ("White Dungeon Lobby",       "valley", 215,  121, -155, false),
    Orange    ("Orange Dungeon Lobby",      "valley", 48,   107, 220,  false),
    Magenta   ("Magenta Dungeon Lobby",     "valley", 427,  34,  48,   false),
    LightBlue ("Light Blue Dungeon Lobby",  "valley", 821,  121, -320, false),
    Yellow    ("Yellow Dungeon Lobby",      "valley", 1199, 71,  110,  false),
    Corridors ("Ephemeral Corridors Lobby", "valley", 840,  85,  196,  false),
    Willows   ("Black Willows Lobby",       "valley", 350,  56, -171,  false),
    Sanctum   ("Forsworn Sanctum Lobby",    "valley", 503,  40,  436,  false),
    Verdant   ("Verdant Remnants Lobby",    "valley", 1003, 123, 186,  false),
    //Isles safe
    Mistport   ("Mistport",          "isles", -760,  84,  1320, true),
    Alnera     ("Alnera",            "isles", 320,   87,  760,  true),
    Rahkeri    ("Rahkeri",           "isles", -110,  160, 470,  true),
    Molta      ("Molta",             "isles", 200,   100, 80,   true),
    Frostgate  ("Frostgate",         "isles", -1520, 97,  970,  true),
    Wispervale ("Wispervale",        "isles", -1760, 131, -15,  true),
    Nightroost ("Nightroost",        "isles", -1315, 128, 510,  true),
    Steelmeld  ("Steelmeld",         "isles", -584,  8,   -474, true),
    Carnival   ("Floating Carnival", "isles", -466,  84,  1557, true),
    Horseman   ("Horseman Arena",    "isles", -1214, 80,  -464, true),
    BlackMist  ("Black Mist Lobby",  "isles", -137,  75,  3325, true),
    //Isles unsafe
    Lime      ("Lime Dungeon Lobby",       "isles", -551 , 40,  703,  false),
    Pink      ("Pink Dungeon Lobby",       "isles", -1040, 21,  609,  false),
    Grey      ("Grey Dungeon Lobby",       "isles", 238,   70,  400,  false),
    LightGrey ("Light Grey Dungeon Lobby", "isles", -1463, 155, -24,  false),
    Cyan      ("Cyan Dungeon Lobby",       "isles", -77,   34,  197,  false),
    Purple    ("Purple Dungeon Lobby",     "isles", 596,   45,  1416, false),
    Teal      ("Teal Dungeon Lobby",       "isles", -778,  14,  -436, false),
    Shifting  ("Shifting Lobby",           "isles", -391,  7,   -532, false),
    SR        ("Sealed Remorse Lobby",     "isles", -2064, 73,  -438, false),
    //Ring safe
    PortManteau ("Port Manteau",          "ring", -458, 46,  -915, true),
    Galengarde  ("Galengarde",            "ring", -300, 83,  -655, true),
    NewAntium   ("New Antium",            "ring", -450, 180, -75,  true),
    Chantry     ("Chantry of Repentance", "ring", 0,    170, 835,  true),
    SKT         ("SKT Lobby",             "ring", -530, 31,  -790, true),
    Brown       ("Brown Dungeon Lobby",   "ring", -410, 82,  74,   true),
    Portal      ("Portal Lobby",          "ring", -369, 114, -145, true),
    Sirius      ("Sirius Arena",          "ring", 211,  32,  985, true),
    Austrus     ("Austrus Bell",          "ring", 167,  121, 1368, true),
    Occidenalus ("Occidenalus Bell",      "ring", 1,    207, 978,  true),
    Subterus    ("Subterus Bell",         "ring", 193,  45,  833,  true),
    Orentalus   ("Orentalus Bell",        "ring", 155,  206, 685,  true),
    Borealus    ("Borealus Bell",         "ring", 9,    35,  623,  true),
    //Ring unsafe
    Ruin ("Ruin Lobby",         "ring", -86,  17,  -466, false),
    Blue ("Blue Dungeon Lobby", "ring", -530, 176, -386, false);

    public final String name;
    public final String shard;
    public final int x;
    public final int y;
    public final int z;
    public final boolean isSafe;

    Teleport(String name, String shard, int x, int y, int z, boolean isSafe) {
        this.name = name;
        this.shard = shard;
        this.x = x;
        this.y = y;
        this.z = z;
        this.isSafe = isSafe;
    }
}
