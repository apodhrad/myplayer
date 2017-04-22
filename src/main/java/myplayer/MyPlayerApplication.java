package myplayer;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyPlayerApplication {

	private static final Logger log = LoggerFactory.getLogger(MyPlayerApplication.class);

	private MyPlayer myPlayer;
	private Path myPlayerPath;

	private MyPlayerApplication() {
		log.debug("Initialize MyPlayer");
		try {
			myPlayer = new MyPlayer();
		} catch (LineUnavailableException lue) {
			throw new RuntimeException(lue);
		}

		myPlayerPath = Paths.get(System.getProperty("user.home"), ".myplayer");
	}

	private void start() throws IOException, InterruptedException {
		Path inputPath = myPlayerPath.resolve("input");
		Files.createDirectories(inputPath);
		log.info("Command listener is set to '" + inputPath.toAbsolutePath() + "'");

		Path outputPath = myPlayerPath.resolve("output");
		Files.createDirectories(outputPath);
		log.info("Results will be written to '" + outputPath.toAbsolutePath() + "'");

		WatchService watchService = FileSystems.getDefault().newWatchService();
		inputPath.register(watchService, ENTRY_MODIFY);

		while (true) {
			WatchKey watchKey = watchService.take();

			for (WatchEvent<?> event : watchKey.pollEvents()) {
				Path changedPath = (Path) event.context();

				if (!Files.isDirectory(changedPath)) {
					Path commandPath = inputPath.resolve(changedPath);
					String content = new String(Files.readAllBytes(commandPath));
					String result = null;
					try {
						log.debug("Execute " + commandPath.toAbsolutePath());
						result = execute(content.trim());
					} catch (Exception e) {
						result = e.getMessage();
						e.printStackTrace();
					}

					Path resultPath = outputPath.resolve(changedPath.getFileName() + ".result");
					try (BufferedWriter writer = Files.newBufferedWriter(resultPath)) {
						writer.write(result);
					} catch (IOException x) {
						log.error("Cannot write to " + resultPath.toAbsolutePath(), x);
					}
				}

				// reset the key
				boolean valid = watchKey.reset();
				if (!valid) {
					log.error("Key could not be reset");
				}
			}
		}
	}

	public String execute(String command) {
		log.debug("Command '" + command + "'");
		switch (command) {
		case "play":
			try {
				myPlayer.play();
			} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
				e.printStackTrace();
			}
			return "Started";
		case "pause":
			myPlayer.pause();
			return "Paused";
		case "stop":
			myPlayer.stop();
			return "Stopped";
		case "quit":
			myPlayer.stop();
			System.exit(0);
		default:
			return "Unknown";
		}
	}

	private static void setDefaultUncaughtExceptionHandler() {
		try {
			Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
				@Override
				public void uncaughtException(Thread t, Throwable e) {
					log.error("Uncaught Exception detected in thread " + t, e);
				}
			});
		} catch (SecurityException e) {
			log.error("Could not set the Default Uncaught Exception Handler", e);
		}
	}

	public static void main(String[] args) throws Exception {
		setDefaultUncaughtExceptionHandler();

		MyPlayerApplication myPlayerApplication = new MyPlayerApplication();
		myPlayerApplication.start();
	}
}
