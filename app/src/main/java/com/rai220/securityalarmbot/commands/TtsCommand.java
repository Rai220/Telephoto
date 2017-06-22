package com.rai220.securityalarmbot.commands;

import android.speech.tts.TextToSpeech;

import com.google.common.base.Strings;
import com.pengrad.telegrambot.model.Message;
import com.rai220.securityalarmbot.BotService;
import com.rai220.securityalarmbot.R;
import com.rai220.securityalarmbot.prefs.Prefs;
import com.rai220.securityalarmbot.utils.KeyboardUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by rai220 on 11/1/16.
 */
public class TtsCommand extends AbstractCommand {

    public TtsCommand(BotService service) {
        super(service);
    }

    @Override
    public String getCommand() {
        return "/tts";
    }

    @Override
    public String getName() {
        return "TTS";
    }

    @Override
    public String getDescription() {
        return "Speak synthesized text";
    }

    @Override
    public boolean execute(Message message, Prefs prefs) {
        final CancelCommand cancelCommand = new CancelCommand();
        final long chatId = message.chat().id();
        //@todo
        ICommand command = null;// CommandType.getCommand(message.text());

        if (command != null && command.getClass().equals(TtsCommand.class)) {
            List<ICommand> keyboard = new ArrayList<>();
            keyboard.add(cancelCommand);
            telegramService.sendMessage(chatId, "Type text to say aloud via TTS:", KeyboardUtils.getKeyboard(keyboard));
            return true;
        } else {
            final String textToSpeech = message.text().trim();
            if (!Strings.isNullOrEmpty(textToSpeech)) {
                if (textToSpeech.equals(cancelCommand.getCommand()) || textToSpeech.equals(cancelCommand.getName())) {
                    telegramService.sendMessage(chatId, botService.getString(R.string.operation_cancel));
                    // TODO: 09.04.2017 mainK
//                    telegramService.sendMessage(chatId, botService.getString(R.string.operation_cancel), mainKeyBoard);
                } else if (botService.ttsInitialized) {
                    botService.tts.speak(textToSpeech, TextToSpeech.QUEUE_ADD, null);
                    telegramService.sendMessage(chatId, "I sad '" + textToSpeech + "' using TTS.");
                    // TODO: 09.04.2017 mainK
//                    telegramService.sendMessage(chatId, "I sad '" + textToSpeech + "' using TTS.", mainKeyBoard);
                    telegramService.notifyToOthers(message.from().id(), "used tts to say: " + textToSpeech);
                } else {
                    telegramService.sendMessage(chatId, "Sorry, but TTS is not initialized on your device!");
                    // TODO: 09.04.2017 mainK
//                    telegramService.sendMessage(chatId, "Sorry, but TTS is not initialized on your device!", mainKeyBoard);
                }
            }
            return false;
        }
    }

    public static final class CancelCommand implements ICommand {
        @Override
        public String getCommand() {
            return "/cancel";
        }

        @Override
        public String getName() {
            return "Cancel";
        }

        @Override
        public String getDescription() {
            return "";
        }

        @Override
        public boolean isEnable() {
            return true;
        }

        @Override
        public boolean isHide() {
            return false;
        }

        @Override
        public boolean execute(Message message, Prefs prefs) {
            return false;
        }

        @Override
        public Collection<ICommand> execute(Message message) {
            return null;
        }

    }
}
