
package com.james.uicomparerunner;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import com.james.uicomparerunner.c.Constants;
import com.james.uicomparerunner.res.R;
import com.james.uicomparerunner.ui.RecorderEditFrame;
import com.james.uicomparerunner.ui.ScriptConcatenateFrame;
import com.james.uicomparerunner.ui.UiCompareFrame;
import com.james.uicomparerunner.ui.UiCompareFrame.OnReplaceClickListener;
import com.james.uicomparerunner.ui.dialog.DialogBuilder;
import com.james.uicomparerunner.ui.uiinterface.OnWindowCloseListener;
import com.james.uicomparerunner.utils.FileUtils;
import com.james.uicomparerunner.utils.HtmlGenerator;
import com.james.uicomparerunner.utils.PropertyUtils;
import com.james.uicomparerunner.utils.ScriptGenerator;
import com.james.uicomparerunner.utils.SystemUtils;
import com.james.uicomparerunner.utils.SystemUtils.OnExecCallBack;

public class UICompareRunner {
	public static String android_sdk_path = null;
	public static String adb = "/platform-tools/adb";
	public static String monkey_runner = "/tools/monkeyrunner";
	public static String monkey_runner_ext_lib = "/tools/lib/monkeyrunner_ext.jar";
	public static String monkey_recorder_file_path = "ui_recorder.py";
	public static String dir_device = "%s";
	public static String dir_device_picture = "%s/%s/sreenshot";
	public static String dir_device_target_picture = "%s/%s/sreenshot/target";
	public static String dir_device_test_picture = "%s/%s/sreenshot/test";
	public static String dir_device_result_picture = "%s/%s/sreenshot/result";
	public static String dir_device_script = "%s/script";

	public static String package_name = "gogolook.callgogolook2";

	private static UiCompareFrame uiCompareFrame;

	private static RecorderEditFrame recorderEditFrame;

	private static ScriptConcatenateFrame scriptConcatenateFrame;

	public static void main(String args[]) {

		SystemUtils.init();

		initUI();

		setPath();

		setDefaultDevice(false);

		initProperDir(null);

		showQuikActionSelectDialog(true, R.string.dialog_alert_run_last_script);

		PropertyUtils.saveProperty(PropertyUtils.KEY_VERSION, Constants.VERSION);
	}

