package ecobottle;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
public final class EcoBotGame_CPUGL {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GameFrame().setVisible(true));
    }
    private enum Kind { PRODUCER, HERBIVORE, PREDATOR }
    private static final class Creature implements Serializable {
        private static final long serialVersionUID = 1L;
        Kind kind;
        double x;
        double y;
        double energy;
        int age;
        int lifespan;
        Creature(Kind kind, double x, double y, double energy) {
            this(kind, x, y, energy, 0, defaultLifespan(kind));
        }
        Creature(Kind kind, double x, double y, double energy, int age, int lifespan) {
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.energy = energy;
            this.age = age;
            this.lifespan = lifespan;
        }
        private static int defaultLifespan(Kind kind) {
            return kind == Kind.PRODUCER ? 900 : Integer.MAX_VALUE;
        }
    }
    private static final class SimulationState implements Serializable {
        private static final long serialVersionUID = 1L;
        int light;
        int loss;
        int reproduction;
        long tick;
        List<Creature> creatures;
        List<PopulationSample> history;
    }
    private static final class PopulationSample {
        final int producers;
        final int herbivores;
        final int predators;
        PopulationSample(int producers, int herbivores, int predators) {
            this.producers = producers;
            this.herbivores = herbivores;
            this.predators = predators;
        }
    }
    private static final class Simulation {
        static final int WIDTH = 720;
        static final int HEIGHT = 620;
        static final int CHART_HEIGHT = 120;
        static final int MAX_CREATURES = 300;
        static final int TICKS_PER_SECOND = 8;
        static final double CELL_SIZE = 55;
        static final int SAMPLE_INTERVAL = 1;
        static final int MAX_SAMPLES = 2500;
        static final long AUTO_SAVE_TICK_INTERVAL = 5000;
        private final Random random = new Random();
        private final List<Creature> creatures = new ArrayList<>();
        private final PopulationSample[] history = new PopulationSample[MAX_SAMPLES];
        private int historyStart;
        private int historySize;
        private long tick;
        private int light = 55;
        private int loss = 20;
        private int reproduction = 100;
        private int speed = TICKS_PER_SECOND;
        private boolean ecologicalStability;
        private boolean paused;
        private long lastAutoSaveTick;
        synchronized void tick() {
            if (paused) return;
            tick++;
            for (Creature c : creatures) {
                c.age++;
                if (c.kind == Kind.PRODUCER) {
                    double gain = light * 0.012;
                    double stress = Math.max(0, light - 78) * 0.018;
                    double darknessPenalty = light <= 0 ? 0.75 : 0;
                    c.energy = Math.min(100, c.energy + gain - stress - darknessPenalty);
                }
                double baseLoss = c.kind == Kind.PRODUCER
                        ? 0.15 + loss * 0.025 : 0.15 + loss * 0.012;
                c.energy -= baseLoss;
            }
            for (Creature herbivore : creatures) {
                if (herbivore.kind != Kind.HERBIVORE) continue;
                Creature foodSource = nearest(herbivore, Kind.PRODUCER, 130 * 130);
                moveToward(herbivore, foodSource, 0.8);
                for (Creature producer : creatures) {
                    if (producer.kind == Kind.PRODUCER
                            && distanceSquared(herbivore, producer) < 22 * 22
                            && producer.energy > 0) {
                        double food = Math.min(1.8, producer.energy);
                        producer.energy -= food;
                        herbivore.energy = Math.min(100, herbivore.energy + food * 0.72);
                    }
                }
            }
            Map<Long, List<Creature>> herbivoreGrid = new HashMap<>();
            for (Creature c : creatures) {
                if (c.kind == Kind.HERBIVORE) {
                    herbivoreGrid.computeIfAbsent(cellKey(c.x, c.y), ignored -> new ArrayList<>()).add(c);
                }
            }
            List<Creature> hunted = new ArrayList<>();
            Set<Creature> claimedTargets = new HashSet<>();
            for (Creature predator : creatures) {
                if (predator.kind != Kind.PREDATOR) continue;
                Creature target = nearestHerbivore(predator, herbivoreGrid, claimedTargets);
                moveToward(predator, target, 1.2);
                if (target != null && distanceSquared(predator, target) < 25 * 25
                        && claimedTargets.add(target)) {
                    predator.energy = Math.min(100, predator.energy + 26);
                    hunted.add(target);
                }
            }
            creatures.removeAll(hunted);
            creatures.removeIf(c -> c.energy <= 0
                    || (c.kind == Kind.PRODUCER && c.age >= c.lifespan));
            if (ecologicalStability) {
                applyEcologicalStability();
            }
            if (creatures.size() < MAX_CREATURES) {
                List<Creature> newborns = new ArrayList<>();
                double threshold = 58 * reproduction / 100.0;
                for (Creature parent : creatures) {
                    if (creatures.size() + newborns.size() >= MAX_CREATURES) break;
                    if ((parent.kind == Kind.PRODUCER || parent.kind == Kind.HERBIVORE
                            || parent.kind == Kind.PREDATOR)
                            && parent.energy >= threshold) {
                        if (parent.kind == Kind.PRODUCER && parent.energy < 88) continue;
                        if (parent.kind == Kind.PRODUCER) {
                            int density = nearbyCount(parent, Kind.PRODUCER, 90 * 90);
                            double reproductionChance = density > 12
                                    ? Math.exp(-0.20 * (density - 12)) : 1.0;
                            if (random.nextDouble() > reproductionChance) continue;
                        }
                        parent.energy *= 0.52;
                        newborns.add(new Creature(parent.kind,
                                clamp(parent.x + random.nextDouble() * 36 - 18, 8, WIDTH - 8),
                                clamp(parent.y + random.nextDouble() * 36 - 18, 8, HEIGHT - 8),
                                parent.energy, 0, lifespanFor(parent.kind)));
                    }
                }
                creatures.addAll(newborns);
            }
            if (tick % SAMPLE_INTERVAL == 0) {
                recordSample();
            }
            if (tick - lastAutoSaveTick >= AUTO_SAVE_TICK_INTERVAL) {
                lastAutoSaveTick = tick;
                try {
                    GameFrame.writeState(Path.of("auto_save.dat"), saveState());
                } catch (IOException ignored) {
                    // Automatic saves must not interrupt the simulation.
                }
            }
        }
        private void recordSample() {
            PopulationSample sample = new PopulationSample(
                    count(Kind.PRODUCER), count(Kind.HERBIVORE), count(Kind.PREDATOR));
            int index = (historyStart + historySize) % MAX_SAMPLES;
            if (historySize == MAX_SAMPLES) {
                history[index] = sample;
                historyStart = (historyStart + 1) % MAX_SAMPLES;
            } else {
                history[index] = sample;
                historySize++;
            }
        }
        private void applyEcologicalStability() {
            int producers = count(Kind.PRODUCER);
            int herbivores = count(Kind.HERBIVORE);
            int predators = count(Kind.PREDATOR);
            addIfBelowAverage(Kind.PRODUCER, producers,
                    (herbivores + predators) / 2.0, 1.0);
            addIfBelowAverage(Kind.HERBIVORE, herbivores,
                    (producers + predators) / 2.0, 1.0);
            addIfBelowAverage(Kind.PREDATOR, predators,
                    (producers + herbivores) / 2.0, 0.75);
        }
        private void addIfBelowAverage(Kind kind, int current, double otherAverage,
                                       double probabilityMultiplier) {
            if (current >= otherAverage || creatures.size() >= MAX_CREATURES) return;
            double deficit = otherAverage - current;
            double probability = (1.0 - Math.exp(-0.08 * deficit)) * probabilityMultiplier;
            if (random.nextDouble() < probability) {
                add(kind, 1);
            }
        }
        private Creature nearestHerbivore(Creature predator, Map<Long, List<Creature>> grid,
                                          Set<Creature> claimedTargets) {
            int cellX = (int) (predator.x / CELL_SIZE);
            int cellY = (int) (predator.y / CELL_SIZE);
            Creature nearest = null;
            double best = Double.MAX_VALUE;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    List<Creature> candidates = grid.get(cellKey(cellX + dx, cellY + dy));
                    if (candidates == null) continue;
                    for (Creature candidate : candidates) {
                        if (candidate.energy <= 0 || claimedTargets.contains(candidate)) continue;
                        double distance = distanceSquared(predator, candidate);
                        if (distance < best) {
                            best = distance;
                            nearest = candidate;
                        }
                    }
                }
            }
            return nearest;
        }
        private int lifespanFor(Kind kind) {
            return kind == Kind.PRODUCER ? 700 + random.nextInt(601) : Integer.MAX_VALUE;
        }
        private Creature nearest(Creature origin, Kind kind, double maxDistanceSquared) {
            Creature nearest = null;
            double best = maxDistanceSquared;
            for (Creature candidate : creatures) {
                if (candidate.kind != kind || candidate.energy <= 0) continue;
                double distance = distanceSquared(origin, candidate);
                if (distance < best) {
                    best = distance;
                    nearest = candidate;
                }
            }
            return nearest;
        }
        private void moveToward(Creature creature, Creature target, double speed) {
            if (target == null) {
                creature.x = clamp(creature.x + (random.nextDouble() - 0.5) * speed * 4, 6, WIDTH - 6);
                creature.y = clamp(creature.y + (random.nextDouble() - 0.5) * speed * 4, 6, HEIGHT - 6);
                return;
            }
            double dx = target.x - creature.x;
            double dy = target.y - creature.y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            if (distance == 0) return;
            creature.x = clamp(creature.x + dx / distance * speed, 6, WIDTH - 6);
            creature.y = clamp(creature.y + dy / distance * speed, 6, HEIGHT - 6);
        }
        private long cellKey(double x, double y) {
            return cellKey((int) (x / CELL_SIZE), (int) (y / CELL_SIZE));
        }
        private long cellKey(int x, int y) {
            return (((long) x) << 32) ^ (y & 0xffffffffL);
        }
        private static double distanceSquared(Creature a, Creature b) {
            double dx = a.x - b.x;
            double dy = a.y - b.y;
            return dx * dx + dy * dy;
        }
        private static double clamp(double value, double min, double max) {
            return Math.max(min, Math.min(max, value));
        }
        synchronized void add(Kind kind, int amount) {
            for (int i = 0; i < amount && creatures.size() < MAX_CREATURES; i++) {
                creatures.add(new Creature(kind, 10 + random.nextDouble() * (WIDTH - 20),
                        10 + random.nextDouble() * (HEIGHT - 20), 45 + random.nextDouble() * 25,
                        0, lifespanFor(kind)));
            }
        }
        synchronized void clear() {
            creatures.clear();
            tick = 0;
            historyStart = 0;
            historySize = 0;
        }
        synchronized void setPaused(boolean paused) {
            this.paused = paused;
        }
        synchronized void setLight(int value) { light = value; }
        synchronized void setLoss(int value) { loss = value; }
        synchronized void setReproduction(int value) { reproduction = value; }
        synchronized int getLight() { return light; }
        synchronized int getLoss() { return loss; }
        synchronized int getReproduction() { return reproduction; }
        synchronized boolean isPaused() { return paused; }
        synchronized int getSpeed() { return speed; }
        synchronized void setSpeed(int value) { speed = Math.max(1, Math.min(500, value)); }
        synchronized void setEcologicalStability(boolean enabled) {
            ecologicalStability = enabled;
        }
        synchronized SimulationState saveState() {
            SimulationState state = new SimulationState();
            state.light = light;
            state.loss = loss;
            state.reproduction = reproduction;
            state.tick = tick;
            state.creatures = new ArrayList<>();
            for (Creature c : creatures) {
                state.creatures.add(new Creature(c.kind, c.x, c.y, c.energy, c.age, c.lifespan));
            }
            state.history = historySnapshot();
            return state;
        }
        synchronized void loadState(SimulationState state) {
            light = state.light;
            loss = state.loss;
            reproduction = state.reproduction;
            tick = state.tick;
            creatures.clear();
            creatures.addAll(state.creatures.subList(0, Math.min(state.creatures.size(), MAX_CREATURES)));
            historyStart = 0;
            historySize = 0;
            if (state.history != null) {
                for (PopulationSample sample : state.history) {
                    history[(historyStart + historySize) % MAX_SAMPLES] = sample;
                    historySize++;
                }
            }
        }
        synchronized int count(Kind kind) {
            int count = 0;
            for (Creature c : creatures) if (c.kind == kind) count++;
            return count;
        }
        private int nearbyCount(Creature origin, Kind kind, double radiusSquared) {
            int count = 0;
            for (Creature candidate : creatures) {
                if (candidate.kind == kind && distanceSquared(origin, candidate) <= radiusSquared) {
                    count++;
                }
            }
            return count;
        }
        synchronized List<Creature> snapshot() {
            List<Creature> result = new ArrayList<>(creatures.size());
            for (Creature c : creatures) {
                result.add(new Creature(c.kind, c.x, c.y, c.energy, c.age, c.lifespan));
            }
            return result;
        }
        synchronized long getTick() { return tick; }
        synchronized List<PopulationSample> historySnapshot() {
            List<PopulationSample> result = new ArrayList<>(historySize);
            for (int i = 0; i < historySize; i++) {
                result.add(history[(historyStart + i) % MAX_SAMPLES]);
            }
            return result;
        }
    }
    private static final class WorldPanel extends JPanel {
        private final Simulation simulation;
        private boolean fixedYAxis;
        WorldPanel(Simulation simulation) {
            this.simulation = simulation;
            setPreferredSize(new Dimension(Simulation.WIDTH, Simulation.HEIGHT));
            setBackground(new Color(235, 248, 232));
        }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(90, 135, 80));
            g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            for (Creature c : simulation.snapshot()) {
                int x = (int) c.x;
                int y = (int) c.y;
                if (c.kind == Kind.PRODUCER) {
                    g.setColor(new Color(48, 156, 67));
                    g.fillRect(x - 6, y - 6, 12, 12);
                } else if (c.kind == Kind.HERBIVORE) {
                    g.setColor(new Color(235, 190, 35));
                    g.fillOval(x - 5, y - 5, 10, 10);
                } else {
                    g.setColor(new Color(204, 55, 55));
                    int[] xs = {x, x - 7, x + 7};
                    int[] ys = {y - 8, y + 6, y + 6};
                    g.fillPolygon(xs, ys, 3);
                }
            }
            drawPopulationChart(g, simulation.historySnapshot());
            g.dispose();
        }
        private void drawPopulationChart(Graphics2D g, List<PopulationSample> samples) {
            int chartTop = getHeight() - Simulation.CHART_HEIGHT;
            g.setColor(new Color(248, 248, 248, 235));
            g.fillRect(0, chartTop, getWidth(), Simulation.CHART_HEIGHT);
            g.setColor(new Color(150, 150, 150));
            g.drawLine(0, chartTop, getWidth(), chartTop);
            g.setColor(new Color(110, 110, 110));
            g.drawString("种群历史（每1 tick采样，最多2500点）", 10, chartTop + 16);
            g.setColor(new Color(40, 150, 60));
            g.drawString("绿=生产者", 270, chartTop + 16);
            g.setColor(new Color(220, 175, 20));
            g.drawString("黄=食草", 360, chartTop + 16);
            g.setColor(new Color(200, 50, 50));
            g.drawString("红=捕食", 430, chartTop + 16);
            if (samples.size() < 2) return;
            int plotTop = chartTop + 22;
            int plotHeight = Simulation.CHART_HEIGHT - 30;
            int max = fixedYAxis ? Simulation.MAX_CREATURES : 1;
            if (!fixedYAxis) {
                for (PopulationSample sample : samples) {
                    max = Math.max(max, Math.max(sample.producers,
                            Math.max(sample.herbivores, sample.predators)));
                }
            }
            drawSeries(g, samples, plotTop, plotHeight, max, 0);
            drawSeries(g, samples, plotTop, plotHeight, max, 1);
            drawSeries(g, samples, plotTop, plotHeight, max, 2);
        }
        void setFixedYAxis(boolean fixedYAxis) {
            this.fixedYAxis = fixedYAxis;
            repaint();
        }
        private void drawSeries(Graphics2D g, List<PopulationSample> samples, int top,
                                int height, int max, int kind) {
            Color color = kind == 0 ? new Color(40, 150, 60)
                    : kind == 1 ? new Color(220, 175, 20) : new Color(200, 50, 50);
            g.setColor(color);
            int previousX = 0;
            int previousY = pointY(samples.get(0), kind, top, height, max);
            for (int i = 1; i < samples.size(); i++) {
                int x = (int) ((long) i * (getWidth() - 1) / (samples.size() - 1));
                int y = pointY(samples.get(i), kind, top, height, max);
                g.drawLine(previousX, previousY, x, y);
                previousX = x;
                previousY = y;
            }
        }
        private int pointY(PopulationSample sample, int kind, int top, int height, int max) {
            int value = kind == 0 ? sample.producers
                    : kind == 1 ? sample.herbivores : sample.predators;
            return top + height - (int) ((long) value * height / max);
        }
    }
    private static final class GameFrame extends JFrame {
        private final Simulation simulation = new Simulation();
        private final WorldPanel worldPanel = new WorldPanel(simulation);
        private final JLabel stats = new JLabel();
        private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        private final Timer repaintTimer;
        private final Timer tpsTimer;
        private ScheduledFuture<?> tickTask;
        private long tpsSampleTick;
        private long tpsSampleTimeNanos;
        private double currentTps;
        private boolean speedConfirmationShowing;
        GameFrame() {
            super("微型生态瓶模拟器");
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setLayout(new BorderLayout(8, 8));
            add(worldPanel, BorderLayout.CENTER);
            add(createControls(), BorderLayout.EAST);
            pack();
            setLocationByPlatform(true);
            repaintTimer = new Timer(33, ignored -> {
                worldPanel.repaint();
                updateStats();
            });
            tpsSampleTimeNanos = System.nanoTime();
            tpsTimer = new Timer(200, ignored -> updateTps());
            repaintTimer.start();
            tpsTimer.start();
            scheduleTicks();
            addWindowListener(new WindowAdapter() {
                @Override public void windowClosed(WindowEvent event) {
                    repaintTimer.stop();
                    tpsTimer.stop();
                    executor.shutdownNow();
                }
            });
        }
        private void runTickSafely() {
            try {
                simulation.tick();
            } catch (RuntimeException ex) {
                simulation.setPaused(true);
                SwingUtilities.invokeLater(() -> showError("模拟已暂停，Tick 执行失败: " + ex.getMessage()));
            }
        }
        private void scheduleTicks() {
            ScheduledFuture<?> oldTask = tickTask;
            tickTask = null;
            if (oldTask != null) oldTask.cancel(false);
            long interval = Math.max(1, 1000L / simulation.getSpeed());
            tickTask = executor.scheduleAtFixedRate(this::runTickSafely, 0, interval,
                    TimeUnit.MILLISECONDS);
        }
        private JPanel createControls() {
            JPanel panel = new JPanel();
            panel.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 10));
            panel.setPreferredSize(new Dimension(230, 620));
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.add(new JLabel("环境参数"));
            panel.add(Box.createVerticalStrut(8));
            JSlider light = addSlider(panel, "光照强度", 0, 100, simulation.getLight());
            JSlider loss = addSlider(panel, "环境损耗", 0, 100, simulation.getLoss());
            JSlider reproduction = addSlider(panel, "繁殖阈值倍率", 25, 200, simulation.getReproduction());
            JSlider speed = addSlider(panel, "模拟速度", 1, 500, simulation.getSpeed());
            light.addChangeListener(e -> simulation.setLight(light.getValue()));
            loss.addChangeListener(e -> simulation.setLoss(loss.getValue()));
            reproduction.addChangeListener(e -> simulation.setReproduction(reproduction.getValue()));
            speed.addChangeListener(e -> {
                if (speed.getValueIsAdjusting() || speedConfirmationShowing) return;
                int selectedSpeed = speed.getValue();
                if (selectedSpeed > 100) {
                    confirmHighSpeed(speed, selectedSpeed);
                } else {
                    simulation.setSpeed(selectedSpeed);
                    scheduleTicks();
                }
            });
            panel.add(Box.createVerticalStrut(8));
            JPanel addButtons = new JPanel(new GridLayout(3, 1, 4, 4));
            addButtons.add(button("投放生产者", () -> simulation.add(Kind.PRODUCER, 5)));
            addButtons.add(button("投放食草者", this::addHerbivores));
            addButtons.add(button("投放捕食者", () -> simulation.add(Kind.PREDATOR, 2)));
            panel.add(addButtons);
            panel.add(Box.createVerticalStrut(6));
            panel.add(button("清除全部生物", simulation::clear));
            panel.add(button("暂停 / 继续", () -> simulation.setPaused(!simulation.isPaused())));
            panel.add(Box.createVerticalStrut(6));
            panel.add(button("保存存档", this::save));
            panel.add(button("读取存档", this::load));
            panel.add(button("导出 CSV", this::exportCsv));
            JCheckBox fixedYAxis = new JCheckBox("固定 Y 轴（0-300）");
            fixedYAxis.addActionListener(e -> worldPanel.setFixedYAxis(fixedYAxis.isSelected()));
            panel.add(fixedYAxis);
            JCheckBox stability = new JCheckBox("生态维稳（按种群差距补充）");
            stability.addActionListener(e -> simulation.setEcologicalStability(stability.isSelected()));
            panel.add(stability);
            panel.add(Box.createVerticalGlue());
            stats.setVerticalAlignment(SwingConstants.TOP);
            panel.add(stats);
            JLabel credit = new JLabel("XGstudio.Co@2026_XG/CL/DB");
            credit.setAlignmentX(RIGHT_ALIGNMENT);
            credit.setForeground(new Color(110, 110, 110));
            panel.add(credit);
            updateStats();
            return panel;
        }
        private JSlider addSlider(JPanel parent, String title, int min, int max, int value) {
            JLabel label = new JLabel();
            JSlider slider = new JSlider(min, max, value);
            slider.setAlignmentX(LEFT_ALIGNMENT);
            slider.addChangeListener(e -> label.setText(title + ": " + slider.getValue()
                    + (title.equals("繁殖阈值倍率") ? "%" : "")));
            label.setText(title + ": " + value + (title.equals("繁殖阈值倍率") ? "%" : ""));
            parent.add(label);
            parent.add(slider);
            return slider;
        }
        private JButton button(String text, Runnable action) {
            JButton button = new JButton(text);
            button.setAlignmentX(LEFT_ALIGNMENT);
            button.addActionListener(e -> action.run());
            return button;
        }
        private void addHerbivores() {
            boolean empty = simulation.count(Kind.HERBIVORE) == 0;
            simulation.add(Kind.HERBIVORE, 5);
            if (empty) {
                javax.swing.JOptionPane.showMessageDialog(this,
                        "当前没有食草生物，生产者可能泛滥，建议继续投放一些食草者。",
                        "生态提示", javax.swing.JOptionPane.WARNING_MESSAGE);
            }
        }
        private void updateStats() {
            stats.setText("<html>生产者: " + simulation.count(Kind.PRODUCER)
                    + "<br>食草者: " + simulation.count(Kind.HERBIVORE)
                    + "<br>捕食者: " + simulation.count(Kind.PREDATOR)
                    + "<br>总数上限: " + Simulation.MAX_CREATURES
                    + "<br>Tick: " + simulation.getTick()
                    + "<br>TPS: " + String.format(java.util.Locale.ROOT, "%.1f", currentTps)
                    + (simulation.isPaused() ? "<br><b>已暂停</b>" : "") + "</html>");
        }
        private void updateTps() {
            long now = System.nanoTime();
            long currentTick = simulation.getTick();
            long elapsed = now - tpsSampleTimeNanos;
            if (elapsed > 0) {
                currentTps = (currentTick - tpsSampleTick) * 1_000_000_000.0 / elapsed;
            }
            tpsSampleTick = currentTick;
            tpsSampleTimeNanos = now;
            updateStats();
        }
        private void confirmHighSpeed(JSlider speedSlider, int selectedSpeed) {
            boolean wasPaused = simulation.isPaused();
            simulation.setPaused(true);
            speedConfirmationShowing = true;
            int choice = javax.swing.JOptionPane.showOptionDialog(this,
                    "你确定要将Tick速度修改至100Tick/s以上吗，这可能会大大增加你的CPU负载",
                    "高速模拟确认",
                    javax.swing.JOptionPane.DEFAULT_OPTION,
                    javax.swing.JOptionPane.WARNING_MESSAGE,
                    null,
                    new Object[] {"是", "否"},
                    "否");
            int acceptedSpeed = choice == 0 ? selectedSpeed : 100;
            simulation.setSpeed(acceptedSpeed);
            speedSlider.setValue(acceptedSpeed);
            speedConfirmationShowing = false;
            scheduleTicks();
            if (wasPaused) simulation.setPaused(true);
        }
        private void save() {
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new java.io.File("eco-bottle.save"));
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            try {
                writeState(chooser.getSelectedFile().toPath(), simulation.saveState());
            } catch (IOException ex) {
                showError("保存失败: " + ex.getMessage());
            }
        }
        private void load() {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
            try {
                simulation.loadState(readState(chooser.getSelectedFile().toPath()));
            } catch (IOException ex) {
                showError("读取失败: " + ex.getMessage());
            }
        }
        private void exportCsv() {
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new java.io.File("population-history.csv"));
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            try (BufferedWriter writer = Files.newBufferedWriter(chooser.getSelectedFile().toPath())) {
                writer.write("sample,producers,herbivores,predators");
                writer.newLine();
                int index = 0;
                for (PopulationSample sample : simulation.historySnapshot()) {
                    writer.write(index++ + "," + sample.producers + "," + sample.herbivores + ","
                            + sample.predators);
                    writer.newLine();
                }
                javax.swing.JOptionPane.showMessageDialog(this, "CSV 导出完成！");
            } catch (IOException ex) {
                showError("CSV 导出失败: " + ex.getMessage());
            }
        }
        private static void writeState(Path path, SimulationState state) throws IOException {
            try (DataOutputStream out = new DataOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(path)))) {
                out.writeInt(3);
                out.writeInt(state.light);
                out.writeInt(state.loss);
                out.writeInt(state.reproduction);
                out.writeLong(state.tick);
                out.writeInt(state.creatures.size());
                for (Creature c : state.creatures) {
                    out.writeByte(c.kind.ordinal());
                    out.writeDouble(c.x);
                    out.writeDouble(c.y);
                    out.writeDouble(c.energy);
                    out.writeInt(c.age);
                    out.writeInt(c.lifespan);
                }
                List<PopulationSample> history = state.history == null
                        ? new ArrayList<>() : state.history;
                out.writeInt(history.size());
                for (PopulationSample sample : history) {
                    out.writeInt(sample.producers);
                    out.writeInt(sample.herbivores);
                    out.writeInt(sample.predators);
                }
            }
        }
        private static SimulationState readState(Path path) throws IOException {
            try (DataInputStream in = new DataInputStream(
                    new BufferedInputStream(Files.newInputStream(path)))) {
                int version = in.readInt();
                if (version != 1 && version != 2 && version != 3) {
                    throw new IOException("不支持的存档版本");
                }
                SimulationState state = new SimulationState();
                state.light = requireRange(in.readInt(), 0, 100);
                state.loss = requireRange(in.readInt(), 0, 100);
                state.reproduction = requireRange(in.readInt(), 25, 200);
                state.tick = in.readLong();
                int size = requireRange(in.readInt(), 0, Simulation.MAX_CREATURES);
                state.creatures = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    int ordinal = in.readUnsignedByte();
                    if (ordinal >= Kind.values().length) throw new IOException("存档包含未知物种");
                    Kind kind = Kind.values()[ordinal];
                    double x = in.readDouble();
                    double y = in.readDouble();
                    double energy = in.readDouble();
                    int age = version >= 3 ? requireRange(in.readInt(), 0, Integer.MAX_VALUE) : 0;
                    int lifespan = version >= 3
                            ? requireRange(in.readInt(), 1, Integer.MAX_VALUE)
                            : Creature.defaultLifespan(kind);
                    state.creatures.add(new Creature(kind, x, y, energy, age, lifespan));
                }
                state.history = new ArrayList<>();
                if (version >= 2) {
                    int historySize = requireRange(in.readInt(), 0, Simulation.MAX_SAMPLES);
                    for (int i = 0; i < historySize; i++) {
                        state.history.add(new PopulationSample(in.readInt(), in.readInt(), in.readInt()));
                    }
                }
                return state;
            }
        }
        private static int requireRange(int value, int min, int max) throws IOException {
            if (value < min || value > max) throw new IOException("存档数据超出范围");
            return value;
        }
        private void showError(String message) {
            javax.swing.JOptionPane.showMessageDialog(this, message, "错误",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }
}


