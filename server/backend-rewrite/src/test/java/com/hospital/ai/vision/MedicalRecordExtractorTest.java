package com.hospital.ai.vision;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

class MedicalRecordExtractorTest {

    private final MedicalRecordExtractor extractor = new MedicalRecordExtractor(new ObjectMapper());

    @Test
    void parsesCleanJsonIntoAllFields() {
        String response = """
                {
                  "patientName": "张三",
                  "gender": "男",
                  "age": 45,
                  "visitDate": "2025-03-12",
                  "department": "内科",
                  "chiefComplaint": "咳嗽3天",
                  "presentIllness": "受凉后出现咳嗽",
                  "diagnosis": "上呼吸道感染",
                  "prescription": "阿莫西林 0.5g 每日三次",
                  "doctor": "李医生"
                }
                """;

        MedicalRecordExtractor.Parsed parsed = extractor.parse(response);

        assertThat(parsed.fields().patientName()).isEqualTo("张三");
        assertThat(parsed.fields().age()).isEqualTo(45);
        assertThat(parsed.fields().doctor()).isEqualTo("李医生");
        assertThat(extractor.confidence(parsed.fields())).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(extractor.status(parsed.fields(), extractor.confidence(parsed.fields())))
                .isEqualTo(AiExtractionHistory.STATUS_SUCCESS);
    }

    @Test
    void stripsMarkdownFenceAroundJson() {
        String response = "```json\n{\"patientName\":\"李四\",\"age\":\"30\"}\n```";

        MedicalRecordExtractor.Parsed parsed = extractor.parse(response);

        assertThat(parsed.fields().patientName()).isEqualTo("李四");
        assertThat(parsed.fields().age()).isEqualTo(30);
    }

    @Test
    void coercesAgeFromChineseSuffixString() {
        MedicalRecordExtractor.Parsed parsed = extractor.parse("{\"age\":\"32 岁\"}");
        assertThat(parsed.fields().age()).isEqualTo(32);
    }

    @Test
    void missingFieldsBecomeNullAndConfidenceReflectsCoverage() {
        MedicalRecordExtractor.Parsed parsed = extractor.parse(
                "{\"patientName\":\"王五\",\"diagnosis\":\"流感\"}");

        assertThat(parsed.fields().patientName()).isEqualTo("王五");
        assertThat(parsed.fields().diagnosis()).isEqualTo("流感");
        assertThat(parsed.fields().age()).isNull();
        assertThat(parsed.fields().doctor()).isNull();
        // 2 of 10 fields filled
        assertThat(extractor.confidence(parsed.fields()))
                .isEqualByComparingTo(BigDecimal.valueOf(20).setScale(2));
        assertThat(extractor.status(parsed.fields(), extractor.confidence(parsed.fields())))
                .isEqualTo(AiExtractionHistory.STATUS_PARTIAL);
    }

    @Test
    void unparseableResponseProducesErrorRawAndEmptyFields() {
        MedicalRecordExtractor.Parsed parsed = extractor.parse("抱歉，无法识别这张图片。");

        assertThat(parsed.rawJson()).containsEntry("error", "unparseable");
        assertThat(parsed.fields().patientName()).isNull();
        assertThat(extractor.confidence(parsed.fields())).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(extractor.status(parsed.fields(), extractor.confidence(parsed.fields())))
                .isEqualTo(AiExtractionHistory.STATUS_FAILED);
    }

    @Test
    void recoversJsonObjectEmbeddedInProse() {
        String response = "好的，识别结果如下：{\"patientName\":\"赵六\",\"age\":50} 希望对您有帮助。";

        MedicalRecordExtractor.Parsed parsed = extractor.parse(response);

        assertThat(parsed.fields().patientName()).isEqualTo("赵六");
        assertThat(parsed.fields().age()).isEqualTo(50);
    }

    @Test
    void blankResponseReturnsAllNulls() {
        MedicalRecordExtractor.Parsed parsed = extractor.parse("");
        assertThat(parsed.fields().patientName()).isNull();
        assertThat(extractor.confidence(parsed.fields())).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
