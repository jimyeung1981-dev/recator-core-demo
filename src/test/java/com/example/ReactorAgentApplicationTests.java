package com.example;

import com.example.tools.AlgorithmTools;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ReactorAgentApplicationTests {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private AlgorithmTools algorithmTools;

    @Test
    @DisplayName("验证服务健康检查接口 /api/health")
    void testHealthEndpoint() {
        webTestClient.get().uri("/api/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP")
                .jsonPath("$.framework").isEqualTo("Project Reactor (reactor-core)")
                .jsonPath("$.model").isEqualTo("deepseek-ai/DeepSeek-V4.1-Flash");
    }

    @Test
    @DisplayName("验证本地算法工具直接调用接口 /api/tools/bubble-sort")
    void testBubbleSortToolEndpoint() {
        webTestClient.get().uri("/api/tools/bubble-sort?numbers=3,1,2")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.result").isNotEmpty();
    }

    @Test
    @DisplayName("验证 AlgorithmTools 冒泡排序执行逻辑")
    void testAlgorithmToolsBubbleSort() {
        String result = algorithmTools.simulateBubbleSort("5, 1, 4, 2, 8");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("[1, 2, 4, 5, 8]"), "排序结果应包含有序数组");
    }
}
