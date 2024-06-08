package br.com.opencam

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.imgproc.Imgproc
import kotlin.math.pow


object BlurValidation {
    private const val DOUBLE_PATTERN = "%.2f"
    private const val SOGLIA = -6118750
    private const val START_MAX_LAP = -16777216

    fun isBlurredImageFastDetection(image: Bitmap): Double {
        val sourceMatImage = Mat()
        val destination = Mat()
        val matGray = Mat()
        Utils.bitmapToMat(image, sourceMatImage)
        Imgproc.cvtColor(sourceMatImage, matGray, Imgproc.COLOR_BGR2GRAY)
        Imgproc.Laplacian(matGray, destination, 3)
        val median = MatOfDouble()
        val std = MatOfDouble()
        Core.meanStdDev(destination, median, std)

        val value = std.get(0, 0)[0].pow(2.0)

        return DOUBLE_PATTERN.format(value).replace(",", ".").toDouble()
    }

    fun isBlurredImageSlowlyDetection(image: Bitmap): Boolean {
        return try {
            run {
                val opt = BitmapFactory.Options()
                // opt.inDither = true
                opt.inPreferredConfig = Bitmap.Config.ARGB_8888
                val l = CvType.CV_8UC1
                val matImage = Mat()
                Utils.bitmapToMat(image, matImage)
                val matImageGrey = Mat()
                Imgproc.cvtColor(matImage, matImageGrey, Imgproc.COLOR_BGR2GRAY)
                val dst2 = Mat()
                Utils.bitmapToMat(image, dst2)
                val laplacianImage = Mat()
                dst2.convertTo(laplacianImage, l)
                Imgproc.Laplacian(matImageGrey, laplacianImage, CvType.CV_8U)
                val laplacianImage8bit = Mat()
                laplacianImage.convertTo(laplacianImage8bit, l)
                System.gc()
                val bmp = Bitmap.createBitmap(
                    laplacianImage8bit.cols(),
                    laplacianImage8bit.rows(), Bitmap.Config.ARGB_8888
                )
                Utils.matToBitmap(laplacianImage8bit, bmp)
                val pixels = IntArray(bmp.height * bmp.width)
                bmp.getPixels(
                    pixels, 0, bmp.width, 0, 0, bmp.width,
                    bmp.height
                )
                if (!bmp.isRecycled) {
                    bmp.recycle()
                }
                var maxLap = START_MAX_LAP
                for (i in pixels.indices) {
                    if (pixels[i] > maxLap) {
                        maxLap = pixels[i]
                    }
                }
                maxLap < SOGLIA || maxLap == SOGLIA
            }
        } catch (e: NullPointerException) {
            false
        } catch (e: OutOfMemoryError) {
            false
        }
    }
}
