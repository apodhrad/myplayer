package myplayer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class MyPlayer {

	private Clip clip;
	private long time;
	private URL songUrl;

	public MyPlayer() throws LineUnavailableException {
		clip = AudioSystem.getClip();
		time = 0;
	}

	public String song(String url) throws MalformedURLException {
		String newUrl = url.replaceAll(" ", "%20");
		songUrl = new URL(newUrl);
		time = 0;
		return newUrl;
	}

	public String play() throws UnsupportedAudioFileException, IOException, LineUnavailableException {
		if (clip.isActive() || clip.isRunning()) {
			return "Already running";
		}

		if (!clip.isOpen()) {
			clip.open(getAudioInputStream());
		}

		clip.setMicrosecondPosition(time);
		clip.start();
		return "Started";
	}

	public String pause() {
		time = clip.getMicrosecondPosition();
		clip.stop();
		return "Paused";
	}

	public String stop() {
		time = 0;
		clip.stop();
		clip.close();
		return "Stopped";
	}

	private AudioInputStream getAudioInputStream() throws UnsupportedAudioFileException, IOException {
		if (songUrl == null) {
			throw new IllegalStateException("No song has been set");
		}

		AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(songUrl);

		// Audio format provides information like sample rate, size, channels.
		AudioFormat audioFormat = audioInputStream.getFormat();
		System.out.println("Play input audio format=" + audioFormat);
		if (audioFormat.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
			AudioFormat newFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, audioFormat.getSampleRate(), 16,
					audioFormat.getChannels(), audioFormat.getChannels() * 2, audioFormat.getSampleRate(), false);
			System.out.println("Converting audio format to " + newFormat);
			AudioInputStream newStream = AudioSystem.getAudioInputStream(newFormat, audioInputStream);
			audioFormat = newFormat;
			audioInputStream = newStream;
		}
		return audioInputStream;
	}
}
