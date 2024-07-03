package com.example.log_compressor;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

@SpringBootApplication
public class LogCompressorApplication implements CommandLineRunner {

	@Value("${log.path.default}")
	private String defaultPath;

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(LogCompressorApplication.class);
		app.setWebApplicationType(WebApplicationType.NONE); // 웹 서버 비활성화
		app.run(args);
	}

	@Override
	public void run(String... args) throws Exception {
		System.out.println("Hello, I'm Log Compressor!");
		if (args.length < 1) {
			System.err.println("Usage: LogCompressorApp <log-directory>");
			System.exit(1);
		}

		String logDirPath = args[0];
		Path logDir = Paths.get(logDirPath);

		if (!Files.exists(logDir) || !Files.isDirectory(logDir)) {
			System.err.println("Invalid log directory: " + logDirPath);
			System.exit(1);
		}

		LocalDate today = LocalDate.now();
		String dateSuffix = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		String tarGzFileName = "filename-" + dateSuffix + ".tar.gz";

		try (FileOutputStream fos = new FileOutputStream(tarGzFileName);
			 GzipCompressorOutputStream gcos = new GzipCompressorOutputStream(fos);
			 TarArchiveOutputStream taos = new TarArchiveOutputStream(gcos);
			) {
			Files.list(logDir).filter(Files::isRegularFile)
					.filter(file->file.getFileName().toString().contains(".log"))
					.forEach(path -> {

				try {
					addFileToTarGz(taos, path.toFile(), ".");
				} catch (IOException e) {
					System.err.println("Error adding file to tar.gz: " + path);
				}


			});
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("Log Files have been compressed to " + tarGzFileName);
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
