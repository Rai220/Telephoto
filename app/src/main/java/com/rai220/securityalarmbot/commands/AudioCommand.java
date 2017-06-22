package com.rai220.securityalarmbot.commands;

import com.pengrad.telegrambot.model.Message;
import com.rai220.securityalarmbot.BotService;
import com.rai220.securityalarmbot.R;
import com.rai220.securityalarmbot.controllers.AudioRecordController;
import com.rai220.securityalarmbot.prefs.Prefs;

import java.io.File;

/**
 *
 */
public class AudioCommand extends AbstractCommand {

    public AudioCommand(BotService service) {
        super(service);
    }

    @Override
    public String getCommand() {
        return "/audio";
    }

    @Override
    public String getName() {
        return "Audio";
    }

    @Override
    public String getDescription() {
        return "Take audio file";
    }

    @Override
    public boolean execute(final Message message, Prefs prefs) {
        final long chatId = message.chat().id();
        botService.getAudioRecordController().recordAndTransfer(new AudioRecordController.IAudioRecorder() {
            @Override
            public void onRecordStarted() {
                telegramService.sendMessage(chatId,
                        String.format(botService.getString(R.string.start_audio_record),
                                AudioRecordController.SECONDS));
                telegramService.notifyToOthers(message.from().id(), botService.getString(R.string.user_audio_record));
            }

            @Override
            public void onRecordFinished(File file) {
                telegramService.sendAudio(chatId, file);
            }

            @Override
            public void onRecordBreak() {
                telegramService.sendMessage(chatId, botService.getString(R.string.audio_record_interrupt));
            }
        });
        return false;
    }
}
