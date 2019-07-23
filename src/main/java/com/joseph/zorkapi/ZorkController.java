package com.joseph.zorkapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Random;

@Controller
public class ZorkController {

    @Autowired
    RoomRepository roomRepository;

    @Autowired
    PassageRepository passageRepository;

    private int curr = 1;

    private int currX;
    private int currY;

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

    @RequestMapping("/move_north")
    public @ResponseBody String moveUp() {
        if (checkMove(currX, currY - 1)) {
            currY--;
            Room currRoom = roomRepository.findByXAndY(currX, currY);
            return "You moved to " + currRoom.getName() + "!";
        }

        return "You cannot perform that action";
    }

    @RequestMapping("/move_south")
    public @ResponseBody String moveDown() {
        if (checkMove(currX, currY + 1)) {
            currY++;
            Room currRoom = roomRepository.findByXAndY(currX, currY);
            return "You moved to " + currRoom.getName() + "!";
        }

        return "You cannot perform that action";
    }

    @RequestMapping("/move_west")
    public @ResponseBody String moveLeft() {
        if (checkMove(currX - 1, currY)) {
            currX--;
            Room currRoom = roomRepository.findByXAndY(currX, currY);
            return "You moved to " + currRoom.getName() + "!";
        }

        return "You cannot perform that action";
    }

    @RequestMapping("/move_east")
    public @ResponseBody String moveRight() {
        if (checkMove(currX + 1, currY)) {
            currX++;
            Room currRoom = roomRepository.findByXAndY(currX, currY);
            return "You moved to " + currRoom.getName() + "!";
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

        int max = 20;
        int iterations = 10;

        int startX = max + 5;
        int startY = max + 5;

        currX = startX;
        currY = startY;

        roomRepository.save(new Room("Foyer", startX, startY));

        curr = 1;

        newRoom(20, startX, startY, -1);

        Random r = new Random();

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
            if (roomTo == null) {
                roomTo = new Room("Room " + curr, x, y);
                roomRepository.save(roomTo);
                curr++;
            }
            Passage pass = passageRepository.findByRoomFromAndRoomTo(roomFrom.getId(), roomTo.getId());
            if (pass == null) {
                pass = passageRepository.findByRoomFromAndRoomTo(roomTo.getId(), roomFrom.getId());
            }
            if (pass == null) {
                pass = new Passage(roomFrom.getId(), roomTo.getId(), true);
                passageRepository.save(pass);
            }
            left--;
            newRoom(left, x, y, next);
        }
    }
}
