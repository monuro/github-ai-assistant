package com.github.ai.assistant.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ConsoleUtils 单元测试
 * 
 * 注意：这些测试主要验证输出格式，交互式输入测试需要特殊处理
 */
@DisplayName("ConsoleUtils 测试")
class ConsoleUtilsTest {

    @Nested
    @DisplayName("输出方法测试")
    class OutputTests {

        @Test
        @DisplayName("success 应输出带 ✅ 的信息")
        void shouldOutputSuccessWithCheckmark() {
            // Given
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outputStream));

            try {
                // When
                ConsoleUtils.success("操作成功");

                // Then
                String output = outputStream.toString();
                assertTrue(output.contains("✅"));
                assertTrue(output.contains("操作成功"));
            } finally {
                System.setOut(originalOut);
            }
        }

        @Test
        @DisplayName("error 应输出带 ❌ 的信息到 stderr")
        void shouldOutputErrorWithCross() {
            // Given
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
            PrintStream originalErr = System.err;
            System.setErr(new PrintStream(errorStream));

            try {
                // When
                ConsoleUtils.error("发生错误");

                // Then
                String output = errorStream.toString();
                assertTrue(output.contains("❌"));
                assertTrue(output.contains("发生错误"));
            } finally {
                System.setErr(originalErr);
            }
        }

        @Test
        @DisplayName("warn 应输出带 ⚠️ 的信息")
        void shouldOutputWarningWithWarningSign() {
            // Given
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outputStream));

            try {
                // When
                ConsoleUtils.warn("警告信息");

                // Then
                String output = outputStream.toString();
                assertTrue(output.contains("⚠"));
                assertTrue(output.contains("警告信息"));
            } finally {
                System.setOut(originalOut);
            }
        }

        @Test
        @DisplayName("info 应输出带 ℹ️ 的信息")
        void shouldOutputInfoWithInfoSign() {
            // Given
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outputStream));

            try {
                // When
                ConsoleUtils.info("提示信息");

                // Then
                String output = outputStream.toString();
                assertTrue(output.contains("ℹ"));
                assertTrue(output.contains("提示信息"));
            } finally {
                System.setOut(originalOut);
            }
        }
    }

    @Nested
    @DisplayName("分隔线测试")
    class SeparatorTests {

        @Test
        @DisplayName("separator 应输出 50 个 ─")
        void shouldOutputSingleSeparator() {
            // Given
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outputStream));

            try {
                // When
                ConsoleUtils.separator();

                // Then
                String output = outputStream.toString().trim();
                assertEquals("─".repeat(50), output);
            } finally {
                System.setOut(originalOut);
            }
        }

        @Test
        @DisplayName("doubleSeparator 应输出 50 个 ═")
        void shouldOutputDoubleSeparator() {
            // Given
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outputStream));

            try {
                // When
                ConsoleUtils.doubleSeparator();

                // Then
                String output = outputStream.toString().trim();
                assertEquals("═".repeat(50), output);
            } finally {
                System.setOut(originalOut);
            }
        }
    }

    @Nested
    @DisplayName("title 测试")
    class TitleTests {

        @Test
        @DisplayName("title 应输出带双分隔线的标题")
        void shouldOutputTitleWithDoubleSeparators() {
            // Given
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outputStream));

            try {
                // When
                ConsoleUtils.title("测试标题");

                // Then
                String output = outputStream.toString();
                String[] lines = output.split("\n");
                
                assertTrue(lines.length >= 3);
                assertEquals("═".repeat(50), lines[0]);
                assertEquals("测试标题", lines[1]);
                assertEquals("═".repeat(50), lines[2]);
            } finally {
                System.setOut(originalOut);
            }
        }
    }

    @Nested
    @DisplayName("withSpinner 测试")
    class WithSpinnerTests {

        @Test
        @DisplayName("成功执行任务时应返回结果")
        void shouldShowCheckmarkOnSuccess() throws Exception {
            // When
            String result = ConsoleUtils.withSpinner("Loading", () -> "result");

            // Then
            assertEquals("result", result);
        }

        @Test
        @DisplayName("任务失败时应抛出异常")
        void shouldShowCrossAndThrowOnFailure() {
            // When & Then
            assertThrows(RuntimeException.class, () -> 
                ConsoleUtils.withSpinner("Loading", () -> {
                    throw new RuntimeException("Error");
                })
            );
        }
    }
}
