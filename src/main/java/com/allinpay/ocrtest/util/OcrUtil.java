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
                image = removeRedStamp(image);
                // 使用Tesseract识别图片中的文字
                String text = tesseract.doOCR(image).replace(" ", "");
                result.add(text);
            }

            document.close();
            return result;
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        return null;
    }

    /**
     * 去除图片中的红色图章（使用颜色通道分离和阈值分割）
     */
    private BufferedImage removeRedStamp(BufferedImage image) {
        try {
            // 将BufferedImage转换为3BYTE_BGR格式
            BufferedImage convertedImage = new BufferedImage(
                    image.getWidth(),
                    image.getHeight(),
                    BufferedImage.TYPE_3BYTE_BGR
            );
            convertedImage.getGraphics().drawImage(image, 0, 0, null);

            // 获取像素数据
            byte[] data = ((DataBufferByte) convertedImage.getRaster().getDataBuffer()).getData();
            Mat src = new Mat(convertedImage.getHeight(), convertedImage.getWidth(), CvType.CV_8UC3);
            src.put(0, 0, data);

            // 分离RGB通道
            List<Mat> channels = new ArrayList<>();
            Core.split(src, channels);
            Mat redChannel = channels.get(2);  // BGR格式下，索引2是红色通道
            Mat greenChannel = channels.get(1);
            Mat blueChannel = channels.get(0);

            // 计算红色与其他颜色的差异（红色 - 绿色/2 - 蓝色/2）
            Mat redMinusGreen = new Mat();
            Mat redMinusBlue = new Mat();
            Core.subtract(redChannel, greenChannel, redMinusGreen);
            Core.subtract(redChannel, blueChannel, redMinusBlue);

            // 合并差异
            Mat redDominance = new Mat();
            Core.add(redMinusGreen, redMinusBlue, redDominance);

            // 应用阈值分割
            Mat mask = new Mat();
            Imgproc.threshold(redDominance, mask, 50, 255, Imgproc.THRESH_BINARY);

            // 对掩码进行形态学操作，去除噪点
            Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
            Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_OPEN, kernel);
            Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, kernel);

            // 将红色区域替换为白色
            Mat result = src.clone();
            for (int i = 0; i < result.rows(); i++) {
                for (int j = 0; j < result.cols(); j++) {
                    if (mask.get(i, j)[0] > 0) {
                        result.put(i, j, 255, 255, 255); // 替换为白色
                    }
                }
            }

            // 将Mat转换回BufferedImage
            BufferedImage processedImage = new BufferedImage(
                    result.cols(),
                    result.rows(),
                    BufferedImage.TYPE_3BYTE_BGR
            );
            byte[] data1 = new byte[result.cols() * result.rows() * 3];
            result.get(0, 0, data1);
            byte[] targetPixels = ((DataBufferByte) processedImage.getRaster().getDataBuffer()).getData();
            System.arraycopy(data1, 0, targetPixels, 0, data1.length);

            // 释放资源
            for (Mat channel : channels) {
                channel.release();
            }
            redMinusGreen.release();
            redMinusBlue.release();
            redDominance.release();
            mask.release();
            result.release();
            src.release();

            return processedImage;
        } catch (Exception e) {
            logger.error("处理图片时出错: ", e);
            return image;
        }
    }
}