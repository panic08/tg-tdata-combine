package ru.marthastudios.worker.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;
import ru.marthastudios.worker.api.UnloadApi;
import ru.marthastudios.worker.api.payload.CheckOnExistResponse;
import ru.marthastudios.worker.property.WorkerProperty;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkerScheduler {
    private final WorkerProperty workerProperty;
    private final UnloadApi unloadApi;

    @Scheduled(fixedRate = 6000)
    public void handleWorkScheduler() {
        ExecutorService executorService = Executors.newFixedThreadPool(4);

        CheckOnExistResponse checkOnExistResponse =
                unloadApi.checkOnExist(workerProperty.getId());

        if (!checkOnExistResponse.getIsExist()) {
            log.info("No unload task for our worker");
            return;
        }

        log.info("Unload task is founded, starting work");

        log.info("Create temp directory");

        File tempProperty = null;

        try {
            tempProperty = Files.createTempDirectory("tempProperty").toFile();
        } catch (IOException e) {
            log.error(e.getMessage());
        }

        log.info("Get all tData files by directory");

        File[] tDataFiles = new File(workerProperty.getTDataPath()).listFiles();

        assert tDataFiles != null;

        log.info("Starting check all tDataFiles");

        System.out.println(tempProperty.getPath());

        for (File tDataFile : tDataFiles) {
            File finalTempProperty = tempProperty;
            executorService.submit(() -> {
                Long tDataFileTime = null;

                try {
                    tDataFileTime = Files.readAttributes(Path.of(tDataFile.getPath()), BasicFileAttributes.class)
                            .creationTime().toMillis();
                } catch (IOException e) {
                    log.error(e.getMessage());
                }

//                if (System.currentTimeMillis() - tDataFileTime > 72 * 60 * 60 * 1000)

                File emojiPropertyToDeleteFile = new File(tDataFile.getPath() + "\\tdata\\emoji");

                if (emojiPropertyToDeleteFile.exists()) {
                    log.info("Deleting emojiProperty in tData");

                    deleteDirectory(emojiPropertyToDeleteFile);
                }

                File tDataProperty = null;

                try {
                    tDataProperty = Files.createDirectory(Path.of(finalTempProperty.getPath() + "\\" + tDataFile.getName())).toFile();

                    FileSystemUtils.copyRecursively(tDataFile, tDataProperty);
                } catch (IOException e) {
                    log.error(e.getMessage());
                }

                deleteDirectory(tDataFile);
            });
        }

        executorService.shutdown();

        while (!executorService.isTerminated()) {
        }

        File zipFileTemp = null;

        try {
            zipFileTemp = File.createTempFile("zipFileTemp", ".zip");

            FileOutputStream fos = new FileOutputStream(zipFileTemp);
            ZipOutputStream zipOut = new ZipOutputStream(fos);

            zipFolder(tempProperty.getPath(), zipFileTemp.getPath());

            zipOut.close();
            fos.close();
        } catch (IOException e) {
            log.error(e.getMessage());
        }

        deleteDirectory(tempProperty);

        unloadApi.create(workerProperty.getId(), zipFileTemp);

        zipFileTemp.delete();
        log.info("Ending unload working");
    }
    private static boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }

    public static void zipFolder(String sourceFolder, String zipFileName) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipFileName);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            File sourceFile = new File(sourceFolder);

            addFilesToZip(sourceFile, sourceFile.getName(), zos);
        }
    }

    private static void addFilesToZip(File folder, String parentFolder, ZipOutputStream zos) throws IOException {
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                addFilesToZip(file, parentFolder + "/" + file.getName(), zos);
                continue;
            }

            FileInputStream fis = new FileInputStream(file);
            String entryPath = parentFolder + "/" + file.getName();
            ZipEntry zipEntry = new ZipEntry(entryPath);
            zos.putNextEntry(zipEntry);

            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, bytesRead);
            }

            fis.close();
            zos.closeEntry();
        }
    }
}
