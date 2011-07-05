package net.pms.configuration;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;

class ConfigurationProgramPaths implements ProgramPaths {

	private static final String KEY_VLC_PATH = "vlc_path";
	private static final String KEY_EAC3TO_PATH = "eac3to_path";
	private static final String KEY_MENCODER_PATH = "mencoder_path";
	private static final String KEY_MENCODERMT_PATH = "mencodermt_path";
	private static final String KEY_MENCODEROLDER_PATH = "mencoderolder_path";
	private static final String KEY_MENCODEROLDERMT_PATH = "mencoderoldermt_path";
	private static final String KEY_FFMPEG_PATH = "ffmpeg_path";
	private static final String KEY_MPLAYER_PATH = "mplayer_path";
	private static final String KEY_TSMUXER_PATH = "tsmuxer_path";
	private static final String KEY_FLAC_PATH = "flac_path";
	private static final String KEY_DCRAW = "dcraw_path";
	
	private final Configuration configuration;
	private final ProgramPaths defaults;

	public ConfigurationProgramPaths(Configuration configuration, ProgramPaths defaults) {
		this.configuration = configuration;
		this.defaults = defaults;
	}

	@Override
	public String getEac3toPath() {
		return stringFromConfigFile(KEY_EAC3TO_PATH, defaults.getEac3toPath());
	}

	@Override
	public String getFfmpegPath() {
		return stringFromConfigFile(KEY_FFMPEG_PATH, defaults.getFfmpegPath());
	}

	@Override
	public String getFlacPath() {
		return stringFromConfigFile(KEY_FLAC_PATH, defaults.getFlacPath());
	}

	@Override
	public String getMencoderPath() {
		return stringFromConfigFile(KEY_MENCODER_PATH, defaults.getMencoderPath());
	}

	@Override
	public String getMencoderMTPath() {
		return stringFromConfigFile(KEY_MENCODERMT_PATH, defaults.getMencoderMTPath());
	}

	@Override
	public String getMencoderOlderPath() {
		return stringFromConfigFile(KEY_MENCODEROLDER_PATH, defaults.getMencoderOlderPath());
	}

	@Override
	public String getMencoderOlderMTPath() {
		return stringFromConfigFile(KEY_MENCODEROLDERMT_PATH, defaults.getMencoderOlderMTPath());
	}

	@Override
	public String getMplayerPath() {
		return stringFromConfigFile(KEY_MPLAYER_PATH, defaults.getMplayerPath());
	}

	@Override
	public String getTsmuxerPath() {
		return stringFromConfigFile(KEY_TSMUXER_PATH, defaults.getTsmuxerPath());
	}

	@Override
	public String getVlcPath() {
		return stringFromConfigFile(KEY_VLC_PATH, defaults.getVlcPath());
	}

	private String stringFromConfigFile(String key, String def) {
		String value = configuration.getString(key);
		return StringUtils.isNotBlank(value) ? value : def;
	}

	@Override
	public String getDCRaw() {
		return stringFromConfigFile(KEY_DCRAW, defaults.getDCRaw());
	}

}