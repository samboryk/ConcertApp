import jakarta.persistence.*;

@Entity
@Table(name = "votes")
public class Vote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne
    @JoinColumn(name = "song_id", nullable = false)
    private Song song;

    public Vote() {}
    public Vote(AppUser user, Song song) {
        this.user = user;
        this.song = song;
    }

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public Song getSong() { return song; }
}