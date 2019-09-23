package com.kinetiqa.glacier.menu;

import java.util.List;

/**
 * Created by Tom on 2014-11-01.
 */
public abstract class Menu {
    protected String id;
    protected String title;
    protected String desc;
    protected List<String> requires;

    protected boolean isCompleted = false;
    protected boolean isBookmarked = false;
    protected boolean isPermitted = true;

    protected int interactionTime = 0;

    public String getID() {
        return id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public String getDesc() {
        return desc;
    }

    public List<String> getRequires() {
        return requires;
    }

    public String getRequiresString() {
        String requiresString = "";
        if (requires != null) {
            for (int i = 0; i < requires.size(); i++) {
                requiresString += requires.get(i) + ",";
            }
            requiresString = requiresString.substring(0, requiresString.length() - 1);
        }
        return requiresString;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean isCompleted) {
        this.isCompleted = isCompleted;
    }

    public boolean isBookmarked() {
        return isBookmarked;
    }

    public void setBookmarked(boolean isBookmarked) {
        this.isBookmarked = isBookmarked;
    }

    public boolean isPermitted() {
        return isPermitted;
    }

    public void setPermitted(boolean isPermitted) {
        this.isPermitted = isPermitted;
    }

    public int getInteractionTime() {
        return interactionTime;
    }

    public void setInteractionTime(int interactionTime) {
        this.interactionTime = interactionTime;
    }

}
