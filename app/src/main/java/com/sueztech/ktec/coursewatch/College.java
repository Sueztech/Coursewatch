package com.sueztech.ktec.coursewatch;

import org.json.JSONException;
import org.json.JSONObject;

class College {

    private String id;
    private String name;

    College(JSONObject jsonObject) throws JSONException {
        this.id = jsonObject.getString("COLLEGE");
        this.name = jsonObject.getString("NAME");
    }

    @Override
    public String toString() {
        return this.name;
    }

    public String getId() {
        return this.id;
    }

}
