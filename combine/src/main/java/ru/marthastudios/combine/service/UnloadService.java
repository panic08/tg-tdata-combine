package ru.marthastudios.combine.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.marthastudios.combine.bot.TelegramBot;
import ru.marthastudios.combine.payload.CheckUnloadOnExistResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class UnloadService {
    public final Set<String> unloadWorkersIdSet = new HashSet<>();
    private final TelegramBot telegramBot;

    @Value("${unload.pathToFilteredSave}")
    private String pathToFilteredSave;

    public void create(String workerId) {
        unloadWorkersIdSet.add(workerId);
    }

    public CheckUnloadOnExistResponse checkOnExist(String workerId) {
        if (unloadWorkersIdSet.contains(workerId)) {
            unloadWorkersIdSet.remove(workerId);

            return new CheckUnloadOnExistResponse(true);
        } else {
            return new CheckUnloadOnExistResponse(false);
        }
    }
    
    public void create(String workerId, MultipartFile multipartFile) {
        Date currentDate = new Date(System.currentTimeMillis());

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
        String formattedDate = dateFormat.format(currentDate);

        File file = new File(pathToFilteredSave + formattedDate + " " + workerId + ".zip");

        try {
            multipartFile.transferTo(file);
        } catch (IOException e) {
            log.error(e.getMessage());
        }

        if (file.length() <= 1000000000L) {
            telegramBot.handleCreateDocumentMessageAllAdmins(workerId, file);
        } else {
            telegramBot.handleCreateMessageAllAdmins(workerId);
        }
    }

}
