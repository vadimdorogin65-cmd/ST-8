import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class App {

    private static final String SITE_URL = "http://www.papercdcase.com/";
    private static final int    TRACK_LIMIT = 16;
    private static final Duration WAIT = Duration.ofSeconds(15);

    private record Cover(String artist, String title, List<String> tracks) {}

    public static void main(String[] args) throws Exception {
        Path root      = Path.of(System.getProperty("user.dir"));
        Path dataFile  = root.resolve("data").resolve("data.txt");
        Path resultDir = root.resolve("result");
        Path target    = resultDir.resolve("cd.pdf");

        Cover cover = readCover(dataFile);
        Files.createDirectories(resultDir);
        clearOldDownloads(resultDir);

        WebDriver driver = openBrowser(resultDir);
        WebDriverWait wait = new WebDriverWait(driver, WAIT);
        try {
            driver.get(SITE_URL);

            type(wait, artistField(), cover.artist());
            type(wait, titleField(),  cover.title());
            for (int i = 0; i < cover.tracks().size(); i++) {
                type(wait, trackField(i), cover.tracks().get(i));
            }

            choose(driver, "template", "jewel");
            choose(driver, "size",     "a4");
            choose(driver, "lang",     "west");
            choose(driver, "force_saveas", "yes");

            driver.findElement(submitButton()).submit();

            Path downloaded = awaitDownload(resultDir, WAIT);
            Files.move(downloaded, target, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("PDF сохранён: " + target.toAbsolutePath());
        } finally {
            driver.quit();
        }
    }

    private static Cover readCover(Path file) throws IOException {
        List<String> lines = new ArrayList<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (!line.isBlank()) lines.add(line.strip());
        }
        if (lines.size() < 2) {
            throw new IllegalStateException("В data.txt нужны минимум исполнитель и название альбома");
        }
        List<String> tracks = lines.subList(2, Math.min(lines.size(), 2 + TRACK_LIMIT));
        return new Cover(lines.get(0), lines.get(1), List.copyOf(tracks));
    }

    private static WebDriver openBrowser(Path downloadDir) {
        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("prefs", Map.of(
            "download.default_directory", downloadDir.toAbsolutePath().toString(),
            "download.prompt_for_download", false,
            "plugins.always_open_pdf_externally", true
        ));
        return new ChromeDriver(options);
    }

    private static void clearOldDownloads(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) return;
        try (var files = Files.list(dir)) {
            for (Path p : files.toList()) {
                String name = p.getFileName().toString().toLowerCase();
                if (name.endsWith(".pdf") || name.endsWith(".crdownload")) {
                    Files.deleteIfExists(p);
                }
            }
        }
    }

    private static Path awaitDownload(Path dir, Duration timeout) throws IOException, InterruptedException {
        long deadline = System.nanoTime() + timeout.multipliedBy(4).toNanos();
        while (System.nanoTime() < deadline) {
            boolean stillDownloading = false;
            Path ready = null;
            try (var files = Files.list(dir)) {
                for (Path p : files.toList()) {
                    String name = p.getFileName().toString().toLowerCase();
                    if (name.endsWith(".crdownload")) stillDownloading = true;
                    else if (name.endsWith(".pdf") && Files.size(p) > 0) ready = p;
                }
            }
            if (ready != null && !stillDownloading) return ready;
            Thread.sleep(500);
        }
        throw new IllegalStateException("PDF не был скачан в каталог " + dir);
    }

    private static void type(WebDriverWait wait, By locator, String text) {
        WebElement field = wait.until(ExpectedConditions.elementToBeClickable(locator));
        field.clear();
        field.sendKeys(text);
    }

    private static void choose(WebDriver driver, String group, String value) {
        driver.findElement(By.cssSelector(
            "input[name='" + group + "'][value='" + value + "']")).click();
    }

    private static By artistField() {
        return By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[1]/td[2]/input");
    }

    private static By titleField() {
        return By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[2]/td[2]/input");
    }

    private static By trackField(int index) {
        int column = index / 8 + 1;
        int row    = index % 8 + 1;
        return By.xpath(
            "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]"
            + "/table/tbody/tr/td[" + column + "]/table/tbody/tr[" + row + "]/td[2]/input");
    }

    private static By submitButton() {
        return By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/p/input");
    }
}
