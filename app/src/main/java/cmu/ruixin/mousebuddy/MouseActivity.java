package cmu.ruixin.mousebuddy;

/**
 * Created by JJ on 2/21/2015.
 */
public class MouseActivity {
    private boolean active;
    public int type;
    public static final int MOUSEMOVEMENT = 1;
    public static final int LEFTCLICK = 2;
    public static final int RIGHTCLICK = 3;
    public static final int SCROLL = 4;

    // for movement
    public float deltaX;
    public float deltaY;

    // for mouseup/mousedown
    public boolean leftMouseDown;
    public boolean rightMouseDown;

    public MouseActivity()
    {
        active = true;
    }

    public void deactivate()
    {
        active = false;
    }

    public boolean isActive()
    {
        return active;
    }

}
