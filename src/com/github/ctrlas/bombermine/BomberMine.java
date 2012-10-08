package com.github.ctrlas.bombermine;
import java.util.ArrayList;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class BomberMine extends JavaPlugin implements Listener {
    ArrayList<Game> games;
    Random rand;

    @Override
    public void onEnable(){
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(this, this);
        rand = new Random();
        games = new ArrayList<Game>();
    }

    @Override
    public void onDisable(){
        for(Game game : games){
            game.finish();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if(!cmd.getName().equalsIgnoreCase("bm")){
            return false;
        }

        if(args.length < 1){
            sender.sendMessage("/bm c[command]");
            sender.sendMessage("/bm create [size]");
            sender.sendMessage("/bm s");
            sender.sendMessage("/bm start");
            sender.sendMessage("/bm d [size]");
            sender.sendMessage("/bm destroy [size]");
            return true;
        }

        if(sender instanceof Player){
            Player player = (Player)sender;

            if(args[0].equalsIgnoreCase("c") || args[0].equalsIgnoreCase("create")){
                int size_factor = 3;
                if(args.length >= 2){
                    size_factor = Math.max(1, Math.min(5, Integer.parseInt(args[1])));
                }
                Game game = new Game(this, player.getLocation(), size_factor, rand.nextLong());
                games.add(game);
                return true;
            }else if(args[0].equalsIgnoreCase("s") || args[0].equalsIgnoreCase("start")){
                Game game = findGame(player.getLocation());
                if(game != null){
                    game.setup();
                }else{
                    sender.sendMessage("game not found at your position");
                }
                return true;
            }else if(args[0].equalsIgnoreCase("d") || args[0].equalsIgnoreCase("destroy")){
                Game game = findGame(player.getLocation());
                if(game != null){
                    game.finish();
                    games.remove(game);
                }else{
                    sender.sendMessage("game not found at your position");
                }
                return true;
            }
        }

        return false;
    }

    //ブロックの設置をトラップ
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event){
        //Player p = event.getPlayer();
        final Block b = event.getBlock();
        Game game = findGame(b.getLocation());
        if(game == null){
            return;
        }

        //TNT
        if(b.getTypeId() == 46){
            if(game.canSpawnBomb(event.getPlayer(), b.getLocation())){
                game.spawnBomb(event.getPlayer(), b.getLocation());
                return;
            }
        }
        event.setCancelled(true);
    }

    //ブロックが人の手でダメージを受けるのとをめる。
    @EventHandler
    public void onBlockDamage(BlockDamageEvent event){
        //Player p = event.getPlayer();
        final Block b = event.getBlock();
        Game game = findGame(b.getLocation());
        if(game == null){
            return;
        }
        event.setCancelled(true);
    }

    //ブロックが人の手で破壊されるのを止める
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event){
        final Block b = event.getBlock();
        Game game = findGame(b.getLocation());
        if(game == null){
            return;
        }
        event.setCancelled(true);
    }

    //ブロックが炎上するのを止める
    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        final Block b = event.getBlock();
        Game game = findGame(b.getLocation());
        if(game == null){
            return;
        }
        //炎上を防ぐ
        event.setCancelled(true);
    }

    //延焼を止める
    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        final Block b = event.getBlock();
        Game game = findGame(b.getLocation());
        if(game == null){
            return;
        }
        //延焼を防ぐ
        if(event.getCause() == IgniteCause.SPREAD){
            getLogger().info("spread");
            event.setCancelled(true);
        }
    }

    //爆発による影響をキャンセルする
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event){
        Location l = event.getLocation();
        Game game = findGame(l);
        if(game == null){
            return;
        }
        //no affect
        event.blockList().clear();
    }

    private Game findGame(Location l){
        for(Game g: games)
            if(g.isContains(l)) return g;
        return null;
    }
}
