package cmu.ruixin.mousebuddy;

/**
 * Created by JJ on 2/21/2015.
 */
public class MouseActivity {
    private boolean active;
    public int type;
    public float deltaX;
    public float deltaY;

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
