import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App {

    private static final String BASE_URL = "http://www.papercdcase.com/";
    private static final int MAX_TRACKS = 16;

    // XPaths for track input fields (left column: tracks 1-8, right column: tracks 9-16)
    private static final String[] TRACK_XPATHS = {
        "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[1]/td[2]/input",
        "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[2]/td[2]/input",
        "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[3]/td[2]/input",
        "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[4]/td[2]/input",
        "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[5]/td[2]/input",
        "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[6]/td[2]/input",
        "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[7]/td[2]/input",
        "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[1]/table/tbody/tr[8]/td[2]/input",
        "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[1]/td[2]/input",
        "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[2]/td[2]/input",
        "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[3]/td[2]/input",
        "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[4]/td[2]/input",
        "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[5]/td[2]/input",
        "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[6]/td[2]/input",
        "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[7]/td[2]/input",
        "/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[3]/td[2]/table/tbody/tr/td[2]/table/tbody/tr[8]/td[2]/input"
    };

    public static void main(String[] args) throws Exception {
        Path projectRoot = Path.of(System.getProperty("user.dir"));
        Path dataFile = projectRoot.resolve("data").resolve("data.txt");
        Path resultDir = projectRoot.resolve("result");
        Files.createDirectories(resultDir);

        // Load data from data.txt
        List<String> data = new ArrayList<>();
        for (String line : Files.readAllLines(dataFile, StandardCharsets.UTF_8)) {
            if (!line.isBlank()) data.add(line.stripTrailing());
        }
        String artist = data.get(0);
        String title  = data.get(1);
        List<String> tracks = new ArrayList<>();
        for (int i = 2; i < data.size() && tracks.size() < MAX_TRACKS; i++) {
            tracks.add(data.get(i));
        }

        // Configure Chrome to save downloads directly to result/
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", resultDir.toAbsolutePath().toString());
        prefs.put("plugins.always_open_pdf_externally", true);
        prefs.put("download.prompt_for_download", false);
        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("prefs", prefs);

        // Clean up old downloads before starting
        try (var s = Files.list(resultDir)) {
            for (Path p : s.toList()) {
                String n = p.getFileName().toString().toLowerCase();
                if (n.endsWith(".pdf") || n.endsWith(".crdownload")) Files.deleteIfExists(p);
            }
        }

        WebDriver webDriver = new ChromeDriver(options);
        try {
            webDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            webDriver.get(BASE_URL);

            // Fill in artist and title
            webDriver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[1]/td[2]/input"))
                     .sendKeys(artist);
            webDriver.findElement(By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/table/tbody/tr[2]/td[2]/input"))
                     .sendKeys(title);

            // Fill in track list
            for (int i = 0; i < tracks.size(); i++) {
                webDriver.findElement(By.xpath(TRACK_XPATHS[i])).sendKeys(tracks.get(i));
            }

            // Select Jewel Case (Type)
            webDriver.findElement(By.xpath("//input[@name='template' and @value='jewel']")).click();
            // Select A4 (Paper)
            webDriver.findElement(By.xpath("//input[@name='size' and @value='a4']")).click();
            // Select Western font
            webDriver.findElement(By.xpath("//input[@name='lang' and @value='west']")).click();
            // Force browser to save file (not open inline)
            webDriver.findElement(By.xpath("//input[@name='force_saveas' and @value='yes']")).click();

            // Submit the form
            WebElement btn = webDriver.findElement(
                By.xpath("/html/body/table[2]/tbody/tr/td[1]/div/form/p/input"));
            btn.submit();

            // Wait for PDF to appear in result/
            Path pdfFile = waitForPdf(resultDir);
            Path target = resultDir.resolve("cd.pdf");
            Files.deleteIfExists(target);
            Files.move(pdfFile, target);
            System.out.println("Saved: " + target);
        } finally {
            webDriver.quit();
        }
    }

    private static Path waitForPdf(Path dir) throws Exception {
        for (int i = 0; i < 60; i++) {
            Thread.sleep(1000);
            try (var stream = Files.list(dir)) {
                for (Path p : stream.toList()) {
                    String name = p.getFileName().toString().toLowerCase();
                    // Fully downloaded PDF
                    if (name.endsWith(".pdf") && Files.size(p) > 0) {
                        return p;
                    }
                    // Chrome in-progress download: wait for it to become stable, then rename
                    if (name.endsWith(".crdownload")) {
                        long size = Files.size(p);
                        Thread.sleep(1000);
                        if (Files.size(p) == size && size > 0) {
                            Path finished = dir.resolve(name.replace(".crdownload", ""));
                            if (!Files.exists(finished)) {
                                Files.move(p, finished, StandardCopyOption.REPLACE_EXISTING);
                            }
                            return finished;
                        }
                    }
                }
            }
        }
        throw new IllegalStateException("PDF was not downloaded to " + dir);
    }
}
