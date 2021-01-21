package turtleplayer.flagutilities;

import battlecode.common.MapLocation;

import java.util.Objects;

public strictfp class EncodedMapLocation {
    public int x;
    public int y;

    public EncodedMapLocation(MapLocation mapLocation){
        this.x = mapLocation.x & 0x07F;
        this.y = mapLocation.y & 0x07F;
    }

    public EncodedMapLocation(int x, int y){
        this.x = x & 0x07F;
        this.y = y & 0x07F;
    }

    public MapLocation applyTo(MapLocation mapLocation){
        int modX = mapLocation.x & 0x07F;
        int modY = mapLocation.y & 0x07F;
        int actX, actY;
        if(Math.abs(modX -x) < 64){
            actX = (mapLocation.x &0xFFFFFF80) | x;
        }else{
            actX = (mapLocation.x &0xFFFFFF80) | (128-x);
        }
        if(Math.abs(modY -y) < 64){
            actY = (mapLocation.y &0xFFFFFF80) | y;
        }else{
            actY = (mapLocation.y &0xFFFFFF80) | (128-y);
        }
        return new MapLocation(actX, actY);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EncodedMapLocation that = (EncodedMapLocation) o;
        return x == that.x && y == that.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "EncodedMapLocation{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}
