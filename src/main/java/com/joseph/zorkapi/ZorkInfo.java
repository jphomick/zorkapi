package com.joseph.zorkapi;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.TreeMap;

class ZorkInfo {

    static final String[] ROOM_LIST = new String[]{"Kitchen", "Anteroom", "Master Bedroom", "Study", "Dining Room",
            "Laundry Room", "Parlor", "Guest Bedroom", "Closet", "Storage Room", "Washroom",
            "Electric Room", "Dance Floor", "Sitting Room", "Ice Cooler", "Safari Room", "Trophy Room",
            "Gloomy Tunnel", "Arboretum", "Grand Hall", "Throne Room", "Observatory", "Workshop",
            "Grand Exhibit", "Aquarium", "Tactics Center", "Picture Gallery", "Underground Brook",
            "Stalagmites", "Hidden Pond", "Concert Hall", "Empty Room", "Artist's Studio", "Mushroom Alcove",
            "Wine Cellar", "Theater", "Tool Shed", "Trinket Room"};

    static final String[] PROGRESSION = new String[]{"Brass Key", "Metal Chest", "Oil Lamp", "Vines", "Green Slime",
            "Door Lock", "Sword"};

    static final String[] NO_DROP = new String[]{"Door Key", "Torch", "Stick", "Puzzle Chest", "Puzzle Key",
            "Puzzle Piece", "Brass Key"};

    static final String[] ENEMY_GROWTH = new String[]{"Bat", "Slime", "Crab", "Spider", "Skeleton"};

    static final String[] UNIQUE = new String[]{"Metal Chest", "Compass", "Skeleton King", "Sword", "Green Slime",
            "Golf Club"};

    static final String[][] PAIRS = new String[][]{{"Brass Key", "Wooden Chest"}, {"Brass Key", "Metal Chest"},
            {"Green Slime", "Door Lock"}};

    private ArrayList<String> added;
    private ArrayList<String> unique;
    private int progression;
    private int growth;
    private TreeMap<String, String> pairs;

    ZorkInfo() {
        pairs = new TreeMap<>();
        for (String[] set : PAIRS) {
            pairs.put(set[1], set[0]);
        }
        added = new ArrayList<>();
        unique = new ArrayList<>();
        unique.addAll(Arrays.asList(UNIQUE));

        progression = 0;
        growth = 0;
    }

    ArrayList<Thing> getAvailable(String keyword, ThingRepository thingRepository) {

        ArrayList<Thing> things = new ArrayList<>();

        if (keyword.equals("money")) {
            things.addAll(thingRepository.findAllByType("money"));
        } else if (keyword.equals("enemy")) {
            int lower = new Random().nextInt(1 + growth);
            if (lower >= ENEMY_GROWTH.length) {
                lower = ENEMY_GROWTH.length - 1;
            }
            for (int i = lower; i <= growth && i < ENEMY_GROWTH.length; i++) {
                things.add(thingRepository.findByName(ENEMY_GROWTH[i]));
            }
        } else if (keyword.equals("item")) {
            things.addAll(thingRepository.findAllByType("weapon"));
            things.addAll(thingRepository.findAllByType("potion"));
            things.addAll(thingRepository.findAllByType("key"));
            things.addAll(thingRepository.findAllByType("chest"));
            things.addAll(thingRepository.findAllByType("object"));
            things.addAll(thingRepository.findAllByType("lock"));

            for (int i = progression + 1; i < PROGRESSION.length; i++) {
                things.remove(thingRepository.findByName(PROGRESSION[i]));
            }
            for (String item : added) {
                things.remove(thingRepository.findByName(item));
            }
            for (String item : NO_DROP) {
                things.remove(thingRepository.findByName(item));
            }
        } else if (keyword.equals("key item")) {
            if (progression < PROGRESSION.length) {
                things.add(thingRepository.findByName(PROGRESSION[progression]));
            } else {
                things.add(thingRepository.findByName("Gold"));
            }
        }

        return things;
    }

    Thing add(Thing thing) {
        if (unique.contains(thing.getName())) {
            added.add(thing.getName());
        }
        if (growth < ENEMY_GROWTH.length && ENEMY_GROWTH[growth].equals(thing.getName())) {
            growth++;
        }
        if (progression < PROGRESSION.length && PROGRESSION[progression].equals(thing.getName())) {
            progression++;
        }
        return thing;
    }

    boolean addMore(Thing thing) {
        if (added.contains(thing.getName())) {
            return false;
        }
        return true;
    }

    boolean addMore(String thing) {
        if (added.contains(thing)) {
            return false;
        }
        return true;
    }

    String getPair(String initial) {
        return pairs.get(initial);
    }
}
