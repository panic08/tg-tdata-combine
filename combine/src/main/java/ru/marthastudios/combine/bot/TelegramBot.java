package ru.marthastudios.combine.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.marthastudios.combine.callback.BackCallback;
import ru.marthastudios.combine.callback.UserCallback;
import ru.marthastudios.combine.property.TelegramBotsProperty;
import ru.marthastudios.combine.service.UnloadService;

import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutorService;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {
    @Value("${admin.ids}")
    private String[] adminIds;

    @Autowired
    private TelegramBotsProperty telegramBotsProperty;
    @Autowired
    private ExecutorService executorService;
    @Autowired
    @Lazy
    private UnloadService unloadService;

    private final Set<Long> createUserUnloadSteps = new HashSet<>();

    public TelegramBot(TelegramBotsProperty telegramBotsProperty) {
        this.telegramBotsProperty = telegramBotsProperty;
        List<BotCommand> listOfCommands = new ArrayList<>();

        listOfCommands.add(new BotCommand("/start", "\uD83D\uDD04 Перезапуск"));
        listOfCommands.add(new BotCommand("/unload", "\uD83D\uDCF2 Выгрузить tData"));
        listOfCommands.add(new BotCommand("/checkprofit", "\uD83E\uDDFE Подсчитать профит"));

        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return telegramBotsProperty.getName();
    }

    @Override
    public String getBotToken() {
        return telegramBotsProperty.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        executorService.submit(() -> {
           if (update.hasMessage() && update.getMessage().hasText()) {
               String text = update.getMessage().getText();
               long chatId = update.getMessage().getChatId();

               switch (text) {
                   case "/start" -> {
                       createStartMessage(chatId);
                       return;
                   }

                   case "/unload", "\uD83D\uDCF2 Выгрузить tData" -> {
                       createUnloadMessage(chatId);
                       return;
                   }
               }

               if (createUserUnloadSteps.contains(chatId)) {
                   createUserUnloadSteps.remove(chatId);

                   unloadService.create(text);

                   InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

                   InlineKeyboardButton handleUnloadButton = InlineKeyboardButton.builder()
                           .callbackData(BackCallback.BACK_TO_UNLOAD_CALLBACK_DATA)
                           .text("◀\uFE0F Назад")
                           .build();

                   List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();

                   keyboardButtonsRow1.add(handleUnloadButton);

                   List<List<InlineKeyboardButton>> rowList = new ArrayList<>();

                   rowList.add(keyboardButtonsRow1);

                   inlineKeyboardMarkup.setKeyboard(rowList);

                   SendMessage message = SendMessage.builder()
                           .text("✅ Вы успешно создали задание на выгрузку tData для рабочего <b>" + text + "</b>\n\n"
                           + "⏳ <b>Ожидайте,</b> в скором времени вам придет сообщение о успешной выгрузке файла на сервер")
                           .chatId(chatId)
                           .replyMarkup(inlineKeyboardMarkup)
                           .parseMode("html")
                           .build();

                   try {
                       execute(message);
                   } catch (TelegramApiException e) {
                       log.warn(e.getMessage());
                   }
                   return;
               }
           } else if (update.hasCallbackQuery()) {
               long chatId = update.getCallbackQuery().getMessage().getChatId();
               int messageId = update.getCallbackQuery().getMessage().getMessageId();
               String callbackData = update.getCallbackQuery().getData();

               switch (callbackData) {
                   case UserCallback.HANDLE_UNLOAD_CALLBACK_DATA -> {
                       createUserUnloadSteps.add(chatId);

                       InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

                       InlineKeyboardButton handleUnloadButton = InlineKeyboardButton.builder()
                               .callbackData(BackCallback.BACK_TO_UNLOAD_CALLBACK_DATA)
                               .text("◀\uFE0F Назад")
                               .build();

                       List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();

                       keyboardButtonsRow1.add(handleUnloadButton);

                       List<List<InlineKeyboardButton>> rowList = new ArrayList<>();

                       rowList.add(keyboardButtonsRow1);

                       inlineKeyboardMarkup.setKeyboard(rowList);

                       EditMessageText editMessageText = EditMessageText.builder()
                               .text("▶\uFE0F Введите <b>Worker ID</b> для создания задания на выгрузку tData")
                               .chatId(chatId)
                               .messageId(messageId)
                               .replyMarkup(inlineKeyboardMarkup)
                               .parseMode("html")
                               .build();

                       try {
                           execute(editMessageText);
                       } catch (TelegramApiException e) {
                           log.warn(e.getMessage());
                       }
                   }

                   case BackCallback.BACK_TO_UNLOAD_CALLBACK_DATA -> {
                       createUserUnloadSteps.remove(chatId);

                       editUnloadMessage(chatId, messageId);
                   }
               }
           }
        });
    }

    private ReplyKeyboardMarkup getDefaultReplyKeyboardMarkup() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();

        keyboardMarkup.setSelective(true);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        KeyboardButton unloadTDataButton = new KeyboardButton("\uD83D\uDCF2 Выгрузить tData");
        KeyboardButton checkProfitButton = new KeyboardButton("\uD83E\uDDFE Подсчитать профит");

        KeyboardRow keyboardRow1 = new KeyboardRow();
        KeyboardRow keyboardRow2 = new KeyboardRow();

        keyboardRow1.add(unloadTDataButton);

        keyboardRow2.add(checkProfitButton);

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        keyboardRows.add(keyboardRow1);
        keyboardRows.add(keyboardRow2);

        keyboardMarkup.setKeyboard(keyboardRows);

        return keyboardMarkup;
    }

    private void createUnloadMessage(long chatId) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        InlineKeyboardButton handleUnloadButton = InlineKeyboardButton.builder()
                .callbackData(UserCallback.HANDLE_UNLOAD_CALLBACK_DATA)
                .text("▶\uFE0F Начать выгрузку")
                .build();

        List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();

        keyboardButtonsRow1.add(handleUnloadButton);

        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();

        rowList.add(keyboardButtonsRow1);

        inlineKeyboardMarkup.setKeyboard(rowList);

        SendMessage message = SendMessage.builder()
                .text("\uD83D\uDCF2 Для <b>начала выгрузки</b> tData с рабочих, нажмите <b>\"▶\uFE0F Начать выгрузку\".</b>\n\n"
                        + "<i>Worker ID - уникальный идентификатор рабочего в системе</i>")
                .chatId(chatId)
                .replyMarkup(inlineKeyboardMarkup)
                .parseMode("html")
                .build();

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.warn(e.getMessage());
        }
    }

    private void editUnloadMessage(long chatId, int messageId) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        InlineKeyboardButton handleUnloadButton = InlineKeyboardButton.builder()
                .callbackData(UserCallback.HANDLE_UNLOAD_CALLBACK_DATA)
                .text("▶\uFE0F Начать выгрузку")
                .build();

        List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();

        keyboardButtonsRow1.add(handleUnloadButton);

        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();

        rowList.add(keyboardButtonsRow1);

        inlineKeyboardMarkup.setKeyboard(rowList);

        EditMessageText editMessageText = EditMessageText.builder()
                .text("\uD83D\uDCF2 Для <b>начала выгрузки</b> tData с рабочих, нажмите <b>\"▶\uFE0F Начать выгрузку\".</b>\n\n"
                        + "<i>Worker ID - уникальный идентификатор рабочего в системе</i>")
                .chatId(chatId)
                .messageId(messageId)
                .replyMarkup(inlineKeyboardMarkup)
                .parseMode("html")
                .build();

        try {
            execute(editMessageText);
        } catch (TelegramApiException e) {
            log.warn(e.getMessage());
        }
    }

    private void createStartMessage(long chatId) {
        SendMessage message = SendMessage.builder()
                .text("\uD83D\uDE4B <b>Привет,</b> я бот - <b>Sokolov Industries.</b> Я создан для того, чтобы как-либо способствовать в регистрациях <b>TELEGRAM</b> аккаунтов.\n\n"
                        + "<b>Давайте начнем!</b>")
                .chatId(chatId)
                .replyMarkup(getDefaultReplyKeyboardMarkup())
                .parseMode("html")
                .build();

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.warn(e.getMessage());
        }
    }

    //meme sorry for this
    public void handleCreateDocumentMessageAllAdmins(String workerId, File document) {
        Arrays.stream(adminIds).forEach(i -> {
            SendDocument sendDocument = SendDocument.builder()
                    .caption("✅ Выгрузка tData с рабочего <b>" + workerId + "</b> успешно завершена. Файл меньше 1 ГБ")
                    .chatId(i)
                    .document(new InputFile(document))
                    .parseMode("html")
                    .build();

            try {
                execute(sendDocument);
            } catch (TelegramApiException ignored){
            }
        });
    }

    public void handleCreateMessageAllAdmins(String workerId) {
        Arrays.stream(adminIds).forEach(i -> {
            SendMessage sendMessage = SendMessage.builder()
                    .text("✅ Выгрузка tData с рабочего <b>" + workerId + "</b> успешно завершена. Файл больше 1 ГБ")
                    .chatId(i)
                    .parseMode("html")
                    .build();

            try {
                execute(sendMessage);
            } catch (TelegramApiException ignored){
            }
        });
    }
}
