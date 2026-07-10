package com.example.minshuku.support;

import java.lang.reflect.Method;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

/**
 * テストクラス単位の開始・終了・成否を標準出力へ残す拡張。
 */
public class TestResultLogger implements BeforeAllCallback, BeforeTestExecutionCallback, TestWatcher, AfterAllCallback {
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(TestResultLogger.class);

    @Override
    public void beforeAll(ExtensionContext context) {
        // クラス開始のタイミングで、対象クラス名を出力する。
        printLine("【テストクラス開始】" + className(context));
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        // 各テストの実行直前に、ケース名を出す。
        printLine("  テスト開始：" + testName(context));
        printLine("  テスト内容：" + testContent(context));
        printLine("  テストコード注解：" + testCodeNote(context));
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        // 成功件数を集計し、結果をログへ残す。
        counter(context, "passed").increment();
        printLine("  テスト成功：" + testName(context));
        printStringResult(context, "成功", null);
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        // 失敗理由を後追いしやすいように明示する。
        counter(context, "failed").increment();
        printLine("  テスト失敗：" + testName(context) + "，理由：" + cause.getMessage());
        printStringResult(context, "失敗", cause.getMessage());
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        // 中止されたケースも集計に残す。
        counter(context, "aborted").increment();
        printLine("  テスト中止：" + testName(context) + "，理由：" + cause.getMessage());
        printStringResult(context, "中止", cause.getMessage());
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        // 無効化されたテストもスキップ件数として扱う。
        counter(context, "skipped").increment();
        printLine("  テストスキップ：" + testName(context) + "，理由：" + reason.orElse("理由未入力"));
        printStringResult(context, "スキップ", reason.orElse("理由未入力"));
    }

    @Override
    public void afterAll(ExtensionContext context) {
        // クラス全体の集計結果を1行でまとめる。
        int passed = counter(context, "passed").value();
        int failed = counter(context, "failed").value();
        int aborted = counter(context, "aborted").value();
        int skipped = counter(context, "skipped").value();
        int total = passed + failed + aborted + skipped;
        String resultText = className(context)
                + "：合計 " + total
                + "，成功 " + passed
                + "，失敗 " + failed
                + "，中止 " + aborted
                + "，スキップ " + skipped;
        printLine("【テストクラス結果】" + resultText);
        System.out.print("【テストクラス文字列結果】" + resultText + System.lineSeparator());
    }

    private void printLine(String text) {
        System.out.print(text + System.lineSeparator());
    }

    private void printStringResult(ExtensionContext context, String result, String reason) {
        String text = "  テスト文字列結果：" + testName(context) + " / 結果=" + result;
        if (reason != null && !reason.isBlank()) {
            text += " / 理由=" + reason;
        }
        System.out.print(text + System.lineSeparator());
    }

    private String testName(ExtensionContext context) {
        return className(context) + " / " + context.getDisplayName();
    }

    private String testContent(ExtensionContext context) {
        Method method = context.getRequiredTestMethod();
        DisplayName displayName = method.getAnnotation(DisplayName.class);
        if (displayName != null) {
            return displayName.value();
        }
        return humanizeMethodName(method.getName());
    }

    private String testCodeNote(ExtensionContext context) {
        String methodName = context.getRequiredTestMethod().getName();
        if (methodName.contains("currentDate") || methodName.contains("SystemDate")) {
            return "Given=システム日付とローカル環境日付を分けて準備 / When=日付取得・予約登録を実行 / Then=システム日付基準で判定されることを検証";
        }
        if (methodName.startsWith("create")) {
            return "Given=登録入力と関連 mock/DB データを準備 / When=登録処理を実行 / Then=保存結果または業務エラーを検証";
        }
        if (methodName.startsWith("update")) {
            return "Given=更新対象データと更新値を準備 / When=更新処理を実行 / Then=更新後の状態または業務エラーを検証";
        }
        if (methodName.startsWith("delete") || methodName.startsWith("cancel")) {
            return "Given=削除・取消対象データを準備 / When=削除・取消処理を実行 / Then=状態変更、遷移先、メッセージを検証";
        }
        if (methodName.startsWith("find") || methodName.startsWith("query") || methodName.contains("Returns")) {
            return "Given=検索条件と期待データを準備 / When=検索・参照処理を実行 / Then=取得結果を期待値と比較";
        }
        if (methodName.contains("Csrf")) {
            return "Given=CSRF token なしのリクエストを準備 / When=POST API を実行 / Then=不正リクエストとして拒否されることを検証";
        }
        if (methodName.contains("Path")) {
            return "Given=許可対象外パスを準備 / When=画面アクセスを実行 / Then=安全に拒否されることを検証";
        }
        return "Given=テスト入力を準備 / When=対象処理を実行 / Then=期待結果を文字列出力とアサーションで検証";
    }

    private String humanizeMethodName(String methodName) {
        String separated = methodName.replaceAll("([a-z])([A-Z])", "$1 $2").replaceAll("([A-Z])([A-Z][a-z])", "$1 $2");
        return separated;
    }

    private String className(ExtensionContext context) {
        Class<?> testClass = context.getRequiredTestClass();
        DisplayName displayName = testClass.getAnnotation(DisplayName.class);
        return displayName == null ? testClass.getSimpleName() : displayName.value();
    }

    private Counter counter(ExtensionContext context, String key) {
        return context.getRoot().getStore(NAMESPACE).getOrComputeIfAbsent(
                context.getRequiredTestClass().getName() + "." + key, unused -> new Counter(), Counter.class);
    }

    private static class Counter {
        private int value;

        void increment() {
            value += 1;
        }

        int value() {
            return value;
        }
    }
}