	private static void initUI() {
		uiCompareFrame = new UiCompareFrame(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JMenuItem menuItem = (JMenuItem) e.getSource();
				if (menuItem.getText().equalsIgnoreCase(R.string.menu_file_close)) {
					//
					if (recorderEditFrame != null && recorderEditFrame.isShowing()) {
						recorderEditFrame.dispose();
					}
					else if (scriptConcatenateFrame != null && scriptConcatenateFrame.isShowing()) {
						scriptConcatenateFrame.dispose();
					}
					else if (uiCompareFrame.isEditorShown()) {
						recorderEditFrame.dispose();
					}
					else {
						System.exit(0);
					}
				}
				else if (menuItem.getText().equalsIgnoreCase(R.string.menu_file_generate_script)) {
					//
					recordNewAction();
				}
				else if (menuItem.getText().equalsIgnoreCase(R.string.menu_file_edit_recorder)) {
					//
					loadRecorderAndEdit(null);
				}
				else if (menuItem.getText().equalsIgnoreCase(R.string.menu_file_choose_script)) {
					//
					loadScriptAndRun(null);
				}
				else if (menuItem.getText().equalsIgnoreCase(R.string.menu_file_run_last_script)) {
					//
					startMonkeyRunner();
				}
				else if (menuItem.getText().equalsIgnoreCase(R.string.menu_file_show_last_result)) {
					//
					generateResult();
				}
				else if (menuItem.getText().equalsIgnoreCase(R.string.menu_file_clear)) {
					//
					uiCompareFrame.removeAll();
				}
				else if (menuItem.getText().equalsIgnoreCase(R.string.menu_device_reset_device)) {
					//
					setDefaultDevice(true);
				}
				else if (menuItem.getText().equalsIgnoreCase(R.string.menu_device_reset_package_name)) {
					//
					inputPackageName();
				}
				else if (menuItem.getText().equalsIgnoreCase(R.string.menu_open_editor)) {
					uiCompareFrame.checkSharedPreference(package_name);
				}

			}
		});

		uiCompareFrame.setOnReplaceClickListener(new OnReplaceClickListener() {

			@Override
			public void onReplace(String origin, String target) {
				int re = DialogBuilder.showConfirmDialog(uiCompareFrame, R.string.dialog_alert_set_as_target);
				if (re != 0)
					return;

				try {
					FileUtils.copyFileFromFileToFile(new File(origin), new File(target));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

				DialogBuilder.showMessageDialog(uiCompareFrame, R.string.dialog_alert_set_as_target_success);
				//
				generateResult();
			}
		});
	}

	private static void setPath() {
		if (!PropertyUtils.existProperty(PropertyUtils.KEY_SDK_PATH)) {
			DialogBuilder.showSettingSDKPathDialog(uiCompareFrame);
			setPath();
			return;
		}

		android_sdk_path = PropertyUtils.loadProperty(PropertyUtils.KEY_SDK_PATH, PropertyUtils.NULL);

		monkey_runner = new File(android_sdk_path + monkey_runner).getAbsolutePath();
		if (android_sdk_path.equalsIgnoreCase(PropertyUtils.NULL) ||
				(!new File(monkey_runner).exists() && !new File(monkey_runner + ".bat").exists())) {
			monkey_runner = "/tools/monkeyrunner";
			PropertyUtils.deleteProperty(PropertyUtils.KEY_SDK_PATH);
			setPath();
			return;
		}

		// set path of adb.exe
		adb = new File(android_sdk_path + adb).getAbsolutePath();

		// check monkey_runner_ext_lib exist or not
		if (!PropertyUtils.loadProperty(PropertyUtils.KEY_VERSION, "1.0.0").equalsIgnoreCase(Constants.VERSION) || !new File(android_sdk_path + monkey_runner_ext_lib).exists()) {
			try {
				FileUtils.copyFileFromFileToFile(new File("monkeyrunner_ext"), new File(android_sdk_path + monkey_runner_ext_lib));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				System.exit(-1);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}

		//
		monkey_recorder_file_path = new File(monkey_recorder_file_path).getAbsolutePath();
	}

	private static void setDefaultDevice(boolean reset) {
		final String originDevice = PropertyUtils.loadProperty(PropertyUtils.KEY_DEVICE, PropertyUtils.NULL);
		if (reset)
			PropertyUtils.deleteProperty(PropertyUtils.KEY_DEVICE);

		//
		SystemUtils.exec(adb + " " + "devices", new OnExecCallBack() {

			@Override
			public void onExec(String response, String error) {
				if (!error.equalsIgnoreCase("")) {
					DialogBuilder.showMessageDialog(uiCompareFrame, error);
					return;
				}

				String[] splits = response.split("\n");
				ArrayList<String> devices = new ArrayList<String>();
				for (String split : splits) {
					if (split.contains("	")) {
						String device = split.split("	")[0];
						devices.add(device);
					}
				}

				if (devices.contains(PropertyUtils.loadProperty(PropertyUtils.KEY_DEVICE, PropertyUtils.NULL))) {
					return;
				}

				Object[] possibilities = devices.toArray();

				String selectDevice = DialogBuilder.showDeviceSelectDialog(uiCompareFrame, possibilities);
				if (selectDevice != null)
					PropertyUtils.saveProperty(PropertyUtils.KEY_DEVICE, selectDevice);
				else
					PropertyUtils.saveProperty(PropertyUtils.KEY_DEVICE, originDevice);
			}
		});
	}

	private static void recordNewAction() {
		String device = PropertyUtils.loadProperty(PropertyUtils.KEY_DEVICE, PropertyUtils.NULL);
		if (device.equalsIgnoreCase(PropertyUtils.NULL)) {
			setDefaultDevice(true);
			recordNewAction();
			return;
		}

		//
		setLabelText("start record actions...");
		SystemUtils.exec(monkey_runner + " " + monkey_recorder_file_path + " " + device + " " + "record" + " " + dir_device, null);
		//
		setLabelText("stop record actions...");
		SystemUtils.exec(monkey_runner + " " + monkey_recorder_file_path + " " + device + " " + "close" + " " + dir_device, null);
		//
		// SystemUtils.exec(adb + " -s " + device + " usb", null);
		//
		generateScript();
	}

	private static void generateScript() {
		setLabelText("generate a script file.");
		String mrPath = DialogBuilder.showFindActionFileDialog(uiCompareFrame);

		if (mrPath == null) {
			setLabelText("cancel generating script.");
			return;
		}

		if (!new File(mrPath).exists()) {
			DialogBuilder.showMessageDialog(uiCompareFrame, R.string.dialog_alert_file_disappear);
			return;
		}
		setLabelText("select file: " + mrPath);

		String monkey_runner_file_path = null;
		try {
			initProperDir(mrPath);
			monkey_runner_file_path = ScriptGenerator.getScriptFilePath(mrPath);
		} catch (IOException e) {
			e.printStackTrace();
		}

		//
		if (monkey_runner_file_path == null || !new File(monkey_runner_file_path).exists()) {
			return;
		}
		PropertyUtils.saveProperty(PropertyUtils.KEY_LAST_SCRIPT, monkey_runner_file_path);

		int re = DialogBuilder.showConfirmDialog(uiCompareFrame, R.string.dialog_alert_create_script_ok);
		if (re == 1) {
			//
			int select = DialogBuilder.showConfirmDialog(uiCompareFrame, R.string.dialog_alert_run_script);
			if (select == 0) {
				startMonkeyRunner();
			}
		}
		else {
			//
			loadRecorderAndEdit(mrPath);
		}
	}

	private static void loadRecorderAndEdit(String recorderPath) {
		final String mrPath = recorderPath == null ? DialogBuilder.showFindRecorderFileDialog(uiCompareFrame) : recorderPath;
		if (mrPath == null || !new File(mrPath).exists()) {
			DialogBuilder.showMessageDialog(uiCompareFrame, R.string.dialog_alert_file_disappear);
			return;
		}

		recorderEditFrame = new RecorderEditFrame(new OnWindowCloseListener() {

			@Override
			public void onWindowClosing(String... output) {
				loadScriptAndRun(output[0]);
			}
		});

		try {
			recorderEditFrame.setRecorder(mrPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void loadScriptAndRun(String defaultScriptFile) {
		scriptConcatenateFrame = new ScriptConcatenateFrame(uiCompareFrame, defaultScriptFile, new OnWindowCloseListener() {

			@Override
			public void onWindowClosing(String... output) {
				if (output == null || output.length == 0) {
					return;
				}

				String monkey_runner_file_path = null;
				for (String path : output) {
					if (monkey_runner_file_path == null)
						monkey_runner_file_path = path;
					else
						monkey_runner_file_path = monkey_runner_file_path + "," + path;
				}

				PropertyUtils.saveProperty(PropertyUtils.KEY_LAST_SCRIPT, monkey_runner_file_path);

				showQuikActionSelectDialog(false, R.string.dialog_alert_run_script);
			}
		});
	}

	private static void startMonkeyRunner() {
		//
		final String device = PropertyUtils.loadProperty(PropertyUtils.KEY_DEVICE, PropertyUtils.NULL);
		if (device.equalsIgnoreCase(PropertyUtils.NULL)) {
			setDefaultDevice(true);
			startMonkeyRunner();
			return;
		}

		String path = PropertyUtils.loadProperty(PropertyUtils.KEY_LAST_SCRIPT, PropertyUtils.NULL);

		if (path.equalsIgnoreCase(PropertyUtils.NULL)) {
			return;
		}

		String[] monkey_runner_file_path = path.split(",");

		for (String currentPath : monkey_runner_file_path) {
			if (currentPath == null || !new File(currentPath).exists()) {
				DialogBuilder.showMessageDialog(uiCompareFrame, R.string.dialog_alert_file_disappear);
				return;
			}
		}

		uiCompareFrame.removeAll();

		// TODO
		monitorLogcat();

		for (String currentPath : monkey_runner_file_path) {

			initProperDir(currentPath);

			FileUtils.deletePicturesInDirectory(new File(dir_device_picture));

			//
			setLabelText("start running " + new File(currentPath).getName() + " on " + device + ", please wait.");
			SystemUtils.exec(monkey_runner + " " + currentPath + " " + device + " " + dir_device_picture, null);

			FileUtils.deletePicturesInDirectory(new File(dir_device_test_picture));
			FileUtils.copyFilesFromDirToDir(dir_device_picture, dir_device_test_picture);

			String[] testPictures = new File(dir_device_test_picture).list();
			ArrayList<String> testPictureList = new ArrayList<String>(Arrays.asList(testPictures));
			String[] targetPictures = new File(dir_device_target_picture).list();
			ArrayList<String> targetPictureList = new ArrayList<String>(Arrays.asList(targetPictures));
			for (int i = 0; i < testPictureList.size(); i++) {
				String fileName = testPictureList.get(i);
				if (!targetPictureList.contains(testPictureList.get(i))) {
					try {
						File fromFile = new File(dir_device_test_picture + File.separator + fileName);
						File toFile = new File(dir_device_target_picture + File.separator + fileName);
						FileUtils.copyFileFromFileToFile(fromFile, toFile);
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

			FileUtils.deletePicturesInDirectory(new File(dir_device_picture));
		}

		generateResult();

	}

	private static void generateResult() {
		setLabelText("generating result...");

		String path = PropertyUtils.loadProperty(PropertyUtils.KEY_LAST_SCRIPT, PropertyUtils.NULL);

		if (path.equalsIgnoreCase(PropertyUtils.NULL)) {
			return;
		}

		String[] monkey_runner_file_path = path.split(",");

		for (String currentPath : monkey_runner_file_path) {
			if (currentPath == null || !new File(currentPath).exists()) {
				DialogBuilder.showMessageDialog(uiCompareFrame, R.string.dialog_alert_file_disappear);
				return;
			}
		}

		uiCompareFrame.removeAll();

		for (String currentPath : monkey_runner_file_path) {
			setLabelText("generating result for " + currentPath);

			initProperDir(currentPath);

			File resultDir = new File(dir_device_result_picture);
			if (!resultDir.exists())
				resultDir.mkdir();
			else
				FileUtils.deletePicturesInDirectory(resultDir);

			File targetDir = new File(dir_device_target_picture);
			File testDir = new File(dir_device_test_picture);

			int imageWidth = 360;
			int imageHeight = 640;
			for (String fileName : targetDir.list()) {
				for (String fileName2 : testDir.list()) {
					if (!fileName.equalsIgnoreCase(fileName2))
						continue;

					String path1 = targetDir.getAbsolutePath() + File.separator + fileName;
					String path2 = testDir.getAbsolutePath() + File.separator + fileName;
					String path3 = resultDir.getAbsolutePath() + File.separator + fileName;

					try {
						BufferedImage targetBuffered = ImageIO.read(new File(path1));
						BufferedImage testBuffered = ImageIO.read(new File(path2));
						BufferedImage resultBuffered = ImageIO.read(new File(path1));
						File resultFile = new File(path3);

						Color red = new Color(255, 0, 0);

						for (int i = 0; i < targetBuffered.getWidth(); i++) {
							for (int j = 0; j < targetBuffered.getHeight(); j++) {
								if (targetBuffered.getRGB(i, j) != testBuffered.getRGB(i, j)) {
									resultBuffered.setRGB(i, j, red.getRGB());
								}
								else {
									Color color = new Color(testBuffered.getRGB(i, j));
									resultBuffered.setRGB(i, j, color.darker().darker().getRGB());
								}

							}
						}

						imageWidth = targetBuffered.getWidth();
						imageHeight = targetBuffered.getHeight();

						ImageIO.write(resultBuffered, "png", resultFile);

						targetBuffered.flush();
						targetBuffered = null;
						testBuffered.flush();
						testBuffered = null;
						resultBuffered.flush();
						resultBuffered = null;
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

			try {
				HtmlGenerator.createHtml(imageWidth / 3, imageHeight / 3);
			} catch (IOException e) {
				e.printStackTrace();
			}

			try {
				uiCompareFrame.showComparPictures(targetDir, testDir, resultDir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		setLabelText("complete!");
	}

	//
	private static boolean initProperDir(String monkey_runner_file_path) {
		//
		String deviceName = PropertyUtils.loadProperty(PropertyUtils.KEY_DEVICE, PropertyUtils.NULL);
		if (deviceName.equalsIgnoreCase(PropertyUtils.NULL)) {
			DialogBuilder.showMessageDialog(uiCompareFrame, R.string.dialog_alert_choose_a_device);
			setDefaultDevice(true);

			return false;
		}

		dir_device = "%s";
		dir_device = new File(String.format(dir_device, deviceName)).getAbsolutePath();
		if (!new File(dir_device).exists()) {
			new File(dir_device).mkdirs();
		}

		//
		dir_device_script = "%s/script";
		dir_device_script = new File(String.format(dir_device_script, deviceName)).getAbsolutePath();
		if (!new File(dir_device_script).exists()) {
			new File(dir_device_script).mkdirs();
		}

		if (monkey_runner_file_path == null || !new File(monkey_runner_file_path).exists()) {
			return false;
		}
		//
		dir_device_picture = "%s/%s/sreenshot";
		dir_device_target_picture = "%s/%s/sreenshot/target";
		dir_device_test_picture = "%s/%s/sreenshot/test";
		dir_device_result_picture = "%s/%s/sreenshot/result";
		String scriptName = new File(monkey_runner_file_path).getName().replace(".mr", "").replace(".py", "");

		dir_device_picture = new File(String.format(dir_device_picture, deviceName, scriptName)).getAbsolutePath();
		if (!new File(dir_device_picture).exists())
			new File(dir_device_picture).mkdirs();
		dir_device_target_picture = new File(String.format(dir_device_target_picture, deviceName, scriptName)).getAbsolutePath();
		if (!new File(dir_device_target_picture).exists())
			new File(dir_device_target_picture).mkdirs();
		dir_device_test_picture = new File(String.format(dir_device_test_picture, deviceName, scriptName)).getAbsolutePath();
		if (!new File(dir_device_test_picture).exists())
			new File(dir_device_test_picture).mkdirs();
		dir_device_result_picture = new File(String.format(dir_device_result_picture, deviceName, scriptName)).getAbsolutePath();
		if (!new File(dir_device_result_picture).exists())
			new File(dir_device_result_picture).mkdirs();
		return true;
	}

	private static void showQuikActionSelectDialog(boolean checkFile, String message) {
		//
		if (checkFile) {
			String lastScriptPath = PropertyUtils.loadProperty(PropertyUtils.KEY_LAST_SCRIPT, PropertyUtils.NULL);
			//
			if (lastScriptPath.equalsIgnoreCase(PropertyUtils.NULL)) {
				int re = DialogBuilder.showConfirmDialog(uiCompareFrame, R.string.dialog_alert_create_a_new_script);
				if (re == 0) {
					recordNewAction();
				}
				return;
			}
		}

		//
		int select = DialogBuilder.showConfirmDialog(uiCompareFrame, R.string.dialog_title_quikly_choose, message);
		if (select == 0) {
			startMonkeyRunner();
		}
	}

	public static void inputPackageName() {
		String input = JOptionPane.showInputDialog(R.string.dialog_alert_input_package_name, package_name.equalsIgnoreCase("gogolook.callgogolook2") ? "(Default)" : package_name);
		if (input == null || input.equalsIgnoreCase("")) {
			return;
		}
		if (input.equalsIgnoreCase("(Default)")) {
			package_name = "gogolook.callgogolook2";
		}
		else {
			package_name = input;
		}

		final ArrayList<String> packaeNames = new ArrayList<String>();
		SystemUtils.exec(UICompareRunner.adb + " shell pm list packages", new OnExecCallBack() {

			@Override
			public void onExec(String response, String error) {
				String[] packages = response.split("\n");
				packaeNames.addAll(new ArrayList<String>(Arrays.asList(packages)));
			}
		});

		if (!packaeNames.contains("package:" + package_name)) {
			DialogBuilder.showMessageDialog(uiCompareFrame, R.string.dialog_alert_package_name_error);
			package_name = "gogolook.callgogolook2";
			return;
		}
	}

	public static void setLabelText(String text) {
		uiCompareFrame.setLabelText(text);
	}

	private static Thread logcatThread;

	private static void monitorLogcat() {
		if (logcatThread != null && logcatThread.isAlive()) {
			logcatThread.interrupt();
			logcatThread = null;
		}

		logcatThread = new Thread(new Runnable() {

			@Override
			public void run() {
				final String device = PropertyUtils.loadProperty(PropertyUtils.KEY_DEVICE, PropertyUtils.NULL);
				// adb -d logcat com.example.example:I *:S
				// adb logcat | grep adb shell ps | grep your.package.name | cut -c10-15
				SystemUtils.exec(adb + " -s " + device + " logcat System.err:W *:S", null);
			}
		});
		logcatThread.start();
	}
}
