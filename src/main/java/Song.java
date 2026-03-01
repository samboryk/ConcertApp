import jakarta.persistence.*;

@Entity
@Table(name = "songs")
public class Song {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String artist;

    @Column(nullable = false)
    private int duration;

    @Column(name = "votes_count")
    private int votesCount;

    public Song() {}
    public Song(String title, String artist, int duration) {
        this.title = title;
        this.artist = artist;
        this.duration = duration;
        this.votesCount = 0;
    }


    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    public int getVotesCount() { return votesCount; }
    public void setVotesCount(int votesCount) { this.votesCount = votesCount; }

    @Override
    public String toString() {
        return artist + " - " + title + " (" + duration + " хв) - Голосів: " + votesCount;
    }
}