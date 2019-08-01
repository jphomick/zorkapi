package com.joseph.zorkapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

@Controller
public class ZorkController {

    @Autowired
    RoomRepository roomRepository;

    @Autowired
    PassageRepository passageRepository;

    @Autowired
    ThingRepository thingRepository;

    @Autowired
    ActiveRepository activeRepository;

    @Autowired
    PersonRepository personRepository;

    @Autowired
    AmplifyRepository amplifyRepository;

    @Autowired
    DropRepository dropRepository;

    private Random r = new Random();

    // For auto room naming
    private int curr = 1;

    private ArrayList<String> roomNames = new ArrayList<>();

    private ZorkInfo info;

    private Active getOne(ArrayList<Active> list) {
        Active active = null;
        if (list != null && list.size() > 0) {
            active = list.get(0);
        }
        return active;
    }

    @RequestMapping("/{id}_act_{command}_{target}")
    public @ResponseBody String command(@PathVariable("id") Long playerId, @PathVariable("command") String command, @PathVariable("target") String target) {
        command = command.replace("-", " ");
        String temp = "";
        for (String word : target.split("-")) {
            temp += StringUtils.capitalize(word) + " ";
        }
        target = temp.trim();
        Person player = personRepository.findById(playerId).get();
        Room room = roomRepository.findById(player.getRoomId()).get();
        Thing thing = thingRepository.findByName(target);
        Active equip = activeRepository.findByValue(-100);
        Active armor = activeRepository.findByValue(-200);
        if (thing == null) {
            return "Target not found.";
        }
        Active active = getOne(activeRepository.findAllByRoomIdAndThingId(room.getId(), thing.getId()));
        if (active == null) {
            active = getOne(activeRepository.findAllByInvIdAndThingId(player.getId(), thing.getId()));
        }
        String msg = "Unknown Command";
        if (active != null && thing.getActions().contains(command)) {
            if (command.toLowerCase().contains("take")) {
                if (thing.getType().equals("money")) {
                    player.setMoney(player.getMoney() + active.getValue());
                    msg = "You take the " + thing.getName() + ".\n"
                            + thing.getName() + " is worth $" + active.getValue() + "!";
                    active.setRoomId(-1);
                } else {
                    active.setInvId(player.getId());
                    active.setRoomId(-1);

                    msg = thing.getName() + " has been added to your inventory!";
                }
            }
            if (command.toLowerCase().contains("open")) {
                String object = activeThing(active).getType();
                Active key = getOne(activeRepository.findAllByInvIdAndThingId(player.getId(),
                        thingRepository.findByName(thing.getCode()).getId()));
                if (key == null) {
                    msg = "You need a certain key to open this " + object + ".";
                } else {
                    if (active.getStatus().equals("")) {
                        player.setMoney(player.getMoney() + active.getValue());
                        msg = "You open the " + thing.getName() + ".\n"
                                + "$" + active.getValue() + " is inside!";
                    } else if (active.getStatus().contains("none")) {
                        msg = "You opened the " + thing.getName() + "!";
                    } else {
                        if (active.getStatus().contains("win")) {
                            reset();
                            return "You won the game!\nFinal Score: " +
                                    ((int)player.getMoney() + (int)player.getHealth());
                        }
                        Thing insideStats = thingRepository.findByName(active.getStatus());
                        Active inside = newObject(insideStats, room);
                        inside.setRoomId(-1);
                        inside.setInvId(player.getId());
                        activeRepository.save(inside);
                        msg = "You found a " + insideStats.getName() + " inside the " + object + "!";
                    }
                    hide(active);
                    hide(key);
                    activeRepository.save(key);
                }
            }
            if (command.toLowerCase().contains("use")) {
                if (thing.getType().equals("potion")) {
                    hide(active);
                    int heal = r.nextInt(1 + thing.getValue2() - thing.getValue()) + thing.getValue();
                    player.setHealth(player.getHealth() + heal);
                    if (player.getHealth() > 100) {
                        player.setHealth(100);
                    }
                    msg = "You used the " + thing.getName() + " and restored " +
                            heal + " health!\nCurrent health: " + player.getHealth() + "/100";
                } else if (thing.getType().equals("throw")) {
                    hide(active);
                    msg = "You threw the " + thing.getName() + "!";
                    ArrayList<Active> enemies = activeRepository.findAllByRoomId(player.getRoomId());
                    for (Active enemy : enemies) {
                        if (activeThing(enemy).getType().equals("enemy")) {
                            msg += applyStatus(enemy, active);
                            int damage = r.nextInt(1 + thing.getValue2() - thing.getValue()) + thing.getValue();
                            if (damage > 0) {
                                enemy.setConquer(enemy.getConquer() - damage);
                                msg += activeThing(enemy).getName() + " took " + damage + " damage!\n";
                                msg += defeatEnemy(enemy, room);
                                activeRepository.save(enemy);
                            }
                        }
                    }
                } else if (thing.getType().equals("brew")) {
                    Active potion = getOne(activeRepository.findAllByInvIdAndThingId(player.getId(),
                            thingRepository.findByName("Potion").getId()));
                    if (potion != null) {
                        hide(potion);
                        int heal = r.nextInt(1 + activeThing(potion).getValue2() -
                                activeThing(potion).getValue()) + activeThing(potion).getValue();
                        heal *= 2;
                        player.setHealth(player.getHealth() + heal);
                        if (player.getHealth() > 100) {
                            player.setHealth(100);
                        }
                        msg = "You used the " + thing.getName() + " with a " + activeThing(potion).getName()
                                + " and restored " + heal + " health!\nCurrent health: " + player.getHealth() + "/100";
                    } else {
                        msg = "You have no potions to use in the " + thing.getName() + "!";
                    }
                } else if (active.getStatus().contains("fire")) {
                    if (equip != null && equip.getStatus().contains("flammable")) {
                        if (equip.getStatus().contains("fire")) {
                            msg = "Your " + activeThing(equip).getName() + " is already lit!";
                        } else {
                            equip.setStatus(("fire " + equip.getStatus()).trim());
                            msg = "You lit the " + activeThing(equip).getName() + "!";
                            activeRepository.save(equip);
                        }
                    } else if (equip != null) {
                        msg = "You cannot light " + activeThing(equip).getName() + " on fire!";
                    } else {
                        msg = "You have nothing equipped!";
                    }
                }
            }
            if (command.toLowerCase().contains("equip")) {
                if (activeThing(active).getType().contains("weapon")) {
                    msg = removeEquip(equip);
                    active.setValue(-100);
                    active.setInvId(player.getId());
                    active.setRoomId(-1);
                } else {
                    msg = removeEquip(armor);
                    active.setValue(-200);
                    active.setInvId(player.getId());
                    active.setRoomId(-1);
                }
                msg += "You equipped the " + thing.getName() + "!";
            }
            if (command.toLowerCase().contains("attack")) {
                if (equip == null) {
                    return "You attacked the " + thing.getName() + "!\n...0 damage!";
                }
                Thing weaponStats = thingRepository.findById(equip.getThingId()).get();
                int base = r.nextInt(1 + weaponStats.getValue2() - weaponStats.getValue())
                        + weaponStats.getValue();
                base += amplify(equip);
                if (equip.getStatus().contains("electric") && active.getStatus().contains("wet")) {
                    base += r.nextInt(2) + 6;
                }
                if (thing.getCode().equals("~")) {
                    active.setConquer(active.getConquer() - base);
                    msg = "You attacked the " + thing.getName() + "!\n" + base + " damage!\n";
                } else {
                    String code = thing.getCode();
                    for (String item : code.split("-*[ ()+*/]+")) {
                        if (item.length() > 0 && !NumberHelper.isNumber(item)) {
                            if (item.contains("value")) {
                                code = code.replace("value", String.valueOf(base));
                            } else if (equip.getStatus().contains(item)) {
                                code = code.replace(item, "1");
                            } else {
                                code = code.replace(item, "0");
                            }
                        }
                    }
                    int damage = (int)MathHelper.eval(code);
                    active.setConquer(active.getConquer() - damage);
                    msg = "You attacked the " + thing.getName() + "!\n" + damage + " damage!\n";
                }
                if (active.getConquer() > 0) {
                    msg += applyStatus(active, equip);
                } else {
                    msg += defeatEnemy(active, room);
                }
            }
        } else if (!thing.getActions().contains(command)) {
            msg = "The command [" + command + "] cannot be used on target [" + target + "]";
        }
        if (active == null) {
            msg = thing.getName() + " does not exist in this room!";
        } else {
            activeRepository.save(active);
        }
        msg = enemyAttack(msg, playerId);
        personRepository.save(player);
        return msg.trim();
    }

