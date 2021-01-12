package turtleplayer.flagutilities;

import battlecode.common.MapLocation;

import java.util.Objects;

public strictfp class RelativeLocation {
    private int x;
    private int y;

    public RelativeLocation(int x, int y){
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public MapLocation applyTo(MapLocation mapLocation){
        return new MapLocation(mapLocation.x + x, mapLocation.y + y);
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
