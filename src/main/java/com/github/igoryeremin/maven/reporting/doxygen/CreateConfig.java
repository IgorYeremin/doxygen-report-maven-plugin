package com.github.igoryeremin.maven.reporting.doxygen;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Creates Doxygen config file by running
 * <code>doxygen -g <em>doxygenConf</em></code>.
 */
@Mojo(name = "create-config", requiresProject = true)
public class CreateConfig extends AbstractMojo {

	/**
	 * Doxygen config file location.
	 */
	@Parameter(defaultValue = "${basedir}/src/doxygen/doxygen.config")
	private File doxygenConf;

	/**
	 * Doxygen executable name.
	 */
	@Parameter(defaultValue = "doxygen")
	private String executableName;

	private static final String[] replacedConfigValues = {
		"PROJECT_NAME ",
		"PROJECT_NUMBER ",
		"OUTPUT_DIRECTORY ",
		"INPUT "
	};

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		Log log = getLog();

		try {
			if (!doxygenConf.exists()) {
				doxygenConf.getParentFile().mkdirs();
			}

			File tempFile = new File(doxygenConf.getPath() + ".tmp");
			{
				ProcessBuilder builder = new ProcessBuilder(executableName, "-g", tempFile.getPath());
				builder.redirectErrorStream(true);
				Process shell = builder.start();

				BufferedReader shellReader = new BufferedReader(new InputStreamReader(shell.getInputStream()));

				String line = null;
				while ((line = shellReader.readLine()) != null) {
					log.info(line);
				}

				int result = shell.waitFor();
				if (result != 0)
					throw new MojoFailureException("Doxygen exited with error code: " + result);
			}
			{
				BufferedReader originalFileReader = new BufferedReader(new FileReader(tempFile));
				BufferedWriter strippedFileWriter = new BufferedWriter(new FileWriter(doxygenConf));

				String line = null;
				while ((line = originalFileReader.readLine()) != null) {
					boolean keep = true;
					for (String i : replacedConfigValues) {
						if (line.startsWith(i)) {
							keep = false;
							break;
						}
					}

					if (keep) {
						strippedFileWriter.write(line);
					} else {
						strippedFileWriter.write("#" + line + "# set by maven plugin");
					}
					strippedFileWriter.newLine();
				}
				originalFileReader.close();
				strippedFileWriter.close();
				tempFile.delete();
			}
		} catch (InterruptedException e) {
			throw new MojoFailureException("Error running Doxygen", e);
		} catch (IOException e) {
			throw new MojoFailureException("Error running Doxygen", e);
		}

	}

}
