package com.github.ctrlas.bombermine;

import java.util.Random;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;


public class BattleField {

    enum CellType{
        //static
        Room, Wall,

        //dynamic
        Breakable, //壊れる壁
        Bomb, //爆弾
        Fire, //火炎

        //temp
        Explosion,//炸裂する爆弾
        Vanish //破壊される壁
    }

    private BomberMine plugin;
    private World world;
    private Random rand;

    private AABB aabb;
    private int width, height;
    private Vector center;

    // [u][v]
    private CellType[][] map;
    private Player[][] bomb_owner;
    private int[][] timer;

    final int TICK = 20;
    int bombdelay = 8;
    int firedelay = 2;

    BattleField(BomberMine plugin, World world, long seed, Vector center, int width, int height){
        this.plugin = plugin;
        this.world = world;
        this.rand = new Random(seed);
        this.center = center;
        this.width = width; // must be 4x+1
        this.height = height; // must be 4x+1

        int block_width = width * 2 + 2; // +2 is outer wall
        int block_height = height * 2 + 2; // +2 is outer wall

        aabb = new AABB(
                center.clone().add(new Vector(-block_width/2, -1, -block_height/2)),
                center.clone().add(new Vector(block_width/2-1, 3, block_height/2-1))
                );

        map = new CellType[width][height];
        timer = new int[width][height];
        bomb_owner = new Player[width][height];

        for(int u = -width/2; u<= width/2; ++u){
            for(int v = -height/2; v<= height/2; ++v){
                if((u&v&1) == 1){
                    buildCell(u, v, CellType.Wall);
                }else{
                    buildCell(u, v, CellType.Room);
                }
            }
        }
    }

    public void setup() {
        buildWall();
        for(int u = -width/2; u<= width/2; ++u){
            for(int v = -height/2; v<= height/2; ++v){
                if(getCellType(u, v) == CellType.Wall)
                    continue;
                if(rand.nextInt(100)<70){
                    buildCell(u, v, CellType.Breakable);
                }else{
                    buildCell(u, v, CellType.Room);
                }
            }
        }

        // プレイヤー用の空き
        buildCell(-width()/2, -height()/2, CellType.Room);
        buildCell(-width()/2+1, -height()/2, CellType.Room);
        buildCell(-width()/2, -height()/2+1, CellType.Room);

        buildCell(width()/2, -height()/2, CellType.Room);
        buildCell(width()/2-1, -height()/2, CellType.Room);
        buildCell(width()/2, -height()/2+1, CellType.Room);

        buildCell(-width()/2, height()/2, CellType.Room);
        buildCell(-width()/2+1, height()/2, CellType.Room);
        buildCell(-width()/2, height()/2-1, CellType.Room);

        buildCell(width()/2, height()/2, CellType.Room);
        buildCell(width()/2-1, height()/2, CellType.Room);
        buildCell(width()/2, height()/2-1, CellType.Room);
    }

    public void clear(){
        int y = center.getBlockY();
        for(int xx = aabb.aa().getBlockX(); xx <= aabb.bb().getBlockX(); ++xx){
            for(int zz = aabb.aa().getBlockZ(); zz <= aabb.bb().getBlockZ(); ++zz){
                world.getBlockAt(xx, y-1, zz).setType(Material.GRASS);
                for(int i=0; i<4; ++i){
                    world.getBlockAt(xx, y+i,zz).setType(Material.AIR);
                }
            }
        }
    }

    public int width(){
        return width;
    }

    public int height(){
        return height;
    }

    private void buildCell(int u, int v, CellType type){
        map[width/2 + u][height/2 + v] = type;
        setCell2x2(u, v, type);
    }

    public void setBomb(Player player, final int u, final int v){
        map[width/2 + u][height/2 + v] = CellType.Bomb;
        bomb_owner[width/2 + u][height/2 + v] = player;

        setBlock2x2(u, v, Material.COBBLESTONE.getId(), Material.TNT.getId(), Material.TNT.getId());

        timer[width/2 + u][height/2 + v] = plugin.getServer().getScheduler().scheduleAsyncDelayedTask(plugin,new Runnable(){
            public void run(){
                plugin.getLogger().info("explode!");
                explodeBomb(u, v);
            }
        }, TICK * bombdelay);
    }


    private void setCellType(int u, int v, CellType type){
        map[width/2 + u][height/2 + v] = type;
    }

    public CellType getCellType(int u, int v){
        return map[width/2 + u][height/2 + v];
    }

