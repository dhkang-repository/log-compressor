package com.example.log_compressor;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;

@SpringBootApplication
@EnableScheduling
public class LogCompressorApplication {

	@Value("${log.path.default}")
	private String defaultPath;

	@Value("${log.filenames}")
	private Set<String> fileSet;

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(LogCompressorApplication.class);
		app.setWebApplicationType(WebApplicationType.NONE); // 웹 서버 비활성화
		app.run(args);
	}

	@Scheduled(cron = "5 * * * * ?", zone = "Asia/Seoul") // 매일 오전 4시에 실행
	private void scheduleLogCompression() throws IOException {
		StringBuilder stringBuilder = new StringBuilder();
		LocalDate today = LocalDate.now().minusDays(1L);
		String dateSuffix = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

		for(String filename : fileSet) {
			String tarGzFileName = filename + "-" + dateSuffix + ".tar.gz";
			
			Path logDir = Paths.get(defaultPath);

			Files.list(logDir).filter(Files::isRegularFile)
				.filter(file->file.getFileName().toString().contains(".log"))
				.forEach(path -> {
					try (FileOutputStream fos = new FileOutputStream(tarGzFileName);
						 GzipCompressorOutputStream gcos = new GzipCompressorOutputStream(fos);
						 TarArchiveOutputStream taos = new TarArchiveOutputStream(gcos);
					) {
						File file = path.toFile();
						addFileToTarGz(taos, file, ".");
						file.delete();
						stringBuilder.append("Log Files have been compressed from " + file.getName() + "\n");
						stringBuilder.append("Log Files have been compressed to " + tarGzFileName + "\n");
					} catch (IOException e) {
						stringBuilder.append("Error adding file to tar.gz: " + path + "\n");
					}
			});
		}

		stringBuilder.append("Schedule End\n");
		System.out.println(stringBuilder);
	}

	private static void addFileToTarGz(TarArchiveOutputStream taos, File file, String base) throws IOException {
		String entryName = base + File.separator + file.getName();
		TarArchiveEntry entry = new TarArchiveEntry(file, entryName);
		taos.putArchiveEntry(entry);

		try (FileInputStream fis = new FileInputStream(file)) {
			byte[] buffer = new byte[1024];
			int count;
			while ((count = fis.read(buffer)) != -1) {
				taos.write(buffer, 0 , count);
			}
			taos.closeArchiveEntry();
		}
	}
}