    private String applyStatus(Active target, Active source) {
        String msg = "";
        if (target.getStatus().contains("flammable") && !target.getStatus().contains("wet")
                && source.getStatus().contains("fire")) {
            target.setStatus(target.getStatus() + " fire");
            msg += activeThing(target).getName() + " has been set on fire!\n";
        } else if (target.getStatus().contains("fire") && source.getStatus().contains("water")) {
            target.setStatus(target.getStatus().replace("fire", "").trim());
            msg += activeThing(target).getName() + "'s fire has been put out!\n";
        }
        if (source.getStatus().contains("water")) {
            target.setStatus(target.getStatus() + " wet");
            msg += activeThing(target).getName() + " is now wet!\n";
        }
        activeRepository.save(target);
        return msg;
    }

    private String defeatEnemy(Active active, Room room) {
        String msg = "";
        if (active.getConquer() <= 0) {
            msg += "You defeated the " + activeThing(active).getName() + "!\n";
            hide(active);
            ArrayList<Thing> drops = getDrops(active);
            for (Thing drop : drops) {
                msg += "A " + drop.getName() + " has dropped!\n";
                newObject(drop, room);
            }
            activeRepository.save(active);
        }
        return msg;
    }

    private String removeEquip(Active equip) {
        String msg;
        msg = "";
        if (equip != null) {
            if (equip.getStatus().contains("fire")) {
                equip.setStatus(equip.getStatus().replace("fire", ""));
                msg += "You put out the fire on your " + activeThing(equip).getName() + ".\n";
            }
            equip.setValue(0);
            activeRepository.save(equip);
        }
        return msg;
    }

