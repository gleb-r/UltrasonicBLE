package com.example.gleb.ultrasonicble;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

/**
 * Data of single height measurement
 * Created by Gleb on 13.09.2017.
 */

public class UltraHeight {

    private UUID uuid;

    //высота в мм
    private float height;
    // дата и время измерения
    private Date date;

    public UltraHeight (UUID uuid, float height, Date date) {
        this.uuid = uuid;
        this.height = height;
        this.date = date;
    }

    public UltraHeight( float height, Date date) {
        this(UUID.randomUUID(),height,date);
    }

    public UltraHeight (float height) {
        this(height, Calendar.getInstance().getTime());
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
