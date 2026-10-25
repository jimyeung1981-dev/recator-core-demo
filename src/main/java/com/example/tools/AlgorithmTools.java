package com.example.tools;

import com.example.agent.tool.AgentTool;
import com.example.agent.tool.ToolParam;
import java.util.Arrays;

/**
 * 算法演示工具类，提供供 Reactor ReAct Agent 响应式调用的实际执行工具。
 */
public class AlgorithmTools {

    /**
     * 在真实 JVM 环境中模拟并追踪冒泡排序的执行过程。
     *
     * @param numbers 待排序的数字（以逗号分隔，例如 "29, 10, 14, 37, 14"）
     * @return 排序过程的完整日志，包括每轮状态、交换次数和最终结果
     */
    @AgentTool(
            name = "simulate_bubble_sort",
            description = "在真实 Java 环境中执行冒泡排序算法，分析输入的数组并返回逐步排序过程、比较次数、交换次数及最终有序数组"
    )
    public String simulateBubbleSort(
            @ToolParam(name = "numbers", description = "待排序的整数列表，以英文逗号分隔，例如 '5, 2, 9, 1, 5, 6'")
            String numbers) {
        try {
            if (numbers == null || numbers.trim().isEmpty()) {
                return "错误：待排序的数组字符串不能为空。";
            }

            int[] arr = Arrays.stream(numbers.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .mapToInt(Integer::parseInt)
                    .toArray();

            if (arr.length == 0) {
                return "错误：未解析到有效的整数。";
            }

            int n = arr.length;
            int comparisons = 0;
            int swaps = 0;
            StringBuilder log = new StringBuilder();
            log.append("================ 冒泡排序执行日志 ================\n");
            log.append("初始数组: ").append(Arrays.toString(arr)).append("\n");

            boolean swapped;
            for (int i = 0; i < n - 1; i++) {
                swapped = false;
                for (int j = 0; j < n - 1 - i; j++) {
                    comparisons++;
                    if (arr[j] > arr[j + 1]) {
                        int temp = arr[j];
                        arr[j] = arr[j + 1];
                        arr[j + 1] = temp;
                        swaps++;
                        swapped = true;
                    }
                }
                log.append(String.format("第 %d 轮遍历结束: %s (本轮有交换: %s)\n", i + 1, Arrays.toString(arr), swapped));
                if (!swapped) {
                    log.append(">> 检测到本轮无任何元素交换，说明数组已有序，提前触发退出优化！\n");
                    break;
                }
            }

            log.append("--------------------------------------------------\n");
            log.append("最终排序结果: ").append(Arrays.toString(arr)).append("\n");
            log.append("累计比较次数: ").append(comparisons).append("\n");
            log.append("累计交换次数: ").append(swaps).append("\n");
            log.append("==================================================");

            return log.toString();
        } catch (NumberFormatException e) {
            return "参数解析错误：请确保输入的是以逗号分隔的合法整数，错误详情: " + e.getMessage();
        } catch (Exception e) {
            return "执行冒泡排序失败: " + e.getMessage();
        }
    }
}