    private String enemyAttack(String msg, Long playerId) {
        msg = msg.trim();
        Person player = personRepository.findById(playerId).get();
        ArrayList<Active> all = activeRepository.findAllByRoomId(player.getRoomId());
        ArrayList<Active> enemies = new ArrayList<>();
        Active equip = activeRepository.findByValue(-100);
        Active armor = activeRepository.findByValue(-200);
        for (Active active : all) {
            if (thingRepository.findById(active.getThingId()).get().getType().equals("enemy")) {
                enemies.add(active);
            }
        }
        if (enemies.size() > 0) {
            msg += "\n---\nEnemies attack!\n";
            for (Active active : enemies) {
                Thing enemyStats = thingRepository.findById(active.getThingId()).get();
                int damage = r.nextInt(1 + enemyStats.getValue2() - enemyStats.getValue()) + enemyStats.getValue();
                if (armor != null) {
                    Thing armorStats = thingRepository.findById(armor.getThingId()).get();
                    damage -= r.nextInt(1 + armorStats.getValue2() - armorStats.getValue())
                            + armorStats.getValue();
                    if (damage < 0) {
                        damage = 0;
                    }
                }
                player.setHealth(player.getHealth() - damage);
                msg += enemyStats.getName() + " attacks you for " + damage + " damage!\n";
                if (active.getStatus().contains("wet") && equip != null && equip.getStatus().contains("fire")) {
                    msg += "The flame on your weapon was put out!\n";
                    equip.setStatus(equip.getStatus().replace("fire", "").trim());
                    activeRepository.save(equip);
                }
            }
            personRepository.save(player);
        }
        if (player.getHealth() <= 0) {
            reset();
            msg += "You're out of health!\nGame Over!";
        }
        return msg.trim();
    }

