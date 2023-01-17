package others;
/**
 * Adapted by Jose Rogado on 05-01-2016.
 */

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

import static java.lang.Runtime.getRuntime;

/**
 * Mandelbrot paralelizado com técnica divide and conquer
 * @author Thiago Henrique dos Santos
 */
public class MandelbrotTaskPool extends JFrame {

    private static final int MAXITERATIONS = 10000;
    // Image Dimensions
    private static int sizeX = 2048;
    private static int sizeY = 2048;
    final private BufferedImage image;

    // Visible zone coordinates
    private static final double CxMin = -2.0;
    private static final double CxMax = 0.5;
    private static final double CyMin = -1.25;
    private static final double CyMax = 1.25;

    private static int threshold;
    private final ForkJoinPool forkJoinPool;

    public MandelbrotTaskPool()  {
        super("Mandelbrot Divide and Conquer");


        //setup maximized screen
        final GraphicsConfiguration config = getGraphicsConfiguration();

        final int left = Toolkit.getDefaultToolkit().getScreenInsets(config).left;
        final int right = Toolkit.getDefaultToolkit().getScreenInsets(config).right;
        final int top = Toolkit.getDefaultToolkit().getScreenInsets(config).top;
        final int bottom = Toolkit.getDefaultToolkit().getScreenInsets(config).bottom;

        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        final int width = screenSize.width - left - right;
        final int height = screenSize.height - top - bottom;

        setBounds(0, 0, width, height);
        setResizable(true);
        setExtendedState(Frame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        //setup scroll image panel
        JScrollPane imageScrollPane = new JScrollPane(new ImagePanel(),JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        getContentPane().add(imageScrollPane);


        //setup threshold
        int nCores = getRuntime().availableProcessors();

        threshold = getHeight() / (10 * nCores);
        int maxDepth = (int) (Math.log(nCores) / Math.log(2)) + 5;

        System.out.println("Number of cores: "+nCores +" Threshold: " + threshold + " Max depth: " + maxDepth);

        //generate image
        image = new BufferedImage(sizeX, sizeY, BufferedImage.TYPE_INT_RGB);

        System.out.println("Generating fractal of " +sizeX + " x "
                + sizeY + " pixels with " + MAXITERATIONS + " iterations");
        System.out.println("Limits: x0, x1: " + CxMin + " , " + CxMax);
        System.out.println("Limits: y0, y1: " + CyMin + " , " + CyMax);

        forkJoinPool = new ForkJoinPool();
        long startTime = System.currentTimeMillis();
        forkJoinPool.invoke(new MandelParallel(0,sizeY,sizeX,image));

        long stopTime = System.currentTimeMillis();
        System.out.println("Generating a fractal of "
                + getWidth() + " by " + getHeight() + " took " + (stopTime - startTime)
                + " ms");

    }

    //panel to scroll the image
    public class ImagePanel extends JPanel {

        public ImagePanel() {
            setVisible(true);
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.drawImage(image, 0, 0, this);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(image.getWidth(), image.getHeight());
        }
    }


    //mandel divide and conquer class
    static class MandelParallel extends RecursiveAction {
        private int index_y;
        private int width;
        private int heigth;
        double pixelWidth = (CxMax - CxMin) / sizeX;
        double pixelHeight = (CyMax - CyMin) / sizeY;


        private BufferedImage myImage;

        MandelParallel(int index_y,int heigth, int width, BufferedImage myImage){
            this.index_y = index_y;
            this.width = width;
            this.heigth = heigth;
            this.myImage = myImage;

        }

        @Override
        protected void compute() {

            if (heigth < threshold){
                for(int py = index_y;py < index_y+heigth; py++){
                    double cY = CyMin + py * pixelHeight;
                    for (int px = 0;px < width; px++){
                        double cX = CxMin + px * pixelWidth;
                        int iter = iterateMandel(cX, cY, MAXITERATIONS);
                        myImage.setRGB(px, py, iter | (iter << 8));

                    }
                }
            }else{
                invokeAll( new MandelParallel(index_y + heigth/2, heigth/2,width, myImage),
                        new MandelParallel(index_y,heigth - heigth/2,width, myImage));
            }
        }
    }


    public static int iterateMandel( double creal, double cimag, int maxIter) {
        double real = creal;
        double imag = cimag;
        int n = maxIter;

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

    public static void main(String[] args)  {
        new MandelbrotTaskPool().setVisible(true);
    }
}
