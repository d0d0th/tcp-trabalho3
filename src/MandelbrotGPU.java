import org.jocl.*;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.jocl.CL.*;
import static org.jocl.CL.clCreateCommandQueue;


/**
 * Mandelbrot sem parelização
 * @author  Thiago Henrique dos Santos
 */
public class MandelbrotGPU extends JFrame {

    private static final int MAXITERATIONS = 10000;
    // Image Dimensions
    private static int sizeX = 4096;
    private static int sizeY = 4096;
    final private BufferedImage image;

    // Visible zone coordinates
    private static final float CxMin = -2.0f;
    private static final float CxMax = 0.5f;
    private static final float CyMin = -1.25f;
    private static final float CyMax = 1.25f;

    // Pixel size
    float pixelWidth = (CxMax - CxMin) / sizeX;
    float pixelHeight = (CyMax - CyMin) / sizeY;

    //opencl objects
    private cl_context context;
    private cl_command_queue commandQueue;
    private cl_kernel kernel;

    //shared memory
    private cl_mem imagePixelsMem;

    private final String sourceFile = "MandelbrotKernel.cl";

    public MandelbrotGPU() throws IOException {
        super("Mandelbrot Set");

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

        //generate image
        image = new BufferedImage(sizeX, sizeY, BufferedImage.TYPE_INT_RGB);

        //config opencl
        final int platformIndex = 0;
        final long deviceType = CL_DEVICE_TYPE_ALL;
        final int deviceIndex = 0;
        CL.setExceptionsEnabled(true);

        // Obtain the number of platforms
        int numPlatformsArray[] = new int[1];
        clGetPlatformIDs(0, null, numPlatformsArray);
        int numPlatforms = numPlatformsArray[0];

        // Obtain a platform ID
        cl_platform_id platforms[] = new cl_platform_id[numPlatforms];
        clGetPlatformIDs(platforms.length, platforms, null);
        cl_platform_id platform = platforms[platformIndex];

        // Initialize the context properties
        cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);

        // Obtain the number of devices for the platform
        int numDevicesArray[] = new int[1];
        clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
        int numDevices = numDevicesArray[0];

        // Obtain a device ID
        cl_device_id devices[] = new cl_device_id[numDevices];
        clGetDeviceIDs(platform, deviceType, numDevices, devices, null);
        cl_device_id device = devices[deviceIndex];

        // Create a context for the selected device
        context = clCreateContext(
                contextProperties, 1, new cl_device_id[]{device},
                null, null, null);

        // Create a command-queue for the selected device
        commandQueue =
                clCreateCommandQueue(context, device, 0, null);


        //reading source file
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(sourceFile)));
        StringBuffer sb = new StringBuffer();
        while (true)
        {
            String line = br.readLine();
            if (line == null)
            {
                break;
            }
            sb.append(line).append("\n");
        }
        String source = sb.toString();

        // Create the program
        cl_program cpProgram = clCreateProgramWithSource(context, 1,
                new String[]{ source }, null, null);

        // Build the program
        clBuildProgram(cpProgram, 0, null, "-cl-mad-enable", null, null);

        // Create the kernel
        kernel = clCreateKernel(cpProgram, "mandelbrotKernel", null);

        imagePixelsMem = clCreateBuffer(context, CL_MEM_WRITE_ONLY,
                sizeX * sizeY * Sizeof.cl_uint, null, null);

        long globalWorkSize[] = new long[2];
        globalWorkSize[0] = sizeX;
        globalWorkSize[1] = sizeY;

        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(imagePixelsMem));
        clSetKernelArg(kernel, 1, Sizeof.cl_uint, Pointer.to(new int[]{sizeX}));
        clSetKernelArg(kernel, 2, Sizeof.cl_uint, Pointer.to(new int[]{sizeY}));
        clSetKernelArg(kernel, 3, Sizeof.cl_float, Pointer.to(new float[]{ CyMin }));
        clSetKernelArg(kernel, 4, Sizeof.cl_float, Pointer.to(new float[]{ CxMin}));
        clSetKernelArg(kernel, 5, Sizeof.cl_float, Pointer.to(new float[]{ pixelWidth }));
        clSetKernelArg(kernel, 6, Sizeof.cl_float, Pointer.to(new float[]{ pixelHeight }));
        clSetKernelArg(kernel, 7, Sizeof.cl_int, Pointer.to(new int[]{ MAXITERATIONS }));

        System.out.println("Generating fractal of " + sizeX + " x "
                + sizeY + " pixels with " + MAXITERATIONS + " iterations");
        System.out.println("Limits: x0, x1: " + CxMin + " , " + CxMax);
        System.out.println("Limits: y0, y1: " + CyMin + " , " + CyMax);
        long startTime = System.currentTimeMillis();

        clEnqueueNDRangeKernel(commandQueue, kernel, 2, null,
                globalWorkSize, null, 0, null, null);

        // Read the pixel data into the BufferedImage
        DataBufferInt dataBuffer = (DataBufferInt)image.getRaster().getDataBuffer();
        int data[] = dataBuffer.getData();
        clEnqueueReadBuffer(commandQueue, imagePixelsMem, CL_TRUE, 0,
                Sizeof.cl_int * sizeY * sizeX, Pointer.to(data), 0, null, null);


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


    public static void main(String[] args) throws IOException {
        new MandelbrotGPU().setVisible(true);
    }
}