    @RequestMapping("/help")
    public @ResponseBody String help() {
        return "Welcome to Zork!\nCan you defeat the Skeleton King and unlock the Puzzle Chest?\n"
                + "Commands:\n[check] [move] [take] [use] [equip] [attack] [open] [status]\n"
                + "All commands except [status] require a target, e.g. \"attack bat\"\n"
                + "For items two words or more, connect the words with -, e.g. \"take brass-key\"\n\n"
                + "Essential tips:\n"
                + "\"check move\" will check the directions you can move\n"
                + "\"check room\" will check the things in the room\n"
                + "You must equip a weapon to deal damage!";
    }

    @RequestMapping("/{id}_status")
    public @ResponseBody String status(@PathVariable("id") Long playerId) {
        Person player = personRepository.findById(playerId).get();
        String result = player.getName() + "\nHealth: " + player.getHealth() + "/100\nMoney: $" + player.getMoney()
                + "\nItems in your inventory:\n";
        ArrayList<Long> found = new ArrayList<>();
        for (Active active : activeRepository.findAllByInvId(player.getId())) {
            if (!found.contains(active.getThingId())) {
                int size = activeRepository.findAllByInvIdAndThingId(player.getId(), active.getThingId()).size();
                String mult = "";
                if (size > 1) {
                    mult = " x" + size;
                }
                Active equip = activeRepository.findByValue(-100);
                if (equip != null && equip.getThingId() == active.getThingId()) {
                    mult += " (equipped)";
                }
                Active armor = activeRepository.findByValue(-200);
                if (armor != null && armor.getThingId() == active.getThingId()) {
                    mult += " (equipped)";
                }
                result += thingRepository.findById(active.getThingId()).get().getName() + mult + "\n";
                found.add(active.getThingId());
            }
        }
        return result;
    }

    @RequestMapping("/{id}_check_room")
    public @ResponseBody String roomCheck(@PathVariable("id") Long playerId) {
        return enemyAttack(seeRoom(playerId), playerId);
    }

    private int getX(Person person) {
        Room room  = roomRepository.findById(person.getRoomId()).get();
        return (int) room.getX();
    }

    private int getY(Person person) {
        Room room  = roomRepository.findById(person.getRoomId()).get();
        return (int) room.getY();
    }

    @RequestMapping("/{id}_check_move")
    public @ResponseBody String moveCheck(@PathVariable("id") Long playerId) {
        return moveCheck(true, playerId);
    }

    private String moveCheck(boolean attack, Long playerId) {
        Person player = personRepository.findById(playerId).get();
        String result = "";
        if (checkMove(getX(player), getY(player) - 1, playerId)) {
            Room currRoom = roomRepository.findByXAndY(getX(player), getY(player) - 1);
            result += currRoom.getName() +
                    " is to the north. " + getBlocked(getX(player), getY(player) - 1, playerId) + "\n";
        }
        if (checkMove(getX(player), getY(player) + 1, playerId)) {
            Room currRoom = roomRepository.findByXAndY(getX(player), getY(player) + 1);
            result += currRoom.getName() +
                    " is to the south. " + getBlocked(getX(player), getY(player) + 1, playerId) + "\n";
        }
        if (checkMove(getX(player) + 1, getY(player), playerId)) {
            Room currRoom = roomRepository.findByXAndY(getX(player) + 1, getY(player));
            result += currRoom.getName() +
                    " is to the east. " + getBlocked(getX(player) + 1, getY(player), playerId) + "\n";
        }
        if (checkMove(getX(player) - 1, getY(player), playerId)) {
            Room currRoom = roomRepository.findByXAndY(getX(player) - 1, getY(player));
            result += currRoom.getName() +
                    " is to the west. " + getBlocked(getX(player) - 1, getY(player), playerId) + "\n";
        }

        if (result.equals("")) {
            result = "You have nowhere to move.";
        }
        if (attack) {
            return enemyAttack(result, playerId);
        } else {
            return result;
        }
    }

