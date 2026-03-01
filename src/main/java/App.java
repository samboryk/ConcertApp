
import org.hibernate.Session;
import org.hibernate.Transaction;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.FileWriter;
import java.util.List;

public class App extends JFrame {
    private AppUser currentUser;

    private int maxVotesLimit = 3;
    private int maxConcertDuration = 90;

    private JTable songsTable;
    private DefaultTableModel tableModel;
    private JButton btnAdd, btnEdit, btnDelete, btnVote;
    private JTextArea reportArea;

    public App() {
        if (!showLoginForm()) {
            System.exit(0);
        }

        setTitle("Концерт на замовлення - " + currentUser.getLogin() + " (" + currentUser.getRole() + ")");
        setSize(850, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
        loadSongs();
    }

    private boolean showLoginForm() {
        JTextField loginField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        Object[] msg = {"Логін:", loginField, "Пароль:", passwordField};
        Object[] options = {"Увійти", "Реєстрація", "Вийти"};

        int choice = JOptionPane.showOptionDialog(null, msg, "Авторизація",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[0]);

        if (choice == 2 || choice == JOptionPane.CLOSED_OPTION) return false;

        String login = loginField.getText();
        String pass = new String(passwordField.getPassword());

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();

            if (session.createQuery("from AppUser", AppUser.class).getResultList().isEmpty()) {
                session.persist(new AppUser("admin", "admin", "Admin"));
                session.persist(new Song("Bohemian Rhapsody", "Queen", 6));
                session.persist(new Song("Ой у лузі", "Бумбокс", 3));
                session.persist(new Song("Shum", "Go_A", 4));
            }
            tx.commit();

            List<AppUser> users = session.createQuery("from AppUser where login = :l", AppUser.class)
                    .setParameter("l", login).getResultList();

            if (choice == 1) {
                if (!users.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Користувач вже існує!");
                    return showLoginForm();
                }
                tx = session.beginTransaction();
                currentUser = new AppUser(login, pass, "User");
                session.persist(currentUser);
                tx.commit();
                JOptionPane.showMessageDialog(null, "Реєстрація успішна!");
                return true;
            } else {
                if (users.isEmpty() || !users.get(0).getPassword().equals(pass)) {
                    JOptionPane.showMessageDialog(null, "Невірний логін або пароль!");
                    return showLoginForm();
                }
                currentUser = users.get(0);
                return true;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Помилка підключення до БД!", "Помилка", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }


    private void initUI() {
        JTabbedPane tabbedPane = new JTabbedPane();

        JPanel songsPanel = new JPanel(new BorderLayout());
        String[] cols = {"ID", "Назва", "Виконавець", "Тривалість (хв)", "Голоси"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        songsTable = new JTable(tableModel);
        songsPanel.add(new JScrollPane(songsTable), BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();
        btnAdd = new JButton("Додати пісню");
        btnEdit = new JButton("Редагувати");
        btnDelete = new JButton("Видалити");
        btnVote = new JButton("Проголосувати");


        btnEdit.setEnabled(false);
        btnDelete.setEnabled(false);
        btnVote.setEnabled(false);

        songsTable.getSelectionModel().addListSelectionListener(e -> {
            boolean hasSelection = songsTable.getSelectedRow() != -1;
            btnEdit.setEnabled(hasSelection);
            btnDelete.setEnabled(hasSelection);
            btnVote.setEnabled(hasSelection);
        });


        if ("Admin".equals(currentUser.getRole())) {
            controlPanel.add(btnAdd);
            controlPanel.add(btnEdit);
            controlPanel.add(btnDelete);
        } else {
            controlPanel.add(btnVote);
        }
        songsPanel.add(controlPanel, BorderLayout.SOUTH);
        tabbedPane.addTab("Список пісень", songsPanel);

        if ("User".equals(currentUser.getRole())) {
            JPanel myChoicePanel = new JPanel(new BorderLayout());
            JTextArea myVotesArea = new JTextArea();
            myVotesArea.setEditable(false);
            JButton btnRefresh = new JButton("Оновити мій вибір");
            btnRefresh.addActionListener(e -> myVotesArea.setText(getMyVotesText()));

            myChoicePanel.add(new JScrollPane(myVotesArea), BorderLayout.CENTER);
            myChoicePanel.add(btnRefresh, BorderLayout.SOUTH);
            tabbedPane.addTab("Мій вибір", myChoicePanel);

        } else if ("Admin".equals(currentUser.getRole())) {
            JPanel reportPanel = new JPanel(new BorderLayout());
            reportArea = new JTextArea();
            reportArea.setEditable(false);

            JPanel adminControls = new JPanel();
            JButton btnGenerate = new JButton("Сформувати концерт");
            adminControls.add(btnGenerate);
            btnGenerate.addActionListener(e -> generateConcertReport());

            reportPanel.add(new JScrollPane(reportArea), BorderLayout.CENTER);
            reportPanel.add(adminControls, BorderLayout.SOUTH);
            tabbedPane.addTab("Статистика та Звіт", reportPanel);
        }

        btnAdd.addActionListener(e -> showSongDialog(null));
        btnEdit.addActionListener(e -> editSong());
        btnDelete.addActionListener(e -> deleteSong());
        btnVote.addActionListener(e -> vote());

        add(tabbedPane);
    }

    private void loadSongs() {
        tableModel.setRowCount(0);
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Song> songs = session.createQuery("from Song order by id", Song.class).getResultList();
            for (Song s : songs) {
                tableModel.addRow(new Object[]{s.getId(), s.getTitle(), s.getArtist(), s.getDuration(), s.getVotesCount()});
            }
        }
    }

    private void showSongDialog(Song songToEdit) {
        boolean isEdit = (songToEdit != null);

        JTextField titleField = new JTextField(isEdit ? songToEdit.getTitle() : "");
        JTextField artistField = new JTextField(isEdit ? songToEdit.getArtist() : "");
        JTextField durationField = new JTextField(isEdit ? String.valueOf(songToEdit.getDuration()) : "");

        Object[] message = {"Назва пісні:", titleField, "Виконавець:", artistField, "Тривалість (хв):", durationField};
        String dialogTitle = isEdit ? "Редагування пісні" : "Додавання нової пісні";
        int option = JOptionPane.showConfirmDialog(this, message, dialogTitle, JOptionPane.OK_CANCEL_OPTION);

        if (option == JOptionPane.OK_OPTION) {
            try {
                String title = titleField.getText().trim();
                String artist = artistField.getText().trim();
                int duration = Integer.parseInt(durationField.getText().trim());

                if (title.isEmpty() || artist.isEmpty() || duration <= 0) {
                    JOptionPane.showMessageDialog(this, "Заповніть всі поля!", "Помилка", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                    Transaction tx = session.beginTransaction();
                    if (isEdit) {
                        Song s = session.get(Song.class, songToEdit.getId());
                        s.setTitle(title);
                        s.setArtist(artist);
                        s.setDuration(duration);
                    } else {
                        session.persist(new Song(title, artist, duration));
                    }
                    tx.commit();
                    loadSongs();
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Тривалість має бути числом!", "Помилка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void editSong() {
        int row = songsTable.getSelectedRow();
        if (row == -1) return;
        Long songId = (Long) tableModel.getValueAt(row, 0);

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Song song = session.get(Song.class, songId);
            if (song != null) showSongDialog(song);
        }
    }

    private void deleteSong() {
        int row = songsTable.getSelectedRow();
        if (row == -1) return;
        Long songId = (Long) tableModel.getValueAt(row, 0);

        int confirm = JOptionPane.showConfirmDialog(this, "Видалити пісню?", "Підтвердження", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            Song s = session.get(Song.class, songId);
            if (s != null) {
                session.createMutationQuery("delete from Vote where song.id = :sid")
                        .setParameter("sid", s.getId()).executeUpdate();
                session.remove(s);
            }
            tx.commit();
            loadSongs();
        }
    }


    private void vote() {
        int row = songsTable.getSelectedRow();
        if (row == -1) return;
        Long songId = (Long) tableModel.getValueAt(row, 0);

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            Long myVotesCount = session.createQuery("select count(v) from Vote v where v.user.id = :uid", Long.class)
                    .setParameter("uid", currentUser.getId()).getSingleResult();

            if (myVotesCount >= maxVotesLimit) {
                JOptionPane.showMessageDialog(this, "Перевищення ліміту! Ви можете обрати лише " + maxVotesLimit + " пісні.", "Помилка", JOptionPane.ERROR_MESSAGE);
                return;
            }


            Long specificVote = session.createQuery("select count(v) from Vote v where v.user.id = :uid and v.song.id = :sid", Long.class)
                    .setParameter("uid", currentUser.getId())
                    .setParameter("sid", songId).getSingleResult();

            if (specificVote > 0) {
                JOptionPane.showMessageDialog(this, "Ви вже проголосували за цю пісню!", "Увага", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Transaction tx = session.beginTransaction();
            Song song = session.get(Song.class, songId);
            song.setVotesCount(song.getVotesCount() + 1);
            session.persist(new Vote(currentUser, song));
            tx.commit();

            JOptionPane.showMessageDialog(this, "Голос зараховано! (Залишилось: " + (maxVotesLimit - myVotesCount - 1) + ")");
            loadSongs();
        }
    }

    private String getMyVotesText() {
        StringBuilder sb = new StringBuilder("Ваші обрані пісні:\n\n");
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Vote> myVotes = session.createQuery("from Vote v join fetch v.song where v.user.id = :uid", Vote.class)
                    .setParameter("uid", currentUser.getId()).getResultList();
            for (Vote v : myVotes) {
                sb.append("- ").append(v.getSong().toString()).append("\n");
            }
        }
        return sb.toString();
    }

    private void generateConcertReport() {
        StringBuilder report = new StringBuilder("=== ПРОГРАМА КОНЦЕРТУ (Макс: " + maxConcertDuration + " хв) ===\n\n");
        int currentDuration = 0;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            List<Song> topSongs = session.createQuery("from Song s order by s.votesCount desc", Song.class).getResultList();

            for (Song s : topSongs) {
                if (s.getVotesCount() == 0) continue;

                if (currentDuration + s.getDuration() <= maxConcertDuration) {
                    currentDuration += s.getDuration();
                    report.append(s.getArtist()).append(" - ").append(s.getTitle())
                            .append(" (").append(s.getDuration()).append(" хв) [Голосів: ")
                            .append(s.getVotesCount()).append("]\n");
                }
            }
            report.append("\nЗагальна тривалість: ").append(currentDuration).append(" хв.");
            reportArea.setText(report.toString());

            try (FileWriter fw = new FileWriter("Concert_Program.txt")) {
                fw.write(report.toString());
                JOptionPane.showMessageDialog(this, "Програму сформовано! Збережено у файл Concert_Program.txt");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new App().setVisible(true));
    }
}