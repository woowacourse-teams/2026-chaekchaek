import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

public class ValidateCommitMessage {

    private static final Pattern COMMIT_MESSAGE_PATTERN = Pattern.compile(
            "^\\[(BE|FE|AN|ALL)] " +
            "(feat|fix|docs|refactor|test|chore|ci|build|perf|revert|style)" +
            "(\\([^)]+\\))?" +
            "!?: .+$"
    );

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("커밋 메시지 파일 경로가 전달되지 않았습니다.");
            System.exit(1);
        }

        Path commitMessageFilePath = Path.of(args[0]);
        String subject = readSubject(commitMessageFilePath);

        if (!COMMIT_MESSAGE_PATTERN.matcher(subject).matches()) {
            printErrorMessage();
            System.exit(1);
        }
    }

    private static String readSubject(Path commitMessageFilePath) throws IOException {
        List<String> lines = Files.readAllLines(commitMessageFilePath);

        if (lines.isEmpty()) {
            return "";
        }

        return lines.get(0)
                .replace("\r", "")
                .trim();
    }

    private static void printErrorMessage() {
        System.err.println("커밋 메시지 형식이 올바르지 않습니다.");
        System.err.println("형식: [영역] type(scope): 설명");
        System.err.println("예시: [BE] build: 프로젝트 초기 세팅");
        System.err.println("예시: [FE] feat(auth): 로그인 화면 구현");
        System.err.println("예시: [AN] fix: 토큰 만료 처리 수정");
        System.err.println("예시: [ALL] docs: 개발 환경 설정 안내 추가");
    }
}

