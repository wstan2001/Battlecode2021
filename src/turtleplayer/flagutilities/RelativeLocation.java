package turtleplayer.flagutilities;

import battlecode.common.MapLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public strictfp class RelativeLocation {
    public int x;
    public int y;

    public RelativeLocation(int x, int y){
        this.x = x;
        this.y = y;
    }

    public MapLocation applyTo(@Nullable MapLocation mapLocation){
        if(mapLocation == null){
            return null;
        }else {
            return mapLocation.translate(x,y);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RelativeLocation that = (RelativeLocation) o;
        return x == that.x && y == that.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "RelativeLocation{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}
