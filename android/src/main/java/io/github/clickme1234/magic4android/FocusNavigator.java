package io.github.clickme1234.magic4android;

import java.util.List;

/** Spatial fallback for apps which expose controls but no keyboard focus chain. */
public final class FocusNavigator {
    public record Box(int left,int top,int right,int bottom) {
        double cx(){return (left+right)/2.0;}
        double cy(){return (top+bottom)/2.0;}
    }
    public static int nearest(List<Box> boxes,float x,float y) {
        int best=-1;double distance=Double.MAX_VALUE;
        for(int i=0;i<boxes.size();i++) {
            Box b=boxes.get(i);
            double dx=Math.max(b.left-x,Math.max(0,x-b.right));
            double dy=Math.max(b.top-y,Math.max(0,y-b.bottom));
            double score=dx*dx+dy*dy;
            if(score<distance){distance=score;best=i;}
        }
        return best;
    }
    public static int next(List<Box> boxes,Box from,int key) {
        int best=-1;boolean bestBeam=false;double bestScore=Double.MAX_VALUE;
        for(int i=0;i<boxes.size();i++) {
            Box b=boxes.get(i);if(b.equals(from))continue;
            // Focus/zoom animations can give the selected item slightly different
            // bounds in two snapshots. It must not become its own next target.
            if(b.left<=from.cx() && b.right>=from.cx() && b.top<=from.cy() && b.bottom>=from.cy() &&
                from.left<=b.cx() && from.right>=b.cx() && from.top<=b.cy() && from.bottom>=b.cy())continue;
            boolean horizontal=key==37 || key==39;
            boolean ahead=switch(key){case 37->b.left<from.left && b.right<from.right;case 38->b.top<from.top && b.bottom<from.bottom;case 39->b.left>from.left && b.right>from.right;case 40->b.top>from.top && b.bottom>from.bottom;default->throw new IllegalArgumentException("Not an arrow key");};
            if(!ahead)continue;
            double primary=switch(key){case 37->from.cx()-b.cx();case 38->from.cy()-b.cy();case 39->b.cx()-from.cx();case 40->b.cy()-from.cy();default->throw new IllegalArgumentException("Not an arrow key");};
            if(primary<=0)continue;
            boolean beam=horizontal?b.bottom>from.top && b.top<from.bottom:b.right>from.left && b.left<from.right;
            double minor=horizontal?b.cy()-from.cy():b.cx()-from.cx();
            double score=13*primary*primary+minor*minor;
            if(best<0 || (beam && !bestBeam) || (beam==bestBeam && score<bestScore)) {
                best=i;bestBeam=beam;bestScore=score;
            }
        }
        return best;
    }
    private FocusNavigator() {}
}
