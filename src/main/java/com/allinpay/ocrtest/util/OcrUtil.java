package com.allinpay.ocrtest.util;

import net.sourceforge.tess4j.Tesseract;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
public class OcrUtil {

    private static final Logger logger = LoggerFactory.getLogger(OcrUtil.class);

    // 在类中添加静态初始化块
    static {
        // 加载OpenCV库
        nu.pattern.OpenCV.loadShared();
        System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
    }

    public List<String> printPDF(String filePath) {
        File file = new File(filePath);
        try(PDDocument document = PDDocument.load(file)) {

            // 创建Tesseract实例
            Tesseract tesseract = new Tesseract();
            tesseract.setTessVariable("user_defined_dpi", "96");
            tesseract.setDatapath("src/main/resources/tessdata");  // 设置tessdata目录路径
            tesseract.setLanguage("chi_sim");   // 设置中文简体

            List<String> result = new ArrayList<>();
            PDFRenderer renderer = new PDFRenderer(document);

            // 遍历每一页
            for (int page = 0; page < document.getNumberOfPages(); page++) {
                logger.info("Processing page {}", page);
                // 将PDF页面渲染为图片
                BufferedImage image = renderer.renderImageWithDPI(page, 300);
                // 去除红色图章
                image = preprocessImage(image);
                // 使用Tesseract识别图片中的文字
                String text = tesseract.doOCR(image).replace(" ", "").replace("\n\n", "\n");
                result.add(text);
            }

            document.close();
            return result;
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        return null;
    }

    private BufferedImage preprocessImage(BufferedImage image) {
        try {
            Mat src = bufferedImageToMat(image);
            List<Mat> channels = new ArrayList<>();
            Core.split(src, channels);
            Mat redChannel = channels.get(2);  // BGR格式下，索引2是红色通道

            // 1. 对R通道进行自适应阈值二值化
            Mat binary = new Mat();
            Imgproc.adaptiveThreshold(redChannel, binary, 255,
                    Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                    Imgproc.THRESH_BINARY, 11, 2);

            // 2.中值滤波
            Mat denoised = new Mat();
            Imgproc.medianBlur(binary, denoised, 3);

            // 3.高斯模糊降噪
            Imgproc.GaussianBlur(denoised, denoised, new Size(3, 3), 0);

            // 4.双边滤波
            Mat result = new Mat();
            Imgproc.bilateralFilter(denoised, result, 9, 75, 75);

            return matToBufferedImage(result);
        } catch (Exception e) {
            logger.error("图像预处理失败: ", e);
            return image;
        }
    }

    // BufferedImage转Mat
    private Mat bufferedImageToMat(BufferedImage bi) {
        // 将BufferedImage转换为TYPE_3BYTE_BGR格式
        BufferedImage convertedImage = new BufferedImage(
                bi.getWidth(),
                bi.getHeight(),
                BufferedImage.TYPE_3BYTE_BGR
        );
        convertedImage.getGraphics().drawImage(bi, 0, 0, null);

        // 获取像素数据
        byte[] pixels = ((DataBufferByte) convertedImage.getRaster().getDataBuffer()).getData();

        // 创建Mat对象
        Mat mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
        mat.put(0, 0, pixels);

        return mat;
    }

    // Mat转BufferedImage
    private BufferedImage matToBufferedImage(Mat mat) {
        BufferedImage image = new BufferedImage(mat.width(), mat.height(),
                mat.channels() == 1 ? BufferedImage.TYPE_BYTE_GRAY : BufferedImage.TYPE_3BYTE_BGR);
        byte[] data = new byte[mat.width() * mat.height() * (int)mat.elemSize()];
        mat.get(0, 0, data);
        image.getRaster().setDataElements(0, 0, mat.width(), mat.height(), data);
        return image;
    }
}