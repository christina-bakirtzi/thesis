package com.example.app.models;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class chord_model {
    @SerializedName("Chords")
    @Expose
    private List<String> chords = null;
    @SerializedName("Bass")
    @Expose
    private List<String> bass = null;
    @SerializedName("Time")
    @Expose
    private List<String> time = null;
    @SerializedName("AlternativeChords")
    @Expose
    private List<String> alternativeChords = null;
    @SerializedName("AlternativeChords2")
    @Expose
    private List<String> alternativeChords2 = null;


    public List<String> getChords() {
        return chords;
    }

    public void setChords(List<String> chords) {
        this.chords = chords;
    }

    public List<String> getBass() {
        return bass;
    }

    public void setBass(List<String> bass) {
        this.bass = bass;
    }

    public List<String> getTime() {
        return time;
    }

    public void setTime(List<String> time) {
        this.time = time;
    }

    public List<String> getAlternativeChords() {
        return alternativeChords;
    }

    public void setAlternativeChords(List<String> alternativeChords) {
        this.alternativeChords = alternativeChords;
    }

    public List<String> getAlternativeChords2() {
        return alternativeChords2;
    }

    public void setAlternativeChords2(List<String> alternativeChords2) {
        this.alternativeChords2 = alternativeChords2;
    }

}
