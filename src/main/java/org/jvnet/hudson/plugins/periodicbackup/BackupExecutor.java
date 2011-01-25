package org.jvnet.hudson.plugins.periodicbackup;

import com.google.common.collect.Sets;
import hudson.util.DescribableList;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Set;

public class BackupExecutor {

    private Set<File> filesToBackup = Sets.newHashSet();

    public void backup(DescribableList<FileManager, FileManagerDescriptor> fileManagers,
                       DescribableList<Storage, StorageDescriptor> storages,
                       DescribableList<Location, LocationDescriptor> locations,
                       String tempDirectory) throws IOException {
        //collecting files for backup  TODO: if we specify that there can be only one FileManager it's redundant
        for(FileManager fm: fileManagers) {
            for(File f: fm.getFilesToBackup()) {
                filesToBackup.add(f);
            }
        }
        File backupObjectFile;
        //creating backup archives for each storage defined
        Date timestamp = new Date();
        String fileNameBase = Util.generateFileNameBase(timestamp);
        for(Storage storage: storages) {
            //TODO: if we want to store serialized BackupObject File inside archive this has to be changed
            Iterable<File> archives = storage.archiveFiles(filesToBackup, tempDirectory, fileNameBase);
            //sends all backup archives and backup files to all activated locations
            for(Location location: locations) {
                //here I assumed 1 and only 1 FileManager
                BackupObject backupObject = new BackupObject(fileManagers.iterator().next(), storage, location, timestamp);
                backupObjectFile = Util.createBackupObjectFile(backupObject, tempDirectory, fileNameBase);
                location.storeBackupInLocation(archives, backupObjectFile);
                System.out.println("[INFO] Deleting temporary file " + backupObjectFile.getAbsolutePath()); //TODO: logger instead
                if(!backupObjectFile.delete()) {
                    System.out.println("[WARNING] Could not delete " + backupObjectFile.getAbsolutePath()); //TODO: logger instead
                }
            }
            for(File f: archives) {
                System.out.println("[INFO] Deleting temporary file " + f.getAbsolutePath()); //TODO: logger instead
                if(!f.delete()) {
                    System.out.println("[WARNING] Could not delete " + f.getAbsolutePath()); //TODO: logger instead
                }
            }
        }
    }
}
