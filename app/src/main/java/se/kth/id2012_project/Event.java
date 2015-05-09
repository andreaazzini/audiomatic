package se.kth.id2012_project;

import java.util.HashMap;

public class Event {
    private String name;
    private HashMap<String, Resource> resources;

    public Event(String name) {
        this.name = name;
        resources = new HashMap<>();
    }

    public boolean hasResourceOfBeacon(String beaconUUID) {
        return resources.containsKey(beaconUUID);
    }

    public Resource getResourceOfBeacon(String beaconUUID) {
        return resources.get(beaconUUID);
    }

    public void saveResource(String beaconUUID, Resource resource) {
        resources.put(beaconUUID, resource);
    }

    public String getName() {
        return name;
    }
}
