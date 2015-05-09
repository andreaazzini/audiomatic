package se.kth.id2012_project;

public class Resource {
    private String name;
    private String imageUrl;
    private String audioUrl;

    public Resource(String name, String audioUrl, String imageUrl) {
        this.name = name;
        this.audioUrl = audioUrl;
        this.imageUrl = imageUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public String getName() {
        return name;
    }
}
