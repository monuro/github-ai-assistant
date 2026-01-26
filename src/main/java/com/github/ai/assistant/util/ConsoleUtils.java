package com.github.ai.assistant.util;

import java.io.Console;
import java.util.Scanner;

/**
 * 控制台工具类
 * 
 * 提供控制台交互功能，包括：
 * - 确认提示
 * - 颜色输出
 * - 进度显示
 */
public final class ConsoleUtils {

    private static final Scanner scanner = new Scanner(System.in);
    
    private ConsoleUtils() {
        // 工具类禁止实例化
    }

    /**
     * 请求用户确认 (y/n)
     * 
     * @param message 提示信息
     * @return 用户是否确认
     */
    public static boolean confirm(String message) {
        System.out.print(message + " (y/n): ");
        System.out.flush();
        
        String input = scanner.nextLine().trim().toLowerCase();
        return input.equals("y") || input.equals("yes");
    }

    /**
     * 请求用户确认，带默认值
     * 
     * @param message      提示信息
     * @param defaultValue 默认值
     * @return 用户是否确认
     */
    public static boolean confirm(String message, boolean defaultValue) {
        String hint = defaultValue ? " (Y/n): " : " (y/N): ";
        System.out.print(message + hint);
        System.out.flush();
        
        String input = scanner.nextLine().trim().toLowerCase();
        
        if (input.isEmpty()) {
            return defaultValue;
        }
        
        return input.equals("y") || input.equals("yes");
    }

    /**
     * 获取用户输入
     * 
     * @param prompt 提示信息
     * @return 用户输入
     */
    public static String input(String prompt) {
        System.out.print(prompt);
        System.out.flush();
        return scanner.nextLine();
    }

    /**
     * 获取用户输入，带默认值
     * 
     * @param prompt       提示信息
     * @param defaultValue 默认值
     * @return 用户输入或默认值
     */
    public static String input(String prompt, String defaultValue) {
        System.out.print(prompt + " [" + defaultValue + "]: ");
        System.out.flush();
        
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? defaultValue : input;
    }

    /**
     * 打印成功信息
     */
    public static void success(String message) {
        System.out.println("✅ " + message);
    }

    /**
     * 打印错误信息
     */
    public static void error(String message) {
        System.err.println("❌ " + message);
    }

    /**
     * 打印警告信息
     */
    public static void warn(String message) {
        System.out.println("⚠️  " + message);
    }

    /**
     * 打印信息
     */
    public static void info(String message) {
        System.out.println("ℹ️  " + message);
    }

    /**
     * 打印分隔线
     */
    public static void separator() {
        System.out.println("─".repeat(50));
    }

    /**
     * 打印双分隔线
     */
    public static void doubleSeparator() {
        System.out.println("═".repeat(50));
    }

    /**
     * 打印标题
     */
    public static void title(String title) {
        doubleSeparator();
        System.out.println(title);
        doubleSeparator();
    }

    /**
     * 显示简单的加载动画（阻塞式）
     * 
     * @param message 加载信息
     * @param task    要执行的任务
     * @return 任务结果
     */
    public static <T> T withSpinner(String message, java.util.concurrent.Callable<T> task) throws Exception {
        String[] frames = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};
        final boolean[] running = {true};
        final Exception[] error = {null};
        @SuppressWarnings("unchecked")
        final Object[] result = {null};
        
        // 后台执行任务
        Thread taskThread = new Thread(() -> {
            try {
                result[0] = task.call();
            } catch (Exception e) {
                error[0] = e;
            } finally {
                running[0] = false;
            }
        });
        taskThread.start();
        
        // 显示动画
        int i = 0;
        while (running[0]) {
            System.out.print("\r" + frames[i % frames.length] + " " + message);
            System.out.flush();
            i++;
            try {
                Thread.sleep(80);
            } catch (InterruptedException ignored) {
                break;
            }
        }
        
        taskThread.join();
        
        // 清除动画行，显示结果
        System.out.print("\r" + " ".repeat(message.length() + 3) + "\r");
        System.out.flush();
        
        if (error[0] != null) {
            throw error[0];
        }
        
        @SuppressWarnings("unchecked")
        T typedResult = (T) result[0];
        return typedResult;
    }

    /**
     * 选择菜单
     * 
     * @param prompt  提示信息
     * @param options 选项列表
     * @return 选择的索引 (从0开始)，-1 表示取消
     */
    public static int select(String prompt, String... options) {
        System.out.println(prompt);
        for (int i = 0; i < options.length; i++) {
            System.out.println("  " + (i + 1) + ") " + options[i]);
        }
        System.out.print("请选择 (1-" + options.length + "): ");
        System.out.flush();
        
        try {
            int choice = Integer.parseInt(scanner.nextLine().trim());
            if (choice >= 1 && choice <= options.length) {
                return choice - 1;
            }
        } catch (NumberFormatException ignored) {
        }
        
        return -1;
    }
}