    public void buildWall(){
        // build outer walls
        int y = center.getBlockY();
        int x1 = aabb.aa().getBlockX();
        int x2 = aabb.bb().getBlockX();
        int z1 = aabb.aa().getBlockZ();
        int z2 = aabb.bb().getBlockZ();
        for(int x = x1; x <= x2; ++x){
            for(int i=0; i<3; ++i){
                world.getBlockAt(x, y+i,z1).setTypeIdAndData(43, (byte)0, true);
                world.getBlockAt(x, y+i,z2).setTypeIdAndData(43, (byte)0, true);
            }
        }
        for(int z = z1; z <= z2; ++z){
            for(int i=0; i<3; ++i){
                world.getBlockAt(x1, y+i,z).setTypeIdAndData(43, (byte)0, true);
                world.getBlockAt(x2, y+i,z).setTypeIdAndData(43, (byte)0, true);
            }
        }
    }

    public void clearWall(){
        // clear outer walls
        int y = center.getBlockY();
        int x1 = aabb.aa().getBlockX();
        int x2 = aabb.bb().getBlockX();
        int z1 = aabb.aa().getBlockZ();
        int z2 = aabb.bb().getBlockZ();
        for(int x = x1; x <= x2; ++x){
            for(int i=0; i<3; ++i){
                world.getBlockAt(x, y+i,z1).setTypeIdAndData(0, (byte)0, true);
                world.getBlockAt(x, y+i,z2).setTypeIdAndData(0, (byte)0, true);
            }
        }
        for(int z = z1; z <= z2; ++z){
            for(int i=0; i<3; ++i){
                world.getBlockAt(x1, y+i,z).setTypeIdAndData(0, (byte)0, true);
                world.getBlockAt(x2, y+i,z).setTypeIdAndData(0, (byte)0, true);
            }
        }
    }


    public boolean isContains(Vector v){
        return aabb.isContains(v);
    }

    private void setBlock2x2(int u, int v, int id, int id2, int id3){
        int cx = center.getBlockX();
        int cz = center.getBlockZ();
        int y = center.getBlockY()-1;

        if(id>=0){
            world.getBlockAt(cx + u * 2, y, cz + v * 2).setTypeId(id);
            world.getBlockAt(cx + u * 2-1, y, cz + v * 2).setTypeId(id);
            world.getBlockAt(cx + u * 2, y, cz + v * 2-1).setTypeId(id);
            world.getBlockAt(cx + u * 2-1, y, cz + v * 2-1).setTypeId(id);
        }

        ++y;
        if(id2>=0){
            world.getBlockAt(cx + u * 2, y, cz + v * 2).setTypeId(id2);
            world.getBlockAt(cx + u * 2-1, y, cz + v * 2).setTypeId(id2);
            world.getBlockAt(cx + u * 2, y, cz + v * 2-1).setTypeId(id2);
            world.getBlockAt(cx + u * 2-1, y, cz + v * 2-1).setTypeId(id2);
        }
        ++y;

        if(id3>=0){
            world.getBlockAt(cx + u * 2, y, cz + v * 2).setTypeId(id3);
            world.getBlockAt(cx + u * 2-1, y, cz + v * 2).setTypeId(id3);
            world.getBlockAt(cx + u * 2, y, cz + v * 2-1).setTypeId(id3);
            world.getBlockAt(cx + u * 2-1, y, cz + v * 2-1).setTypeId(id3);
        }
    }

    private void setCell2x2(int u, int v, CellType type){
        int cx = center.getBlockX();
        int cz = center.getBlockZ();
        int y = center.getBlockY()-1;
        int x1 = cx + u * 2-1;
        int x2 = cx + u * 2;
        int z1 = cz + v * 2-1;
        int z2 = cz + v * 2;

        int m[] = new int[5];
        byte d[] = new byte[5];
        m[0] = Material.COBBLESTONE.getId();
        m[1] = m[2] = m[3] = Material.AIR.getId();
        m[4] = Material.AIR.getId(); //Material.GLASS.getId();
        d[0] = d[1] = d[2] = d[3] = d[4] = 0;
        switch(type){
        case Room: //room
            break;
        case Wall: //inner wall
            m[1] = Material.DOUBLE_STEP.getId();
            m[2] = Material.STEP.getId();
            break;
        case Breakable: //breakable wall
            m[1] = Material.DOUBLE_STEP.getId();
            m[2] = Material.STEP.getId();
            d[1] = d[2] = 2; //wooden
            break;
        default: //outer wall
            m[1] = m[2] = m[3] = Material.DOUBLE_STEP.getId();
            break;
        }

        for(int i=0; i<5; ++i){
            world.getBlockAt(x1, y+i, z1).setTypeIdAndData(m[i], d[i], true);
            world.getBlockAt(x2, y+i, z1).setTypeIdAndData(m[i], d[i], true);
            world.getBlockAt(x1, y+i, z2).setTypeIdAndData(m[i], d[i], true);
            world.getBlockAt(x2, y+i, z2).setTypeIdAndData(m[i], d[i], true);
        }
    }

