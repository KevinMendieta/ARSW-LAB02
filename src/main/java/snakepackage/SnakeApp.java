package snakepackage;

import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JFrame;

import enums.GridSize;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * @author jd-
 *
 */
public class SnakeApp {

    private static SnakeApp app;
    public static final int MAX_THREADS = 8;
    Snake[] snakes = new Snake[MAX_THREADS];
    private static final Cell[] spawn = {
        new Cell(1, (GridSize.GRID_HEIGHT / 2) / 2),
        new Cell(GridSize.GRID_WIDTH - 2,
        3 * (GridSize.GRID_HEIGHT / 2) / 2),
        new Cell(3 * (GridSize.GRID_WIDTH / 2) / 2, 1),
        new Cell((GridSize.GRID_WIDTH / 2) / 2, GridSize.GRID_HEIGHT - 2),
        new Cell(1, 3 * (GridSize.GRID_HEIGHT / 2) / 2),
        new Cell(GridSize.GRID_WIDTH - 2, (GridSize.GRID_HEIGHT / 2) / 2),
        new Cell((GridSize.GRID_WIDTH / 2) / 2, 1),
        new Cell(3 * (GridSize.GRID_WIDTH / 2) / 2,
        GridSize.GRID_HEIGHT - 2)};
    private JFrame frame;
    private static Board board;
    int nr_selected = 0;
    Thread[] thread = new Thread[MAX_THREADS];
    private JButton startButton, pauseButton, resumeButton;
    private boolean started;
    private int deadSnakes;
    private Object locker = new Object();

    public SnakeApp() {
        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
        frame = new JFrame("The Snake Race");
        frame.setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // frame.setSize(618, 640);
        frame.setSize(GridSize.GRID_WIDTH * GridSize.WIDTH_BOX + 17,
                GridSize.GRID_HEIGHT * GridSize.HEIGH_BOX + 40);
        frame.setLocation(dimension.width / 2 - frame.getWidth() / 2,
                dimension.height / 2 - frame.getHeight() / 2);
        board = new Board();
        frame.add(board,BorderLayout.CENTER);
        JPanel actionsBPabel=new JPanel();
        actionsBPabel.setLayout(new FlowLayout());
        //Buttons
        startButton = new JButton("Start");
        pauseButton = new JButton("Pause");
        resumeButton = new JButton("Resume");
        actionsBPabel.add(startButton);
        actionsBPabel.add(pauseButton);
        actionsBPabel.add(resumeButton);
        frame.add(actionsBPabel,BorderLayout.SOUTH);
        started = false;
        deadSnakes = 0;
        prepareActions();
    }
    
    /**
     * Increase the counter of the dead snakes concurrently and sets the property of the first snake dead and calculates if all snakes are dead.
     * @param snake
     * @return return true if all snakes are dead otherwise false. 
     */
    public synchronized boolean increaseAndCheckDeadSnakes(Snake snake){
        if(deadSnakes == 0) snake.setFirstDead(true);
        deadSnakes++;
        return deadSnakes == MAX_THREADS;
    }

    /**
     * Prepare listeners for the action (starting, pausing and resuming).
     */
    public void prepareActions(){
        startButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(!started)start();
                started = true;
            }
        });
        
        pauseButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                pause();
            }
        });
        
        resumeButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                resume();
            }
        });
    }
    
    /**
     * Start all the threads.
     */
    public void start(){
        for (int i = 0; i < MAX_THREADS; i++){
            thread[i].start();
        }
    }

    /**
     * Pause all the threads.
     */
    public void pause(){
        for (int i = 0; i < MAX_THREADS; i++){
            snakes[i].lock();
        }
        showPauseInfo();
    }

    /**
     * Resume all the threads.
     */
    public void resume(){
        for (int i = 0; i < MAX_THREADS; i++){
            snakes[i].unlock();
        }
    }
    
    /**
     * Use a component to show the pausing information.
     */
    private void showPauseInfo(){
        Snake worstSnake = null;
        Snake bestSnake = snakes[0];
        for (int i = 0; i < snakes.length; i++) {
            if (snakes[i].isFirstDead()) {
                worstSnake = snakes[i];
            }
            if (snakes[i].getBody().size() > bestSnake.getBody().size()) {
                bestSnake = snakes[i];
            }
        }
        String firstMessage, secondMessage;
        if (worstSnake != null) {
            firstMessage = "The worst Snake is on " + worstSnake.getHead().getX() + ", " + worstSnake.getHead().getY() + " cell.";
        } else {
            firstMessage = "No deads yet!";
        }
        secondMessage = "The best snake is on " + bestSnake.getHead().getX() + ", " + bestSnake.getHead().getY() + " cell with " + bestSnake.getBody().size() + " length!";
        JOptionPane.showMessageDialog(null, firstMessage + "\n" + secondMessage, "InfoBox", JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * @return the locker for this class
     */
    public Object getLocker(){return locker;}

    public static void main(String[] args) {
        app = new SnakeApp();
        app.init();
    }

    private void init() {
        for (int i = 0; i != MAX_THREADS; i++) {
            snakes[i] = new Snake(i + 1, spawn[i], i + 1);
            snakes[i].addObserver(board);
            thread[i] = new Thread(snakes[i]);
            snakes[i].setLocker(this);
            //thread[i].start();
        }
        frame.setVisible(true);
        /*while (true) {
            int x = 0;
            for (int i = 0; i != MAX_THREADS; i++) {
                if (snakes[i].isSnakeEnd() == true) {
                    x++;
                }
            }
            if (x == MAX_THREADS) {
                break;
            }
        }*/
        synchronized(locker){
            try {
                locker.wait();
            } catch (InterruptedException e) {
                Logger.getLogger(SnakeApp.class.getName()).log(Level.SEVERE, null, e);
            }
        }
        System.out.println("Thread (snake) status:");
        for (int i = 0; i != MAX_THREADS; i++) {
            System.out.println("["+i+"] :"+thread[i].getState());
        }
    }

    public static SnakeApp getApp() {
        return app;
    }

}
