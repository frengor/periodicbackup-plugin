/*
 * The MIT License
 *
 * Copyright (c) 2010 - 2011, Tomasz Blaszczynski, Emanuele Zattin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.periodicbackup;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import hudson.Extension;
import hudson.model.Hudson;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;

/**
 *
 * This implementation of FileManager will only select the .xml files from the Jenkins homedir
 * and the config.xml files of all the jobs and users during backup.
 * During restore it will try to overwrite the existing files.
 */
public class ConfigOnly extends FileManager {

    private static final Logger LOGGER = Logger.getLogger(ConfigOnly.class.getName());

    @DataBoundConstructor
    public ConfigOnly() {
        super();
        this.restorePolicy = new OverwriteRestorePolicy();
    }

    public String getDisplayName() {
        return "ConfigOnly";
    }

    @Override
    public Iterable<File> getFilesToBackup() throws PeriodicBackupException {
        File rootDir = Jenkins.getActiveInstance().getRootDir();
        List<File> filesToBackup = Lists.newArrayList();
        addRootFiles(rootDir, filesToBackup);
        addJobFiles(rootDir, filesToBackup);
        addUserFiles(rootDir, filesToBackup);
        return filesToBackup;
    }

    private void addRootFiles(File rootDir, List<File> filesToBackup) throws PeriodicBackupException {
        // First find the xml files in the home directory
        final File[] xmlsInRoot = Util.listFiles(rootDir, Util.extensionFileFilter("xml"));
        filesToBackup.addAll(Arrays.asList(xmlsInRoot));
    }

    private void addJobFiles(File rootDir, List<File> filesToBackup) throws PeriodicBackupException {
        File jobsDir = new File(rootDir, "jobs");
        if (jobsDir.exists() && jobsDir.isDirectory()) {
            // Each job directory should have a config.xml file
            File[] dirsInJobs = Util.listFiles(jobsDir, FileFilterUtils.directoryFileFilter());
            
            for (File job : dirsInJobs) {
                addFilesRecursively(job, filesToBackup); // fren_gor - Backup every jobs files

                /* fren_gor - Backup every jobs files
                File jobConfig = new File(job, "config.xml");
                if (jobConfig.exists() && jobConfig.isFile()) {
                    filesToBackup.add(jobConfig);
                    // There might be some config file from the Promotion plugin
                    File promotionDir = new File(job, "promotions");
                    if (promotionDir.exists()) {
                        try {
                            File[] promotionDirs = Util.listFiles(promotionDir, FileFilterUtils.directoryFileFilter());
                            for (File dir : promotionDirs) {
                                File promotionConfig = new File(dir, "config.xml");
                                if (promotionConfig.exists() && promotionConfig.isFile()) {
                                    filesToBackup.add(promotionConfig);
                                }
                            }
                        } catch(PeriodicBackupException ex) {
                            LOGGER.log(Level.WARNING, "Skipping the promotions for Job directory " + promotionDir.getAbsolutePath(), ex);
                        }
                    }
                }
                else {
                    LOGGER.warning(jobConfig.getAbsolutePath() + " does not exist or is not a file.");
                }
                 */
            }
        }
    }

    // fren_gor - Start
    private void addFilesRecursively(File file, List<File> filesToBackup) throws PeriodicBackupException {
        if (file == null || !file.exists()) {
            return;
        }

        if (file.isDirectory()) {
            File[] filesInFile =  Util.listFiles(file);
            for (File fileOfFile : filesInFile) {
                addFilesRecursively(fileOfFile, filesToBackup);
            }
        } else if (file.isFile()) {
            filesToBackup.add(file);
        }
    }
    // fren_gor - End

    private void addUserFiles(File rootDir, List<File> filesToBackup) throws PeriodicBackupException {
        File usersDir = new File(rootDir, "users");
        if (usersDir.exists() && usersDir.isDirectory()) {

            // fren_gor - Start
            File usersXML = new File(usersDir, "users.xml");
            if (usersXML.exists()) {
                filesToBackup.add(usersXML);
            }
            // fren_gor - End

            // Each user directory should have a config.xml file
            File[] dirsInUsers =  Util.listFiles(usersDir, FileFilterUtils.directoryFileFilter());
            for (File user : dirsInUsers) {
                File userConfig = new File(user, "config.xml");
                if (userConfig.exists() && userConfig.isFile()) {
                    filesToBackup.add(userConfig);
                }
                else {
                    LOGGER.warning(userConfig.getAbsolutePath() + " does not exist or is not a file.");
                }
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ConfigOnly) {
            ConfigOnly that = (ConfigOnly) o;
            return Objects.equal(this.restorePolicy, that.restorePolicy);
        }
        return false;

    }

    @Override
    public int hashCode() {
        return 97;
    }

    @SuppressWarnings("unused")
    @Extension
    public static class DescriptorImpl extends FileManagerDescriptor {
        public String getDisplayName() {
            return "ConfigOnly";
        }
    }
}
