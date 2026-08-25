package com.codingame.game.ui;

import com.codingame.game.Constants;

import java.util.Objects;

/**
 * Function for holding empty barrier locations. Required to find the correct empty barrier sprite in a hashmap.
 */
public class EmptyBarrier {

    public final int y;
    public final int x;
    public final Constants.Orientation orientation;

    public EmptyBarrier(int y, int x, Constants.Orientation orientation){
        this.y = y;
        this.x = x;
        this.orientation = orientation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(y, x, orientation.equals(Constants.Orientation.VERT));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmptyBarrier that = (EmptyBarrier) o;
        return y == that.y &&
                x == that.x &&
                orientation == that.orientation;
    }
}
