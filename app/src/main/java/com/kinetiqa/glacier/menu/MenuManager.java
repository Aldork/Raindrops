package com.kinetiqa.glacier.menu;

import android.content.Context;
import android.os.Environment;

import com.kinetiqa.glacier.components.MenuErrorMessage;
import com.kinetiqa.glacier.core.Config;
import com.kinetiqa.glacier.database.DatabaseHelper;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Created by Tom on 2014-11-01.
 */
public class MenuManager {
    public static int MEDIA_OTHER = -2;
    public static int MEDIA_UNDEFINED = -1;
    public static int MEDIA_FOLDER = 0;
    public static int MEDIA_VIDEO = 1;
    public static int MEDIA_DOCUMENT = 2;

    private static MenuManager mInstance;

    private Stack<Menu> menuStack;
    private static Menu rootMenu;

    private MenuManager() {
        menuStack = new Stack<Menu>();
    }

    public static MenuManager getInstance() {
        if (mInstance == null) {
            mInstance = new MenuManager();
            initMenuManager();
        }
        return mInstance;
    }

    private static void initMenuManager() {
        System.setProperty("org.xml.sax.driver", "org.xmlpull.v1.sax2.Driver");
        try {
            XMLReader reader = XMLReaderFactory.createXMLReader();
            reader.setContentHandler(new MenuXMLParser(mInstance));
            File xmlMenu = new File(Config.CONTENT_DIR_PATH + "menu.xml");
            InputStream inputStream = new FileInputStream(xmlMenu);

            Reader readerInput = new InputStreamReader(inputStream, "UTF-8");
            InputSource is = new InputSource(readerInput);
            reader.parse(is);

        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public MenuFolder getRootMenuFolder() {
        return (MenuFolder) rootMenu;
    }

    public MenuFolder getCurrentMenuFolder() {
        return (MenuFolder) menuStack.peek();
    }

    public boolean isRootMenuFolder(MenuFolder menuFolder) {
        return menuFolder.getID() == rootMenu.getID();
    }

    public List<Menu> getBreadcrumbMenuList() {
        Stack<Menu> breadcrumbStack = (Stack<Menu>) menuStack.clone();
        List<Menu> breadcrumbList = new ArrayList<Menu>();
        while (!breadcrumbStack.empty()) {
            breadcrumbList.add(0, breadcrumbStack.pop());
        }
        return breadcrumbList;
    }

    public MenuErrorMessage getOpenMenuErrorMessages(Context c, Menu menu) {
        boolean isPermitted = DatabaseHelper.getInstance(c).isPermitted(menu.getID());

        if (!isPermitted) {
            return new MenuErrorMessage("Locked", "This item has been locked by the trainer.");
        }

        MenuErrorMessage notCompletedPreRequisites = MenuManager.getInstance().getNotCompletedPreRequisites(c, menu);
        if (notCompletedPreRequisites != null) {
            return notCompletedPreRequisites;
        }

        return null;
    }

    public void openedMenu(Menu menu) {
        menuStack.push(menu);
    }

    public Menu closeMenu() {
        menuStack.pop();
        Menu currentMenu;
        if (menuStack.isEmpty()) {
            menuStack.push(rootMenu);
            currentMenu = rootMenu;
        } else {
            currentMenu = menuStack.peek();
        }
        return currentMenu;
    }

    public Menu findMenu(String itemIDtoFind, Menu menu) {
        if (itemIDtoFind.equals(menu.getID())) {
            return menu;
        }

        for (Menu subMenu : ((MenuFolder) menu).getSubMenus()) {
            if (subMenu instanceof MenuFolder) {
                Menu innerNode = findMenu(itemIDtoFind, subMenu);
                if (innerNode != null) {
                    return innerNode;
                }
            } else {
                // This node was a MenuItem; we cannot go any further so return null
                if (itemIDtoFind.equals(subMenu.getID())) {
                    return subMenu;
                }
            }
        }
        return null;
    }

    public Menu findMenu(String itemIDtoFind) {
        return findMenu(itemIDtoFind, rootMenu);
    }

    public Menu getRootNode() {
        return rootMenu;
    }

    public void setRootNode(Menu rootMenu) {
        this.rootMenu = rootMenu;
        menuStack.push(rootMenu);
    }

    // ----------


    public MenuErrorMessage getNotCompletedPreRequisites(Context context, Menu menu) {
        List<String> preRequisiteMenus = menu.getRequires();
        if (preRequisiteMenus == null) {
            return null;
        }

        if (preRequisiteMenus.size() > 0) {
            // Menu/Video/Activity has a restriction - must go check
            List<String> notCompletedPreRequisites = new ArrayList<String>();
            for (int s = 0; s < preRequisiteMenus.size(); s++) {
                boolean isComplete = DatabaseHelper.getInstance(context).isCompleted(preRequisiteMenus.get(s));
                if (!isComplete) {
                    notCompletedPreRequisites.add(preRequisiteMenus.get(s));
                }
            }

            StringBuilder notCompletedPreRequisitesStr = new StringBuilder();
            if (notCompletedPreRequisites.size() > 0) {
                for (int x = 0; x < notCompletedPreRequisites.size(); x++) {
                    notCompletedPreRequisitesStr.append(DatabaseHelper.getInstance(context).itemNameLookup(notCompletedPreRequisites.get(x)));
                    if (x != (notCompletedPreRequisites.size() - 1)) {
                        notCompletedPreRequisitesStr.append(", ");
                    }
                }
                return new MenuErrorMessage("Not Yet!", "You must first complete: " + notCompletedPreRequisitesStr.toString());
            } else {
                return null;
            }
        } else {
            return null;
        }
    }


}