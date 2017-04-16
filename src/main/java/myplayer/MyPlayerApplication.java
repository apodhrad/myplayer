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

public class MyPlayerApplication {

	private static MyPlayer myPlayer;

	private static void init() {
		try {
			myPlayer = new MyPlayer();
		} catch (LineUnavailableException lue) {
			throw new RuntimeException(lue);
		}
	}

	public static String execute(String command) {
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
	
	public static void main(String[] args) throws Exception {
		init();

		WatchService watchService = FileSystems.getDefault().newWatchService();

		Path path = Paths.get("/home/apodhrad/Temp/watch");
		path.register(watchService, ENTRY_MODIFY);

		while (true) {
			WatchKey watchKey = watchService.take();

			for (WatchEvent<?> event : watchKey.pollEvents()) {
				Path changedPath = (Path) event.context();

				if (!Files.isDirectory(changedPath) && changedPath.endsWith("command.txt")) {
					Path newPath = path.resolve(changedPath);
					String content = new String(Files.readAllBytes(newPath));
					String result = null;
					try {
						System.out.println(content.trim());
						result = execute(content.trim());
					} catch (Exception e) {
						result = e.getMessage();
						e.printStackTrace();
					}

					try (BufferedWriter writer = Files.newBufferedWriter(path.resolve("result.txt"))) {
						writer.write(result);
					} catch (IOException x) {
						System.err.format("IOException: %s%n", x);
					}
				}

				// reset the key
				boolean valid = watchKey.reset();
				if (!valid) {
					System.err.println("Key could not be reset");
				}
			}
		}
	}
}