    public void explodeBomb(int u, int v) {
        explodeBombSub(u, v);
        replaceAllCellType(CellType.Vanish, CellType.Room);
        replaceAllCellType(CellType.Explosion, CellType.Fire);
    }
    private void explodeBombSub(int u, int v) {
        if(getCellType(u, v) != CellType.Bomb){
            //既に爆破済み？
            return;
        }

        //timer のキャンセル
        if(timer[width/2+u][height/2+v] != -1){
            plugin.getServer().getScheduler().cancelTask(timer[width/2+u][height/2+v]);
            timer[width/2+u][height/2+v] = -1;
        }

        // 爆弾の持ち主へ返す
        bomb_owner[width/2+u][height/2+v].getInventory().addItem(new ItemStack(Material.TNT.getId(), 1));

        setBlock2x2(u, v, Material.LAVA.getId(), Material.AIR.getId(), Material.AIR.getId());

        //爆風が貫通しないように一時的に壁扱いする。
        setCellType(u, v, CellType.Explosion);

        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();
        world.createExplosion(
                cx + u*2 + 0.5,
                cy,
                cz + v*2 + 0.5,
                3,
                false);

        final int firepower = 3;

        spreadFire(firepower, u,v, -1,0);
        spreadFire(firepower, u,v, +1,0);
        spreadFire(firepower, u,v, 0,-1);
        spreadFire(firepower, u,v, 0,+1);

        final int uu = u;
        final int vv = v;
        plugin.getServer().getScheduler().scheduleAsyncDelayedTask(plugin, new Runnable(){
            public void run(){
                extinctFire(firepower, uu,vv, -1,0);
                extinctFire(firepower, uu,vv, +1,0);
                extinctFire(firepower, uu,vv, 0,-1);
                extinctFire(firepower, uu,vv, 0,+1);
            }
        }, TICK * firedelay);
    }

    private void spreadFire(int firepower, int u, int v, int du, int dv){
        for(int i=1; i<= firepower; ++i){
            //範囲外に出そうなら止める
            int tu = u+du*i;
            int tv = v+dv*i;
            if(tu > width/2) break;
            if(tu < -width/2) break;
            if(tv > height/2) break;
            if(tv < -height/2) break;

            //壁に当たったら止まる
            if(getCellType(tu, tv) == CellType.Wall){
                break;
            }

            //炸裂中の爆弾にあたったら止まる
            if(getCellType(tu, tv) == CellType.Explosion){
                break;
            }

            //消滅中の壁にあたったら止まる
            if(getCellType(tu, tv) == CellType.Vanish){
                break;
            }

            //壊れる壁に当たったら壊して止まる
            if(getCellType(tu, tv) == CellType.Breakable){
                setBlock2x2(tu, tv, -1, Material.AIR.getId(), Material.AIR.getId());
                spawnItem(tu,tv);
                // 消滅中の壁
                setCellType(tu, tv, CellType.Vanish);
                break;
            }

            //爆弾は誘爆する
            if(getCellType(tu, tv) == CellType.Bomb){
                explodeBombSub(tu,tv);
            }

            //通った軌跡を溶岩化する
            setBlock2x2(tu, tv, Material.LAVA.getId(), Material.AIR.getId(), Material.AIR.getId());
            setCellType(tu, tv, CellType.Fire);
        }
    }

    private void spawnItem(int u, int v) {
        int cx = center.getBlockX();
        int cz = center.getBlockZ();
        int y = center.getBlockY();
        Location l = new Location(world, cx + u * 2 - 0.5, y, cz + v*2 -0.5);
        if(rand.nextInt(100)<50){
            world.dropItem(l, new ItemStack(Material.TNT.getId(), 1));
        }
    }

    private void extinctFire(int firepower, int u, int v, int du, int dv){
        for(int i=0; i<= firepower; ++i){
            int tu = u+du*i;
            int tv = v+dv*i;

            //範囲外に出そうなら止める
            if(tu > width/2 || tu < -width/2) break;
            if(tv > height/2 || tv < -height/2) break;

            if(getCellType(tu, tv) == CellType.Fire || getCellType(tu, tv) == CellType.Room){
                setBlock2x2(tu, tv, Material.COBBLESTONE.getId(), -1, -1);
                setCellType(tu, tv, CellType.Room);
                continue;
            }
            break;
        }
    }

    private void replaceAllCellType(CellType from, CellType to) {
        for(int u = -width()/2; u<= width()/2; ++u){
            for(int v = -height()/2; v<= height()/2; ++v){
                if(getCellType(u, v) == from){
                    setCellType(u, v, to);
                }
            }
        }
    }

    public AABB aabb() {
        return aabb;
    }

}