    private String seeRoom(Long playerId) {
        Person player = personRepository.findById(playerId).get();
        Room room = roomRepository.findByXAndY(getX(player), getY(player));
        String result = "Things in the " + room.getName() + ":\n";
        ArrayList<Long> found = new ArrayList<>();
        for (Active active : activeRepository.findAllByRoomId(room.getId())) {
            if (!found.contains(active.getThingId())) {
                int size = activeRepository.findAllByRoomIdAndThingId(player.getRoomId(), active.getThingId()).size();
                String mult = "";
                if (size > 1) {
                    mult = " x" + size;
                }
                result += thingRepository.findById(active.getThingId()).get().getName() + mult + "\n";
                found.add(active.getThingId());
            }
        }
        if (inInventory("Compass", playerId)) {
            result += "---\n" + moveCheck(false, playerId);
        }
        return result;
    }

    @RequestMapping("/play_{name}")
    public @ResponseBody String newFile(@PathVariable("name") String name) {
        Person player = new Person(name);
        player.setRoomId(roomRepository.findByName("Foyer").getId());
        personRepository.save(player);
        return String.valueOf(player.getId());
    }

    @RequestMapping("/load_{id}")
    public @ResponseBody String loadFile(@PathVariable("id") Long id) {
        String name = personRepository.findById(id).get().getName();
        return "Welcome back, " + name + "!";
    }

    @RequestMapping("/{id}_move_north")
    public @ResponseBody String moveUp(@PathVariable("id") Long playerId) {
        Person player = personRepository.findById(playerId).get();
        String blocked = getBlocked(getX(player), getY(player) - 1, playerId);
        if (!blocked.equals("")) {
            return blocked;
        } else if (checkMove(getX(player), getY(player) - 1, playerId)) {
            Room currRoom = roomRepository.findByXAndY(getX(player), getY(player) - 1);
            currRoom.setVisited(true);
            roomRepository.save(currRoom);
            player.setRoomId(currRoom.getId());
            personRepository.save(player);
            return "You moved to " + currRoom.getName() + "!" + "\n" + seeRoom(playerId);
        }

        return enemyAttack("You cannot perform that action", playerId);
    }

    @RequestMapping("/{id}_move_south")
    public @ResponseBody String moveDown(@PathVariable("id") Long playerId) {
        Person player = personRepository.findById(playerId).get();
        String blocked = getBlocked(getX(player), getY(player) + 1, playerId);
        if (!blocked.equals("")) {
            return blocked;
        } else if (checkMove(getX(player), getY(player) + 1, playerId)) {
            Room currRoom = roomRepository.findByXAndY(getX(player), getY(player) + 1);
            currRoom.setVisited(true);
            roomRepository.save(currRoom);
            player.setRoomId(currRoom.getId());
            personRepository.save(player);
            return "You moved to " + currRoom.getName() + "!" + "\n" + seeRoom(playerId);
        }

        return enemyAttack("You cannot perform that action", playerId);
    }

    @RequestMapping("/{id}_move_east")
    public @ResponseBody String moveRight(@PathVariable("id") Long playerId) {
        Person player = personRepository.findById(playerId).get();
        String blocked = getBlocked(getX(player) + 1, getY(player), playerId);
        if (!blocked.equals("")) {
            return blocked;
        } else if (checkMove(getX(player) + 1, getY(player), playerId)) {
            Room currRoom = roomRepository.findByXAndY(getX(player) + 1, getY(player));
            currRoom.setVisited(true);
            roomRepository.save(currRoom);
            player.setRoomId(currRoom.getId());
            personRepository.save(player);
            return "You moved to " + currRoom.getName() + "!" + "\n" + seeRoom(playerId);
        }

        return enemyAttack("You cannot perform that action", playerId);
    }

