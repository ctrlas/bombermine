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

    public Game(BomberMine bombermine, Location l, int size_factor, long seed){
        plugin = bombermine;
        world = l.getWorld();

        int cell_width = size_factor * 4 + 1;
        int cell_height = size_factor * 4 + 1;

        center = l.toVector();
        bf = new BattleField(plugin, world, seed, center, cell_width, cell_height);

        players = new ArrayList<Player>();
    }

    public void setup() {
        bf.setup();

        for(Player p :world.getPlayers()){
            p.setWalkSpeed(1.0f);
            if(bf.aabb().isContains(p.getLocation().toVector())){
                players.add(p);
            }
        }
    }

    public void finish() {
        players.clear();
        bf.clear();
    }

    public boolean isContains(Location l){
        return bf.isContains(l.toVector());
    }

    public void spawnBomb(Player player, Location location) {
        if(!players.contains(player)){
            return;
        }
        Pos pos = new Pos(center, location);
        bf.setBomb(player, pos.u, pos.v);
    }

    public boolean canSpawnBomb(Player player, Location location) {
        if(!players.contains(player)){
            return false;
        }

        if(center.getBlockY() != location.getBlockY()){
            return false;
        }

        Pos p = new Pos(center, location);
        if( (p.u&p.v&1) == 0){
            return true;
        }
        return false;
    }

    public class Pos{
        public int u;
        public int v;

        public Pos(){
            u = v = 0;
        }
        public Pos(int x, int z){
            this.u = x;
            this.v = z;
        }

        public Pos(Pos p){
            u = p.u;
            v = p.v;
        }

        public Pos(Vector center, Location location){
            int x = location.getBlockX();
            int z = location.getBlockZ();
            u = (x - center.getBlockX()+1)>>1;
            v = (z - center.getBlockZ()+1)>>1;
        }
    }
}
