/**
 * 
 */

package org.jenkinsci.plugins.periodicbackup;

import static hudson.Functions.isWindows;

import com.google.common.collect.Iterables;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;


/**
 * @author marc
 * 
 */
@RunWith(Parameterized.class)
public class FullBackupTest {

	private static final File BASE_DIR = new File("src/test/resources/test-basedir/");
	private static final File CONFIG_XML = new File(BASE_DIR, "config.xml");
	private static final File BUILD_XML = new File(BASE_DIR, "jobs/myjob/builds/1/build.xml");
	private static final File JOB_CONFIG_XML = new File(BASE_DIR, "jobs/myjob/config.xml");
	private static final File NEXT_BUILD_NUMBER = new File(BASE_DIR, "jobs/myjob/nextBuildNumber");
	private static final File PLUGIN = new File(BASE_DIR, "plugins/periodicbackup.jpl");
	private static final File SOFTLINK = new File(BASE_DIR, "soft-link-to-source.txt");

	private static final List<File> ALL_FILES = Arrays.asList(
			CONFIG_XML,
			BUILD_XML,
			JOB_CONFIG_XML,
			NEXT_BUILD_NUMBER,
			PLUGIN
		);

	@Before
	public void unixOnly() {
		Assume.assumeFalse(isWindows());
	}

	@Parameters(name = "{0}")
	public static List<Object[]> getTestData() {
		List<Object[]> testData = new ArrayList<Object[]>();

		// test with <null> includes and excludes
		testData.add(new Object[] { null, null, false,
				ALL_FILES });

		// test with empty includes and excludes
		testData.add(new Object[] { "", "", false,
				ALL_FILES });

		// test with blank excludesString
		testData.add(new Object[] { "    ", "    ", false,
				ALL_FILES });

		// test include all
		testData.add(new Object[] { "**", null, false,
				ALL_FILES });

		// test include config.xml
		testData.add(new Object[] { "config.xml", null, false,
				Arrays.asList(CONFIG_XML) });

		// test include all xml files
		testData.add(new Object[] { "**/*.xml", null, false,
				Arrays.asList(CONFIG_XML, BUILD_XML, JOB_CONFIG_XML) });

		// test exclude all
		testData.add(new Object[] { null, "**", false,
				Collections.emptyList() });

		// test exclude config.xml
		testData.add(new Object[] { null, "config.xml", false,
				Arrays.asList(BUILD_XML, JOB_CONFIG_XML, NEXT_BUILD_NUMBER, PLUGIN) });

		// test exclude all xml files
		testData.add(new Object[] { null, "**/*.xml", false,
				Arrays.asList(NEXT_BUILD_NUMBER, PLUGIN) });

		// test exclude jobs files
		testData.add(new Object[] { null, "jobs/", false,
				Arrays.asList(CONFIG_XML, PLUGIN) });

		// test exclude jobs-build files
		testData.add(new Object[] { null, "jobs/*/builds/", false,
				Arrays.asList(CONFIG_XML, JOB_CONFIG_XML, NEXT_BUILD_NUMBER, PLUGIN) });

		// test exclude jobs-build files and nextBuildNumber
		testData.add(new Object[] { null, "jobs/*/builds/; **/nextBuildNumber", false,
				Arrays.asList(CONFIG_XML, JOB_CONFIG_XML, PLUGIN) });

		// test include all xml files except config.xml
		testData.add(new Object[] { "**/*.xml", "config.xml", false,
				Arrays.asList(BUILD_XML, JOB_CONFIG_XML) });

		// test with follow symbolic links enabled
		testData.add(new Object[] { null, "jobs/*/builds/; **/nextBuildNumber", true,
				Arrays.asList(CONFIG_XML, JOB_CONFIG_XML, PLUGIN, SOFTLINK) });

		return testData;
	}

	private String includesString;
	private String excludesString;
	private boolean followSymbolicLinks;
	private List<File> expectedFiles;

	public FullBackupTest(String includesString, String excludesString, boolean followSymbolicLinks, List<File> expectedFiles) {
		this.includesString = includesString;
		this.excludesString = excludesString;
		this.followSymbolicLinks = followSymbolicLinks;
		this.expectedFiles = expectedFiles;
	}

	@Test
	public void test() {
		FullBackup fullBackup = new FullBackup(includesString, excludesString, followSymbolicLinks, BASE_DIR);
		List<File> filesToBackup = asList(fullBackup.getFilesToBackup());
		Assert.assertThat(filesToBackup, Matchers.equalTo(expectedFiles));
	}

	private List<File> asList(Iterable<File> iterable) {
		return Arrays.asList(Iterables.toArray(iterable, File.class));
	}
}
