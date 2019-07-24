package com.joseph.zorkapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Controller;
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

    Random r = new Random();

    // For auto room naming
    private int curr = 1;

    private int currX = 25;
    private int currY = 25;

    @RequestMapping("/room_check")
    public @ResponseBody String roomCheck() {
        return seeRoom();
    }

    @RequestMapping("/move_check")
    public @ResponseBody String moveCheck() {
        String result = "";
        if (checkMove(currX, currY - 1)) {
            Room currRoom = roomRepository.findByXAndY(currX, currY - 1);
            result += currRoom.getName() + " is to the north.\n";
        }
        if (checkMove(currX - 1, currY)) {
            Room currRoom = roomRepository.findByXAndY(currX - 1, currY);
            result += currRoom.getName() + " is to the west.\n";
        }
        if (checkMove(currX, currY + 1)) {
            Room currRoom = roomRepository.findByXAndY(currX, currY + 1);
            result += currRoom.getName() + " is to the south.\n";
        }
        if (checkMove(currX + 1, currY)) {
            Room currRoom = roomRepository.findByXAndY(currX + 1, currY);
            result += currRoom.getName() + " is to the east.\n";
        }

        if (result.equals("")) {
            return "You have nowhere to move.";
        }
        return result;
    }

    private String seeRoom() {
        Room room = roomRepository.findByXAndY(currX, currY);
        String result = "Things in the room:\n";
        for (Active active : activeRepository.findAllByRoomId(room.getId())) {
            result += thingRepository.findById(active.getThingId()).get().getName() + "\n";
        }
        return result;
    }

    @RequestMapping("/move_north")
    public @ResponseBody String moveUp() {
        if (checkMove(currX, currY - 1)) {
            currY--;
            Room currRoom = roomRepository.findByXAndY(currX, currY);
            return "You moved to " + currRoom.getName() + "!" + "\n" + seeRoom();
        }

        return "You cannot perform that action";
    }

    @RequestMapping("/move_south")
    public @ResponseBody String moveDown() {
        if (checkMove(currX, currY + 1)) {
            currY++;
            Room currRoom = roomRepository.findByXAndY(currX, currY);
            return "You moved to " + currRoom.getName() + "!" + "\n" + seeRoom();
        }

        return "You cannot perform that action";
    }

    @RequestMapping("/move_west")
    public @ResponseBody String moveLeft() {
        if (checkMove(currX - 1, currY)) {
            currX--;
            Room currRoom = roomRepository.findByXAndY(currX, currY);
            return "You moved to " + currRoom.getName() + "!" + "\n" + seeRoom();
        }

        return "You cannot perform that action";
    }

    @RequestMapping("/move_east")
    public @ResponseBody String moveRight() {
        if (checkMove(currX + 1, currY)) {
            currX++;
            Room currRoom = roomRepository.findByXAndY(currX, currY);
            return "You moved to " + currRoom.getName() + "!" + "\n" + seeRoom();
        }

        return "You cannot perform that action";
    }

    private boolean checkMove(int moveX, int moveY) {
        Room currRoom = roomRepository.findByXAndY(currX, currY);
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

        int max = 20;
        int iterations = 10;

        int startX = max + 5;
        int startY = max + 5;

        currX = startX;
        currY = startY;

        roomRepository.save(new Room("Foyer", startX, startY));

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
            Boolean added = false;
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

        if (r.nextInt(4) == 1) {
            Thing toAdd = money.get(r.nextInt(money.size()));
            activeRepository.save(new Active(toAdd.getId(),
                    r.nextInt(1 + toAdd.getValue2() - toAdd.getValue()) + toAdd.getValue(), toAdd.getConquer(),
                    room.getId(), 0, -1));
        }
        if (r.nextInt(4) == 1) {
            Thing toAdd = enemies.get(r.nextInt(enemies.size()));
            activeRepository.save(new Active(toAdd.getId(),
                    r.nextInt(1 + toAdd.getValue2() - toAdd.getValue()) + toAdd.getValue(), toAdd.getConquer(),
                    room.getId(), 0, -1));
        }
        if (r.nextInt(4) == 1) {
            Thing toAdd = items.get(r.nextInt(items.size()));
            activeRepository.save(new Active(toAdd.getId(),
                    r.nextInt(1 + toAdd.getValue2() - toAdd.getValue()) + toAdd.getValue(), toAdd.getConquer(),
                    room.getId(), 0, -1));
        }
    }
}
