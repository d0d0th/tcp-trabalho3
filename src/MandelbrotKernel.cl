/*
 * Thiago Santos
 */
__kernel void mandelbrotKernel(
    __global uint *output,
    int sizeX,
    int sizeY,
    float cyMin,
    float cxMin,
    float pixelWidth,
    float pixelHeight,
    int maxIterations
    )
{
    unsigned int ix = get_global_id(0);
    unsigned int iy = get_global_id(1);

    float creal = cxMin + ix * pixelWidth;
    float cimag = cyMin + iy * pixelHeight;
    float real = creal;
    float imag = cimag;
    int n = maxIterations;

    while (n > 0) {
        double real2 = real * real;
        double imag2 = imag * imag;
        if (real2 + imag2 > 4.0){
            break;
        }
        imag = 2 * real * imag + cimag;
        real = real2 - imag2 + creal;
        n--;
    }
    output[iy*sizeX+ix] = n | (n << 8);

}