    @RequestMapping("/{id}_move_west")
    public @ResponseBody String moveLeft(@PathVariable("id") Long playerId) {
        Person player = personRepository.findById(playerId).get();
        String blocked = getBlocked(getX(player) - 1, getY(player), playerId);
        if (!blocked.equals("")) {
            return blocked;
        } else if (checkMove(getX(player) - 1, getY(player), playerId)) {
            Room currRoom = roomRepository.findByXAndY(getX(player) - 1, getY(player));
            currRoom.setVisited(true);
            roomRepository.save(currRoom);
            player.setRoomId(currRoom.getId());
            personRepository.save(player);
            return "You moved to " + currRoom.getName() + "!" + "\n" + seeRoom(playerId);
        }

        return enemyAttack("You cannot perform that action", playerId);
    }

    private boolean checkMove(int moveX, int moveY, Long playerId) {
        Person player = personRepository.findById(playerId).get();
        Room currRoom = roomRepository.findByXAndY(getX(player), getY(player));
        Room toRoom = roomRepository.findByXAndY(moveX, moveY);

        if (currRoom != null && toRoom != null) {
            Passage pass = passageRepository.findByRoomFromAndRoomTo(currRoom.getId(), toRoom.getId());
            if (pass == null) {
                pass = passageRepository.findByRoomFromAndRoomTo(toRoom.getId(), currRoom.getId());
                //if (pass != null && !pass.isReversable()) {
                //    pass = null;
                //}
            }
            if (pass != null) {
                return true;
            }
        }
        return false;
    }

    private String getBlocked(int moveX, int moveY, Long playerId) {
        Person player = personRepository.findById(playerId).get();
        Room currRoom = roomRepository.findByXAndY(getX(player), getY(player));
        Room toRoom = roomRepository.findByXAndY(moveX, moveY);
        String msg = "";

        if (currRoom != null && toRoom != null) {
            Passage pass = passageRepository.findByRoomFromAndRoomTo(currRoom.getId(), toRoom.getId());
            if (pass == null) {
                pass = passageRepository.findByRoomFromAndRoomTo(toRoom.getId(), currRoom.getId());
                //if (pass != null && !pass.isReversable()) {
                //    pass = null;
                //}
            }
            if (pass != null) {
                ArrayList<Active> blocks = activeRepository.findAllByBlockId(pass.getId());
                if (blocks.size() > 0) {
                    msg = "Blocked by ";
                }
                for (Active block : blocks) {
                    if (block.getRoomId() == toRoom.getId()) {
                        msg += "??? ";
                    } else {
                        msg += activeThing(block).getName() + " ";
                    }
                }
            }
        }
        return msg.trim();
    }

    @RequestMapping("/reset")
    public @ResponseBody String reset()
    {
        roomRepository.deleteAll();
        passageRepository.deleteAll();
        activeRepository.deleteAll();
        personRepository.deleteAll();
        info = new ZorkInfo();

        int size = 50;
        int iterations = 10;

        int startX = size / 2;
        int startY = size / 2;


        roomNames = new ArrayList<>();
        // Unique room names
        roomNames.addAll(Arrays.asList(ZorkInfo.ROOM_LIST));

        Room first = new Room("Foyer", startX, startY);
        first.setVisited(true);

        roomRepository.save(first);

        activeRepository.save(newObject(thingRepository.findByName("Stick"), first));
        activeRepository.save(newObject(thingRepository.findByName("Puzzle Chest"), first));
        //activeRepository.save(newObject(thingRepository.findByName("Torch"), first));
        //activeRepository.save(newObject(thingRepository.findByName("Door Key"), first));
        //Active vines = newObject(thingRepository.findByName("Door Lock"), first);
        //activeRepository.save(vines);

        curr = 1;

        while (newRoom(100, startX, startY, size, -1)) {
        }

        if (info.addMore("Skeleton King")) {
            activeRepository.save(newObject(
                    info.add(thingRepository.findByName("Skeleton King")),
                    roomRepository.findByName("Throne Room")));
        }
        //setBlock(thingRepository.findByName("Door Lock"), vines);

        StringBuilder map = new StringBuilder();
        for (int i = 0; i < startX * 2; i++) {
            for (int j = 0; j < startY * 2; j++) {
                if (roomRepository.findByXAndY(i, j) != null) {
                    map.append('X');
                } else {
                    map.append('-');
                }
            }
            map.append("\t\n");
        }

        return map.toString();
    }

