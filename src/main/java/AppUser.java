import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String login;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private boolean voted;

    public AppUser() {}
    public AppUser(String login, String password, String role) {
        this.login = login;
        this.password = password;
        this.role = role;
        this.voted = false;
    }

    public Long getId() { return id; }
    public String getLogin() { return login; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
    public boolean isVoted() { return voted; }
    public void setVoted(boolean voted) { this.voted = voted; }
}