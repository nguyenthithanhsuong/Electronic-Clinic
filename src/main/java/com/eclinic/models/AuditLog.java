package com.eclinic.models;

public class AuditLog {
    private long id;
    private String action;
    private String actor;
    private String target;
    private String createdAt;

    public AuditLog(long id, String action, String actor, String target, String createdAt) {
        this.id = id;
        this.action = action;
        this.actor = actor;
        this.target = target;
        this.createdAt = createdAt;
    }

    public AuditLog() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