    private boolean newRoom(int left, int x, int y, int max, int last) {
        if (left > 0 && x > 0 && y > 0 && x < max && y < max) {
            Random r = new Random();
            int next = r.nextInt(4);
            if (next == last) {
                next++;
                if (next > 3) {
                    next = 0;
                }
            }
            int prevX = x;
            int prevY = y;
            if (next == 0) {
                y--;
            } else if (next == 1) {
                x--;
            } else if (next == 2) {
                y++;
            } else {
                x++;
            }
            Room roomFrom = roomRepository.findByXAndY(prevX, prevY);
            Room roomTo = roomRepository.findByXAndY(x, y);
            boolean added = false;
            if (roomTo == null) {
                String name = "Room " + curr;
                if (roomNames.size() > 0) {
                    name = roomNames.remove(r.nextInt(roomNames.size()));
                } else {
                    return false;
                }
                roomTo = new Room(name, x, y);
                roomRepository.save(roomTo);
                curr++;
                added = true;
            }
            Passage pass = passageRepository.findByRoomFromAndRoomTo(roomFrom.getId(), roomTo.getId());
            if (pass == null) {
                pass = passageRepository.findByRoomFromAndRoomTo(roomTo.getId(), roomFrom.getId());
            }
            if (pass == null) {
                pass = new Passage(roomFrom.getId(), roomTo.getId(), true);
                passageRepository.save(pass);
            }
            if (added) {
                setThings(roomTo);
            }
            left--;

            boolean more = newRoom(left, x, y, max, next);

            if (left == 0 && info.addMore("Skeleton King")) {
                activeRepository.save(newObject(
                        info.add(thingRepository.findByName("Skeleton King")), roomTo));
            }

            if (added) {
                ArrayList<Active> actives = activeRepository.findAllByRoomId(roomTo.getId());
                if (actives != null && actives.size() > 0) {
                    for (Active active : actives) {
                        setBlock(activeThing(active), active);
                    }
                }
            }

            return more;
        }
        return true;
    }

    private void setThings(Room room) {
        ArrayList<Passage> passes = passageRepository.findAllByRoomFromOrRoomToAndReversableTrue(room.getId(), room.getId());

        ArrayList<Thing> money = info.getAvailable("money", thingRepository);
        ArrayList<Thing> enemies = info.getAvailable("enemy", thingRepository);
        ArrayList<Thing> items = info.getAvailable("item", thingRepository);
        ArrayList<Thing> key = info.getAvailable("key item", thingRepository);

        if (r.nextInt(4) == 1) {
            Thing toAdd = info.add(money.get(r.nextInt(money.size())));
            activeRepository.save(newObject(toAdd, room));
        }
        int max = 3;
        while (r.nextInt(6 - max) == 1 && max > 0) {
            Thing toAdd = info.add(enemies.get(r.nextInt(enemies.size())));
            activeRepository.save(newObject(toAdd, room));
            max--;
        }
        max = 2;
        while (r.nextInt(4 - max) == 1 && max > 0) {
            Thing toAdd = info.add(items.get(r.nextInt(items.size())));
            activeRepository.save(newObject(toAdd, room));
            if (info.getPair(toAdd.getName()) != null) {
                newObject(thingRepository.findByName(info.getPair(toAdd.getName())), randomRoom());
            }
            max--;
        }
        if (r.nextInt(4) == 1) {
            Thing toAdd = info.add(key.get(0));
            activeRepository.save(newObject(toAdd, room));
            if (info.getPair(toAdd.getName()) != null) {
                newObject(thingRepository.findByName(info.getPair(toAdd.getName())), randomRoom());
            }
        }
    }

