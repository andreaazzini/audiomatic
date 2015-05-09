package se.kth.id2012_project;

public class Resource {
    private String imageUrl;
    private String audioUrl;

    public Resource(String audioUrl, String imageUrl) {
        this.audioUrl = audioUrl;
        this.imageUrl = imageUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getAudioUrl() {
        return audioUrl;
    }
}
