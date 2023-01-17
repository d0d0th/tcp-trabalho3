package others;
/**
 * Adapted by Jose Rogado on 05-01-2016.
 */

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class mandelbrot8Threads extends JFrame {

    final private BufferedImage image;

    final private int nThreads = 8;

    // Image Dimensions
    private static int sizeX = 4096;
    private static int sizeY = 4096;
    // Visible zone coordinates
    private static final double CxMin = -2.0;
    private static final double CxMax = 0.5;
    private static final double CyMin = -1.25;
    private static final double CyMax = 1.25;
    private static final int MAXITERATIONS = 10000;
    // Pixel size
    double pixelWidth = (CxMax - CxMin) / sizeX;
    double pixelHeight = (CyMax - CyMin) / sizeY;

    static class ThreadMandelbrotSimple extends Thread {

        BufferedImage image;
        double pixelWidth;
        double pixelHeight;
        int startHeight;
        int endHeight;
        int width;

        ThreadMandelbrotSimple(BufferedImage image,double pixelWidth,double pixelHeight,int startHeight,
                               int endHeight,int width){
            this.image = image;
            this.pixelHeight = pixelHeight;
            this.pixelWidth = pixelWidth;
            this.startHeight = startHeight;
            this.endHeight = endHeight;
            this.width = width;
        }

        @Override
        public void run(){
            for (int py = startHeight; py < endHeight; py++) {
                double cY = CyMin + py * pixelHeight;
                for (int px = 0; px < width; px++) {
                    double cX = CxMin + px * pixelWidth;
                    int iter = iterateMandel(cX, cY, MAXITERATIONS);
                    image.setRGB(px, py, iter | (iter << 8));
                }
            }
        }

        public static int iterateMandel(double creal, double cimag, int maxiter) {
            double real = creal;
            double imag = cimag;
            int n = maxiter;
            while (n > 0) {
                double real2 = real * real;
                double imag2 = imag * imag;
                if (real2 + imag2 > 4.0)
                    // Diverged after n iterations: set color pixel
                    return n;
                // break;
                imag = 2 * real * imag + cimag;
                real = real2 - imag2 + creal;
                n--;
            }
            // Cconverge after maxiter interations: set black pixel
            return 0;

        }

    }
    public mandelbrot8Threads() throws InterruptedException {
        super("Mandelbrot Set");
        // double itX, itY, itX2, itY2;

        setBounds(0, 0, sizeX, sizeY);
        setResizable(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        System.out.println("Generating fractal of " + getWidth() + " x "
                + getHeight() + " pixels with " + MAXITERATIONS + " iterations");
        System.out.println("Limits: x0, x1: " + CxMin + " , " + CxMax);
        System.out.println("Limits: y0, y1: " + CyMin + " , " + CyMax);
        long startTime = System.currentTimeMillis();
        /**
         *
         */
        ThreadMandelbrotSimple[] threadList = new ThreadMandelbrotSimple[nThreads];

        for(int i= 0;i<nThreads;i++){
            int start = i*getHeight()/nThreads;
            int end = start + getHeight()/nThreads;
            threadList[i] = new ThreadMandelbrotSimple(image,pixelWidth,pixelHeight,start, end,getWidth());
            threadList[i].start();
        }

        for (int i = 0; i < nThreads; i++)
        {
            threadList[i].join();
        }
        long stopTime = System.currentTimeMillis();
        System.out.println("Generating a fractal of "
                + getWidth() + " by " + getHeight() + " took " + (stopTime - startTime)
                + " ms");

    }



    @Override
    public void paint(Graphics g) {
        g.drawImage(image, 0, 0, this);
    }

    public static void main(String[] args) throws InterruptedException {
        new mandelbrot8Threads().setVisible(true);
    }
}
