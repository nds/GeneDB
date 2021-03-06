package org.genedb.jogra.drawing;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.WindowConstants;

/**
 * Simple ProgressBar that pops up on the screen to inform the user that a long task 
 * is running.
 * @author nds
 *
 */

public class JograProgressBar extends JPanel  {

    private JProgressBar progressBar;
    private JFrame frame;
    static enum Position { 
        CENTRE (2,2), 
        TOP_LEFT (3,3);
        
        private final int x;
        private final int y;
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension size = tk.getScreenSize();
        
        Position (int xdiv, int ydiv){
            this.x = ((int)size.getWidth()/xdiv)-300;
            this.y = ((int)size.getHeight()/ydiv)-70;   
        }
        
        public int x(){ return this.x; }
        public int y(){ return this.y; }
        
    }
  

    /* Constructor */
    public JograProgressBar(String title){
        this(title,Position.CENTRE); //Default is centre
    }

   /* Position and start the progress bar */
    public JograProgressBar(String title, Position pos ) {
        
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        
        JPanel panel = new JPanel();
        panel.add(progressBar);        
  
        frame = new JFrame();
        frame.setLayout(new BorderLayout());
        frame.setPreferredSize(new Dimension(300,70));
        frame.setTitle(title);
      
        frame.setLocation(pos.x, pos.y); 
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.add(panel);
        frame.pack();
        frame.setVisible(true);
 
    }
    
    /* Stop the progressbar */
    public void stop(){
        progressBar.setIndeterminate(false);
        frame.setVisible(false);
        frame.dispose();
    }
  
    /* Test */
    public static void main(String[] args) {
        JograProgressBar jpb = new JograProgressBar("Testing Progress Bar", Position.TOP_LEFT);

    }
}
