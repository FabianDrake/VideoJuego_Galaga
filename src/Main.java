import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class Main extends JFrame {
    private GamePanel gamePanel;

    public Main() {
        setTitle("Fabian Galaga Game");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        gamePanel = new GamePanel();
        add(gamePanel);
        setVisible(true);
    }

    public static void main(String[] args) {
        new Main();
    }
}

enum GameState { MENU, PLAYING, WON, LOST }

class GamePanel extends JPanel implements ActionListener, KeyListener {
    private Player player;
    private ArrayList<Enemy> enemies;
    private ArrayList<Projectile> projectiles;
    private Timer timer;
    private GameState gameState = GameState.MENU;
    private int timeLeft = 30;
    private int score = 0; // Contador de puntaje

    private Image cursorImage;

    public GamePanel() {
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        loadCursorImage();

        timer = new Timer(1000 / 30, this);

        new Timer(1000, e -> {
            if (gameState == GameState.PLAYING) {
                timeLeft--;
                if (timeLeft <= 0) {
                    gameState = GameState.LOST;
                    timer.stop();
                }
                repaint();
            }
        }).start();
    }

    private void loadCursorImage() {
        cursorImage = Toolkit.getDefaultToolkit().getImage("Imagenes/nave.png");
    }

    public void startGame() {
        player = new Player(400, 500, Toolkit.getDefaultToolkit().getImage("Imagenes/nave.png")); // Cargar imagen del jugador
        enemies = new ArrayList<>();
        projectiles = new ArrayList<>();
        timeLeft = 30;
        score = 0; // Reiniciar puntaje

        for (int row = 0; row < 3; row++) {
            for (int i = 0; i < 8; i++) {
                enemies.add(new Enemy(50 + i * 80, 50 + row * 50));
            }
        }

        gameState = GameState.PLAYING;
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (gameState == GameState.MENU) {
            drawMenu(g);
        } else if (gameState == GameState.PLAYING) {
            drawGame(g);
        } else if (gameState == GameState.WON || gameState == GameState.LOST) {
            drawEndScreen(g);
        }

        drawCustomCursor(g);
    }

    private void drawCustomCursor(Graphics g) {
        Point mousePos = MouseInfo.getPointerInfo().getLocation();
        SwingUtilities.convertPointFromScreen(mousePos, this);
        g.drawImage(cursorImage, mousePos.x, mousePos.y, null);
    }

    private void drawMenu(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 36));
        g.drawString("GALAGA HUMILDE", 200, 200);
        g.setFont(new Font("Arial", Font.PLAIN, 24));
        g.drawString("Presiona ENTER para iniciar", 250, 300);
    }

    private void drawGame(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        GradientPaint gradient = new GradientPaint(0, 0, Color.DARK_GRAY, 0, getHeight(), Color.BLACK);
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        player.draw(g);

        for (Projectile projectile : projectiles) {
            projectile.draw(g);
        }

        for (Enemy enemy : enemies) {
            enemy.draw(g);
        }

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString("Tiempo restante: " + timeLeft + "s", 10, 30);
        g.drawString("Puntaje: " + score, 10, 60); // Mostrar puntaje
    }

    private void drawEndScreen(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 36));

        if (gameState == GameState.WON) {
            g.drawString("¡Has ganado!", 300, 200);
        } else {
            g.drawString("¡Has perdido!", 300, 200);
        }

        g.setFont(new Font("Arial", Font.PLAIN, 24));
        g.drawString("Presiona ESC para volver al menú", 220, 300);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameState != GameState.PLAYING) return;

        player.move();

        projectiles.removeIf(p -> p.getY() < 0);
        for (Projectile projectile : projectiles) {
            projectile.move();
        }

        checkCollisions();

        for (Enemy enemy : enemies) {
            enemy.move();
        }

        if (enemies.isEmpty()) {
            gameState = GameState.WON;
            timer.stop();
        }

        repaint();
    }

    private void checkCollisions() {
        ArrayList<Projectile> toRemoveProjectiles = new ArrayList<>();
        ArrayList<Enemy> toRemoveEnemies = new ArrayList<>();

        for (Projectile projectile : projectiles) {
            for (Enemy enemy : enemies) {
                if (projectile.getBounds().intersects(enemy.getBounds())) {
                    toRemoveProjectiles.add(projectile);
                    toRemoveEnemies.add(enemy);
                    score += 10; // Incrementar puntaje al destruir un enemigo
                }
            }
        }

        projectiles.removeAll(toRemoveProjectiles);
        enemies.removeAll(toRemoveEnemies);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (gameState == GameState.MENU && e.getKeyCode() == KeyEvent.VK_ENTER) {
            startGame();
        } else if ((gameState == GameState.WON || gameState == GameState.LOST) && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            gameState = GameState.MENU;
            repaint();
        } else if (gameState == GameState.PLAYING) {
            player.keyPressed(e);
            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                projectiles.add(new Projectile(player.getX(), player.getY()));
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (gameState == GameState.PLAYING) {
            player.keyReleased(e);
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}
}

class Player {
    private int x, y, dx;
    private Image playerImage;

    public Player(int x, int y, Image image) {
        this.x = x;
        this.y = y;
        this.playerImage = image;
    }

    public void move() {
        x += dx;
        if (x < 0) x = 0;
        if (x > 760) x = 760;
    }

    public void draw(Graphics g) {
        g.drawImage(playerImage, x - playerImage.getWidth(null) / 2, y - playerImage.getHeight(null) / 2, null);
    }

    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT) dx = -5;
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) dx = 5;
    }

    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_RIGHT) {
            dx = 0;
        }
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}

class Enemy {
    private int x, y, dx = 5;
    private double rotation = 0;

    public Enemy(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void move() {
        x += dx;
        if (x < 0 || x > 760) dx = -dx;
        rotation += 0.1;
    }

    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(Color.RED);
        g2d.translate(x + 20, y + 20);
        g2d.rotate(rotation);
        g2d.fillRect(-20, -20, 40, 40);
        g2d.rotate(-rotation);
        g2d.translate(-x - 20, -y - 20);
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, 40, 40);
    }
}

class Projectile {
    private int x, y;

    public Projectile(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void move() {
        y -= 10;
    }

    public void draw(Graphics g) {
        g.setColor(Color.YELLOW);
        g.fillRect(x + 18, y, 4, 10);
    }

    public int getY() {
        return y;
    }

    public Rectangle getBounds() {
        return new Rectangle(x + 18, y, 4, 10);
    }
}
