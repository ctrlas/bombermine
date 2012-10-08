package com.github.ctrlas.bombermine;

import org.bukkit.util.Vector;

public class AABB {
    private Vector aa;
    private Vector bb;

    public AABB(Vector aa, Vector bb) {
        this.aa = aa;
        this.bb = bb;
    }

    public Vector aa(){
        return aa;
    }

    public Vector bb(){
        return bb;
    }

    public boolean isContains(Vector l){
        if(!(aa.getBlockX() <= l.getBlockX() && l.getBlockX() <= bb.getBlockX())){
            return false;
        }
        if(!(aa.getBlockZ() <= l.getBlockZ() && l.getBlockZ() <= bb.getBlockZ())){
            return false;
        }
        if(!(aa.getBlockY() <= l.getBlockY() && l.getBlockY() <= bb.getBlockY())){
            return false;
        }
        return true;
    }
}