    private Active newObject(Thing toAdd, Room room) {
        Active object = new Active(toAdd.getId(), r.nextInt(1 + toAdd.getValue2() - toAdd.getValue())
                + toAdd.getValue(), toAdd.getConquer(), room.getId(), -1, -1, toAdd.getStatus());
        activeRepository.save(object);
        return object;
    }

    private void setBlock(Thing toAdd, Active object) {
        long blockId = 0;
        if (toAdd.getBlock() > 0) {
            ArrayList<Passage> passes =
                    passageRepository.findAllByRoomFrom(object.getRoomId());
            if (passes != null && passes.size() > 0) {
                blockId = passes.get(r.nextInt(passes.size())).getId();
            }
        }
        object.setBlockId(blockId);
        activeRepository.save(object);
    }

    private void hide(Active active) {
        active.setBlockId(-1);
        active.setInvId(-1);
        active.setRoomId(-1);
        activeRepository.save(active);
    }

    private void setDrop(Active active, String item, int chance, boolean unique) {
        EnemyDrop enemyDrop;
        if (unique) {
            enemyDrop = new EnemyDrop(active.getId(), item, chance);
        } else {
            enemyDrop = new EnemyDrop(activeThing(active).getName(), item, chance);
        }
        dropRepository.save(enemyDrop);
    }

    private ArrayList<Thing> getDrops(Active active) {
        ArrayList<Thing> enemyDrops = new ArrayList<>();
        ArrayList<EnemyDrop> general = dropRepository.findAllByGeneralThing(activeThing(active).getName());
        if (general != null) {
            for (EnemyDrop enemyDrop : general) {
                if (r.nextInt(100) < enemyDrop.getChance()) {
                    enemyDrops.add(thingRepository.findByName(enemyDrop.getItem()));
                }
            }
        }
        ArrayList<EnemyDrop> specific = dropRepository.findAllBySpecificActive(activeThing(active).getId());
        if (specific != null) {
            for (EnemyDrop enemyDrop : specific) {
                if (r.nextInt(100) < enemyDrop.getChance()) {
                    enemyDrops.add(thingRepository.findByName(enemyDrop.getItem()));
                }
            }
        }
        return enemyDrops;
    }

    private int amplify(Active equip) {
        int value = 0;
        for (String word : equip.getStatus().split(" ")) {
            Amplify amp = amplifyRepository.findByKeyword(word);
            if (amp != null) {
                value += amp.getValue();
            }
        }
        return value;
    }

    private boolean inInventory(String name, Long playerId) {
        Thing thing = thingRepository.findByName(name);
        Person player = personRepository.findById(playerId).get();
        if (thing != null) {
            ArrayList<Active> actives = activeRepository.findAllByInvIdAndThingId(player.getId(), thing.getId());
            if (actives != null && actives.size() > 0) {
                return true;
            }
        }
        return false;
    }

    private Thing activeThing(Active active) {
        if (active == null) {
            return null;
        } else {
            return thingRepository.findById(active.getThingId()).get();
        }
    }

    private Room randomRoom() {
        ArrayList<Room> rooms = roomRepository.findAll();
        int pick = r.nextInt(rooms.size());
        if (rooms.get(pick).getName().equals("Foyer")) {
            pick++;
            if (pick >= rooms.size()) {
                pick = 0;
            }
        }
        return rooms.get(pick);
    }

    private Room randomRoom(ArrayList<Room> rooms) {
        int pick = r.nextInt(rooms.size());
        if (rooms.get(pick).getName().equals("Foyer")) {
            pick++;
            if (pick >= rooms.size()) {
                pick = 0;
            }
        }
        return rooms.get(pick);
    }
}
