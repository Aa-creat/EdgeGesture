package com.omarea.gesture.util;

import android.content.Intent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Recents {
    private final ArrayList<String> recents = new ArrayList<>();
    // 已经确保可以打开的应用
    public final List<String> whiteList = new CopyOnWriteArrayList<>();
    // 已经可以肯定不是可以打开的应用
    public final List<String> blackList = new CopyOnWriteArrayList<>();

    public volatile List<String> inputMethods = new CopyOnWriteArrayList<>();
    public volatile List<String> launcherApps = new CopyOnWriteArrayList<>();
    private int index = -1;
    private String currentTop = "";

    public void clear() {
        synchronized (recents) {
            recents.clear();
            currentTop = "";
            index = -1;
        }
    }

    public void addRecent(String packageName) {
        if (packageName == null || currentTop.equals(packageName)) {
            return;
        }

        synchronized (recents) {
            int searchResult = recents.indexOf(packageName);
            if (searchResult > -1) {
                recents.remove(searchResult);
            }

            // Intent.CATEGORY_HOME 代表桌面应用，回到桌面时，桌面应该永远在应用后面放
            // 因此，在桌面上向后退永远是打开桌面前的上一个应用，而不是上上个应用
            if (searchResult > -1 && !Intent.CATEGORY_HOME.equals(packageName)) {
                if (index >= 0 && index <= recents.size()) {
                    recents.add(index, packageName);
                } else {
                    recents.add(packageName);
                }
            } else {
                int indexCurrent = recents.indexOf(currentTop);
                if (indexCurrent > -1 && indexCurrent + 1 <= recents.size()) {
                    recents.add(indexCurrent + 1, packageName);
                } else {
                    recents.add(packageName);
                }
            }

            index = recents.indexOf(packageName);
            currentTop = packageName;
        }
    }

    void setRecents(ArrayList<String> items) {
        if (items == null) return;
        synchronized (recents) {
            ArrayList<String> lostedItems = new ArrayList<>();
            for (String recent : recents) {
                if (!recent.equals(Intent.CATEGORY_HOME) && !items.contains(recent)) {
                    lostedItems.add(recent);
                }
            }
            recents.removeAll(lostedItems);
            index = recents.indexOf(currentTop);
        }
    }

    public boolean notEmpty() {
        synchronized (recents) {
            return this.recents.size() > 1;
        }
    }

    public String getCurrent() {
        return currentTop;
    }

    public String moveNext() {
        String packageName = null;
        synchronized (recents) {
            if (recents.isEmpty()) {
                return null;
            }
            int size = recents.size();
            for (int i = 0; i < size; i++) {
                if (index < size - 1) {
                    index += 1;
                } else {
                    index = 0;
                }
                packageName = recents.get(index);
                if (!Intent.CATEGORY_HOME.equals(packageName) || size <= 1) {
                    break;
                }
            }
        }
        return packageName;
    }

    public String movePrevious() {
        String packageName = null;
        synchronized (recents) {
            if (recents.isEmpty()) {
                return null;
            }
            int size = recents.size();
            for (int i = 0; i < size; i++) {
                if (index > 0) {
                    index -= 1;
                } else {
                    index = size - 1;
                }
                packageName = recents.get(index);
                if (!Intent.CATEGORY_HOME.equals(packageName) || size <= 1) {
                    break;
                }
            }
        }
        return packageName;
    }
}
