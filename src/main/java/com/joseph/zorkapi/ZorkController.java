package com.joseph.zorkapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
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

    Random r = new Random();

    // For auto room naming
    private int curr = 1;

    // Until we have roles
    private long playerId;

    private Active getOne(ArrayList<Active> list) {
        Active active = null;
        if (list != null && list.size() > 0) {
            active = list.get(0);
        }
        return active;
    }

    @RequestMapping("/act_{command}_{target}")
    public @ResponseBody String command(@PathVariable("command") String command, @PathVariable("target") String target) {
        command = command.replace("-", " ");
        target = target.replace("-", " ");
        Person player = personRepository.findById(playerId).get();
        Room room = roomRepository.findById(player.getRoomId()).get();
        Thing thing = thingRepository.findByName(target);
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
                Active key = getOne(activeRepository.findAllByInvIdAndThingId(player.getId(),
                        thingRepository.findByName(thing.getCode()).getId()));
                if (key == null) {
                    msg = "You need a certain key to open this chest.";
                } else {
                    player.setMoney(player.getMoney() + active.getValue());
                    msg = "You open the " + thing.getName() + ".\n"
                            + thing.getName() + " is worth $" + active.getValue() + "!";
                    active.setRoomId(-1);
                    key.setInvId(-1);
                    activeRepository.save(key);
                }
            }
            if (command.toLowerCase().contains("use")) {
                active.setInvId(-1);
                active.setRoomId(-1);
                int heal = r.nextInt(1 + thing.getValue2() - thing.getValue()) + thing.getValue();
                player.setHealth(player.getHealth() + heal);
                if (player.getHealth() > 100) {
                    player.setHealth(100);
                }
                msg = "You used the " + thing.getName() + " and restored" +
                        heal + " health!\nCurrent health: " + player.getHealth() + "/100";
            }
            if (command.toLowerCase().contains("equip")) {
                Active prev = activeRepository.findByValue(-100);
                if (prev != null) {
                    prev.setValue(0);
                }
                active.setValue(-100);
                active.setInvId(player.getId());
                active.setRoomId(-1);
                msg = "You equipped the " + thing.getName() + "!";
            }
            if (command.toLowerCase().contains("attack")) {
                Active weapon = activeRepository.findByValue(-100);
                if (weapon == null) {
                    return "You attack with your fists!\n...0 damage!";
                }
                Thing weaponStats = thingRepository.findById(weapon.getThingId()).get();
                int damage = r.nextInt(1 + weaponStats.getValue2() - weaponStats.getValue())
                        + weaponStats.getValue();
                active.setConquer(active.getConquer() - damage);
                msg = "You attacked the " + thing.getName() + "!\n" + damage + " damage!";
                if (active.getConquer() <= 0) {
                    msg += "\nYou defeated the " + thing.getName() + "!";
                    active.setRoomId(-1);
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
        msg = enemyAttack(msg);
        personRepository.save(player);
        return msg;
    }

    private String enemyAttack(String msg) {
        msg = msg.trim();
        Person player = personRepository.findById(playerId).get();
        ArrayList<Active> all = activeRepository.findAllByRoomId(player.getRoomId());
        ArrayList<Active> enemies = new ArrayList<>();
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
                player.setHealth(player.getHealth() - damage);
                msg += enemyStats.getName() + " attacks you for " + damage + " damage!";
            }
            personRepository.save(player);
        }
        return msg;
    }

    @RequestMapping("/status")
    public @ResponseBody String status() {
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
                result += thingRepository.findById(active.getThingId()).get().getName() + mult + "\n";
                found.add(active.getThingId());
            }
        }
        return result;
    }

    @RequestMapping("/check_room")
    public @ResponseBody String roomCheck() {
        return enemyAttack(seeRoom());
    }

    private int getX(Person person) {
        Room room  = roomRepository.findById(person.getRoomId()).get();
        return (int) room.getX();
    }

    private int getY(Person person) {
        Room room  = roomRepository.findById(person.getRoomId()).get();
        return (int) room.getY();
    }

    @RequestMapping("/check_move")
    public @ResponseBody String moveCheck() {
        Person player = personRepository.findById(playerId).get();
        String result = "";
        if (checkMove(getX(player), getY(player) - 1)) {
            Room currRoom = roomRepository.findByXAndY(getX(player), getY(player) - 1);
            result += currRoom.getName() + " is to the north.\n";
        }
        if (checkMove(getX(player) - 1, getY(player))) {
            Room currRoom = roomRepository.findByXAndY(getX(player) - 1, getY(player));
            result += currRoom.getName() + " is to the west.\n";
        }
        if (checkMove(getX(player), getY(player) + 1)) {
            Room currRoom = roomRepository.findByXAndY(getX(player), getY(player) + 1);
            result += currRoom.getName() + " is to the south.\n";
        }
        if (checkMove(getX(player) + 1, getY(player))) {
            Room currRoom = roomRepository.findByXAndY(getX(player) + 1, getY(player));
            result += currRoom.getName() + " is to the east.\n";
        }

        if (result.equals("")) {
            result = "You have nowhere to move.";
        }
        return enemyAttack(result);
    }

    private String seeRoom() {
        Person player = personRepository.findById(playerId).get();
        Room room = roomRepository.findByXAndY(getX(player), getY(player));
        String result = "Things in the room:\n";
        for (Active active : activeRepository.findAllByRoomId(room.getId())) {
            result += thingRepository.findById(active.getThingId()).get().getName() + "\n";
        }
        return result;
    }

    @RequestMapping("/move_north")
    public @ResponseBody String moveUp() {
        Person player = personRepository.findById(playerId).get();
        if (checkMove(getX(player), getY(player) - 1)) {
            Room currRoom = roomRepository.findByXAndY(getX(player), getY(player) - 1);
            player.setRoomId(currRoom.getId());
            personRepository.save(player);
            return "You moved to " + currRoom.getName() + "!" + "\n" + seeRoom();
        }

        return enemyAttack("You cannot perform that action");
    }

    @RequestMapping("/move_south")
    public @ResponseBody String moveDown() {
        Person player = personRepository.findById(playerId).get();
        if (checkMove(getX(player), getY(player) + 1)) {
            Room currRoom = roomRepository.findByXAndY(getX(player), getY(player) + 1);
            player.setRoomId(currRoom.getId());
            personRepository.save(player);
            return "You moved to " + currRoom.getName() + "!" + "\n" + seeRoom();
        }

        return enemyAttack("You cannot perform that action");
    }

    @RequestMapping("/move_west")
    public @ResponseBody String moveLeft() {
        Person player = personRepository.findById(playerId).get();
        if (checkMove(getX(player) - 1, getY(player))) {
            Room currRoom = roomRepository.findByXAndY(getX(player) - 1, getY(player));
            player.setRoomId(currRoom.getId());
            personRepository.save(player);
            return "You moved to " + currRoom.getName() + "!" + "\n" + seeRoom();
        }

        return enemyAttack("You cannot perform that action");
    }

    @RequestMapping("/move_east")
    public @ResponseBody String moveRight() {
        Person player = personRepository.findById(playerId).get();
        if (checkMove(getX(player) + 1, getY(player))) {
            Room currRoom = roomRepository.findByXAndY(getX(player) + 1, getY(player));
            player.setRoomId(currRoom.getId());
            personRepository.save(player);
            return "You moved to " + currRoom.getName() + "!" + "\n" + seeRoom();
        }

        return enemyAttack("You cannot perform that action");
    }

    private boolean checkMove(int moveX, int moveY) {
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

    @RequestMapping("/reset")
    public @ResponseBody String reset()
    {
        roomRepository.deleteAll();
        passageRepository.deleteAll();
        activeRepository.deleteAll();
        personRepository.deleteAll();

        int max = 20;
        int iterations = 10;

        int startX = max + 5;
        int startY = max + 5;

        Person player = new Person("Player");

        Room first = new Room("Foyer", startX, startY);

        roomRepository.save(first);

        activeRepository.save(newObject(thingRepository.findByName("Stick"), first));

        player.setRoomId(first.getId());

        personRepository.save(player);

        playerId = player.getId();

        curr = 1;

        newRoom(20, startX, startY, -1);

        for (int i = 1; i < iterations; i++) {
            int currMax = r.nextInt(max);
            newRoom(currMax, startX, startY, -1);
        }

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

    private void newRoom(int left, int x, int y, int last) {
        if (left > 0) {
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
                roomTo = new Room("Room " + curr, x, y);
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
            newRoom(left, x, y, next);
        }
    }

    private void setThings(Room room) {
        ArrayList<Passage> passes = passageRepository.findAllByRoomFromOrRoomToAndReversableTrue(room.getId(), room.getId());

        ArrayList<Thing> money = thingRepository.findAllByType("money");
        ArrayList<Thing> enemies = thingRepository.findAllByType("enemy");
        ArrayList<Thing> items = thingRepository.findAllByType("weapon");
        items.addAll(thingRepository.findAllByType("potion"));
        items.addAll(thingRepository.findAllByType("key"));
        items.addAll(thingRepository.findAllByType("chest"));

        if (r.nextInt(4) == 1) {
            Thing toAdd = money.get(r.nextInt(money.size()));
            activeRepository.save(newObject(toAdd, room));
        }
        if (r.nextInt(4) == 1) {
            Thing toAdd = enemies.get(r.nextInt(enemies.size()));
            activeRepository.save(newObject(toAdd, room));
        }
        if (r.nextInt(2) == 1) {
            Thing toAdd = items.get(r.nextInt(items.size()));
            activeRepository.save(newObject(toAdd, room));
        }
    }

    private Active newObject(Thing toAdd, Room room) {
        return new Active(toAdd.getId(), r.nextInt(1 + toAdd.getValue2() - toAdd.getValue())
                + toAdd.getValue(), toAdd.getConquer(), room.getId(), 0, -1);
    }
}
