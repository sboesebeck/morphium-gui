/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.caluga.morphium.gui.tests;

import de.caluga.morphium.annotations.Entity;
import de.caluga.morphium.annotations.Id;
import de.caluga.morphium.annotations.caching.Cache;
import de.caluga.morphium.gui.PanelClass;
import org.bson.types.ObjectId;

/**
 * @author stephan
 */
@Cache(clearOnWrite = true, maxEntries = 20000, readCache = true, writeCache = true, strategy = Cache.ClearStrategy.LRU, timeout = 1000)
@Entity
@PanelClass(de.caluga.morphium.gui.tests.CachedObjectPanel.class)
public class CachedObject {
    private String value;
    private int counter;

    @Id
    private ObjectId id;

    public int getCounter() {
        return counter;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }


    public String toString() {
        return "Counter: " + counter + " Value: " + value + " MongoId: " + id;
    }


}
