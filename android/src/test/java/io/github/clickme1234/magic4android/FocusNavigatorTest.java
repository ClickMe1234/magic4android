package io.github.clickme1234.magic4android;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.List;
import static io.github.clickme1234.magic4android.FocusNavigator.*;

public class FocusNavigatorTest {
    private final List<Box> grid=List.of(new Box(0,0,100,100),new Box(120,0,220,100),new Box(0,120,100,220),new Box(120,120,220,220));
    @Test public void startsAtVisibleControlNearPointerWithoutExistingFocus() {
        assertEquals(3,nearest(grid,180,160));
        assertEquals(0,nearest(grid,-20,30));
        assertEquals(-1,nearest(List.of(),10,10));
    }
    @Test public void traversesGridAndDoesNotWrapAtEdge() {
        assertEquals(1,next(grid,grid.get(0),39));
        assertEquals(2,next(grid,grid.get(0),40));
        assertEquals(0,next(grid,grid.get(1),37));
        assertEquals(0,next(grid,grid.get(2),38));
        assertEquals(-1,next(grid,grid.get(1),39));
    }
    @Test public void prefersSameRowToCloserDiagonalControl() {
        Box from=new Box(0,0,100,100);
        assertEquals(1,next(List.of(new Box(110,110,210,210),new Box(250,0,350,100)),from,39));
    }
    @Test public void focusZoomDoesNotSelectTheSameTileAgain() {
        Box focused=new Box(765,588,1154,807);
        assertEquals(2,next(List.of(new Box(782,588,1137,797),new Box(427,588,782,797),new Box(675,144,1245,475)),focused,38));
    }
}
