package com.github.ctrlas.bombermine;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class Game {
    private BomberMine plugin;
    private World world;
    private Vector center;
    private BattleField bf;
    private List<Player> players;
    private State state;
    private Player owner;

    enum State{
        Playing,
        NotPlaying
    }

    public Game(BomberMine bombermine, Location l, int size_factor, long seed){
        plugin = bombermine;
        world = l.getWorld();

        int cell_width = size_factor * 4 + 1;
        int cell_height = size_factor * 4 + 1;

        center = l.toVector();
        bf = new BattleField(plugin, world, seed, center, cell_width, cell_height);

        state = State.NotPlaying;

        players = new ArrayList<Player>();
    }

    public void start(Player player) {
        if(state == State.Playing){
            owner.sendMessage("This battlefield is already started.");
            return;
        }

        owner = player;
        for(Player p :world.getPlayers()){
            if(bf.aabb().isContains(p.getLocation().toVector())){
                players.add(p);
            }
        }
        if(players.size() > bf.getMaxPlayers()){
            for(Player p: players){
                p.sendMessage("There are too many players in battle field.");
            }
            return;
        }
        state = State.Playing;
        bf.setup(players);
    }

    public void finish(Player player) {
        if(state != State.Playing){
            player.sendMessage("This battlefield is not started.");
            return;
        }
        if(!owner.equals(player)){
            player.sendMessage("you are not owner of this battlefield.");
            return;
        }
        owner = null;
        bf.clearWall();
        players.clear();
    }

    public void destroy(){
        bf.clear();
    }

    public boolean isContains(Location l){
        return bf.isContains(l.toVector());
    }

    public void spawnBomb(Player player, Location location) {
        if(!players.contains(player)){
            return;
        }
        //Pos pos = new Pos(center, location);
        bf.setBomb(player, location);
    }

    public boolean canSpawnBomb(Player player, Location location) {
        if(!players.contains(player)){
            return false;
        }

        if(center.getBlockY() != location.getBlockY()){
            return false;
        }

        if( bf.canSpawnBomb(location)){
            return true;
        }
        return false;
    }

    /*
    public Player getOwner() {
        return owner;
    }
    */
}
