package org.genedb.web.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ContextMapWindowServlet extends HttpServlet {
    private static final int HEIGHT = 11;
    
    private static final String FORMAT = "gif";
    private static final String MIME_TYPE = "image/gif";
    
    private static final Color FRAME_COLOR = new Color(0, 0, 200, 255);
    
    private IndexColorModel colorModelFor(Color... colors) {
        int len = 1 + colors.length;
        int bits = 1, twoToBits = 2;
        while (twoToBits < len) {
            bits++;
            twoToBits *= 2;
        }
        
        byte[] reds   = new byte[len];
        byte[] greens = new byte[len];
        byte[] blues  = new byte[len];
        byte[] alphas = new byte[len];
        
        reds[0] = greens[0] = blues[0] = alphas[0] = 0; // transparent "colour"
        for (int i=1; i < len; i++) {
            reds[i]   = (byte) colors[i-1].getRed();
            greens[i] = (byte) colors[i-1].getGreen();
            blues[i]  = (byte) colors[i-1].getBlue();
            alphas[i] = (byte) colors[i-1].getAlpha();
        }

        return new IndexColorModel(bits, 1 + colors.length, reds, greens, blues, alphas);
    }
    
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType(MIME_TYPE);
        int width = Integer.parseInt(req.getParameter("width"));
        
        OutputStream out = resp.getOutputStream();
        
        /*
         * IE6 doesn't deal well with PNG alpha, so we generate a GIF89a
         * image. In order for ImageIO to produce the correct result (with
         * transparent pixels) we need to use an explicit indexed colour
         * model. For PNG we could just use BufferedImage.TYPE_INT_ARGB
         * and avoid this messiness.
         */
        BufferedImage image = new BufferedImage(width + 4, HEIGHT + 4,
            BufferedImage.TYPE_BYTE_INDEXED, colorModelFor(Color.WHITE, FRAME_COLOR));
        Graphics2D graf = (Graphics2D) image.getGraphics();

        graf.setColor(FRAME_COLOR);
        graf.drawRect(1, 1, width+1, HEIGHT+1);
        
        graf.setColor(Color.WHITE);
        graf.drawRect(0, 0, width+3, HEIGHT+3);
        
        ImageIO.write(image, FORMAT, out);
        graf.dispose();
    }
}
