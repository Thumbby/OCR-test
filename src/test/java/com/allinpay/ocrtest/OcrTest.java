package com.allinpay.ocrtest;

import com.allinpay.ocrtest.util.OcrUtil;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest(classes = OcRtestApplication.class)
public class OcrTest {

    private static final Logger logger = LoggerFactory.getLogger(OcrTest.class);

    @Autowired
    private OcrUtil ocrUtil;

    @Test
    public void ocrTest() {
        String filePath = "";
        List<String> result = ocrUtil.printPDF(filePath);
        result.forEach(logger::info);
    }
}
