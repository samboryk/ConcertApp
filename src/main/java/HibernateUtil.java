
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import java.io.InputStream;
import java.util.Properties;

public final class HibernateUtil {
    private static SessionFactory sessionFactory;

    private HibernateUtil() {}

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            Properties props = new Properties();
            try (InputStream in = HibernateUtil.class.getClassLoader().getResourceAsStream("hibernate.properties")) {
                if (in == null) throw new IllegalStateException("Файл hibernate.properties не знайдено");
                props.load(in);
            } catch (Exception e) {
                throw new RuntimeException("Помилка ініціалізації Hibernate", e);
            }

            Configuration cfg = new Configuration();
            cfg.setProperties(props);

            cfg.addAnnotatedClass(Song.class);
            cfg.addAnnotatedClass(AppUser.class);
            cfg.addAnnotatedClass(Vote.class);

            sessionFactory = cfg.buildSessionFactory();
        }
        return sessionFactory;
    }
}